package furgl.infinitory.mixin.inventory;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import furgl.infinitory.config.Config;
import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.impl.inventory.IScreenHandler;
import furgl.infinitory.impl.inventory.SortingType;
import furgl.infinitory.impl.lists.ListeningDefaultedList;
import furgl.infinitory.impl.lists.MainDefaultedList;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements Inventory, IPlayerInventory {

	// Separate main and infinitory
	// PROS: easy to read/write, 
	// CONS: buggy af with server->client sync
	
	// Expanding main
	// PROS: 
	// CONS: harder to read/write
	
	@Unique
	private SortingType sortingType;
	/**Infinitory's extra inventory slots*/
	@Unique
	private ListeningDefaultedList infinitory; 
	@Unique
	private int additionalSlots;
	@Unique
	private boolean needToUpdateInfinitorySize;
	@Unique
	private boolean needToUpdateClient;
	@Unique
	private boolean needToSort;
	/**Combined main + infinitory list for sorting*/
	@Unique
	public ArrayList<ItemStack> mainInfinitory = Lists.newArrayList();

	@Shadow @Final @Mutable
	public DefaultedList<ItemStack> main;
	@Shadow @Final
	public DefaultedList<ItemStack> armor;
	@Shadow @Final
	public DefaultedList<ItemStack> offHand;
	@Shadow @Final @Mutable
	private List<DefaultedList<ItemStack>> combinedInventory;

	@Inject(method = "<init>", at = @At("TAIL"))
	public void constructor(CallbackInfo ci) {
		this.main = MainDefaultedList.ofSize(36, ItemStack.EMPTY, this);
		this.infinitory = ListeningDefaultedList.of(this);
		this.combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand, this.infinitory);
		this.updateInfinitorySize();
	}

	// ========== SORTING ==========

	@Unique
	private void sort() {
		this.sortingType = SortingType.QUANTITY; // TODO remove
		if (!((PlayerInventory)(Object)this).player.world.isClient && this.getSortingType() != SortingType.NONE) {
			// combine all items into one list
			this.mainInfinitory.clear();
			for (List<ItemStack> list : Lists.newArrayList(this.infinitory, this.main.subList(9, this.main.size())))
				for (ItemStack addingStack : list) {
					outer:
						if (!addingStack.isEmpty()) {
							// try to stack with existing items in list
							for (ItemStack stack : this.mainInfinitory)
								if (this.canStackAddMore(stack, addingStack)) {
									int amountToAdd = addingStack.getCount();
									if (amountToAdd > this.getMaxCountPerStack() - stack.getCount()) 
										amountToAdd = this.getMaxCountPerStack() - stack.getCount();
									if (amountToAdd > 0) {
										System.out.println("combining "+amountToAdd+"x "+addingStack.getName().getString()); // TODO remove
										stack.increment(amountToAdd);
										addingStack.decrement(amountToAdd);
										// addingStack empty - we can skip to the next item
										if (addingStack.isEmpty())
											break outer;
									}
								}
							// didn't find anything to stack with, add to list (don't worry about max size - should be handled already)
							System.out.println("adding "+addingStack.getCount()+"x "+addingStack.getName().getString()); // TODO remove
							this.mainInfinitory.add(addingStack.copy());
						}
				}
			// sort
			this.getSortingType().sort(this.mainInfinitory, true);
			// copy from sorted list to main and infinitory
			for (int i=9; i<this.main.size(); ++i)
				this.main.set(i, ItemStack.EMPTY);
			this.infinitory.clear();
			for (int i=0; i<this.mainInfinitory.size(); ++i) 
				if ((i+9)<this.main.size())
					this.main.set(i+9, this.mainInfinitory.get(i));
				else
					this.infinitory.add(this.mainInfinitory.get(i));
			// update to client
			this.needToUpdateClient = true;
		}
	}

	@Unique
	@Override
	public SortingType getSortingType() {
		if (this.sortingType == null)
			this.sortingType = SortingType.NONE;
		return this.sortingType;
	}

	@Unique
	@Override
	public void needToSort() {
		this.needToSort = true;
	}

	// ========== EXPANDING INVENTORY ==========

	/**Update main and infinitory full/empty status*/
	@Unique
	public void updateFullEmptyStatus() {
		for (ListeningDefaultedList list : new ListeningDefaultedList[] {(ListeningDefaultedList) this.main, this.infinitory}) {
			list.isEmpty = true;
			list.isFull = true;
			for (int i=list instanceof MainDefaultedList ? 9 : 0; i<list.size(); ++i) {
				ItemStack stack = list.get(i);
				if (stack.isEmpty())
					list.isFull = false;
				else 
					list.isEmpty = false;
			}
			//System.out.println(list.getClass()+", isEmpty: "+list.isEmpty+", isFull: "+list.isFull+", "+list); // TODO remove
			this.updateInfinitorySize();
		}
			
	}

	/**Recalculate additional slots based on main and infinitory sizes / fullness*/
	@Unique
	public void updateInfinitorySize() { 
		// main not full and infinitory empty = no additional slots
		if (!((MainDefaultedList)this.main).isFull && this.infinitory.isEmpty)
			this.additionalSlots = 0;
		// main full and infinitory empty = 9 additional slots
		else if (((MainDefaultedList)this.main).isFull && this.infinitory.isEmpty) 
			this.additionalSlots = 9;
		// infinitory is not empty = index of last item rounded up to 9 additional slots
		else {
			// get index of last item
			int lastItem = 0;
			boolean fullBeforeLastItem = ((MainDefaultedList)this.main).isFull;
			for (int index=this.infinitory.size()-1; index >= 0; --index) {
				boolean empty = this.infinitory.get(index).isEmpty();
				if (!empty && lastItem == 0) 
					lastItem = index;
				else if (empty && lastItem > 0) {
					fullBeforeLastItem = false;
					break;
				}
			}
			//System.out.println("lastItem: "+lastItem+", fullBeforeLastItem: "+fullBeforeLastItem+", size: "+this.infinitory.size()); // TODO remove
			// index of last item rounded up to multiple of 9 additional slots
			this.additionalSlots = lastItem + (9 - lastItem % 9);
			// if full or last item is size-10 and full before that (meaning full except for empty row at the end) add extra row
			if (this.infinitory.isFull || (fullBeforeLastItem && lastItem == this.infinitory.size()-10)) {
				this.additionalSlots += 9;
				this.infinitory.isFull = false;
			}
		}
		// bound between 0 to config max
		this.additionalSlots = MathHelper.clamp(additionalSlots, 0, Config.maxExtraSlots);
		// must be multiple of 9
		this.additionalSlots = this.additionalSlots - this.additionalSlots % 9;
		//System.out.println("additional slots = "+this.additionalSlots+", main isEmpty: "+((MainDefaultedList)this.main).isEmpty+" isFull: "+((MainDefaultedList)this.main).isFull+", infinitory isEmpty: "+this.infinitory.isEmpty+" isFull: "+this.infinitory.isFull); // TODO remove
		//System.out.println(this.infinitory); // TODO remove
	}

	@Inject(method = "updateItems", at = @At("TAIL"))
	public void updateItems(CallbackInfo ci) {
		// update infinitory size
		if (this.needToUpdateInfinitorySize) {
			this.updateFullEmptyStatus();
			this.needToUpdateInfinitorySize = false;
		}
		// update client
		if (this.needToUpdateClient && ((PlayerInventory)(Object)this).player.age > 10) {
			((PlayerInventory)(Object)this).player.playerScreenHandler.updateToClient();
			this.needToUpdateClient = false;
		}
		// sort
		if (this.needToSort) {
			this.sort();
			this.needToSort = false;
		}
		
		// TODO remove (client has infinitory twice?!?!?)
		if (((PlayerInventory)(Object)this).player.age % 100 == 0) {
			ScreenHandler handler = ((PlayerInventory)(Object)this).player.currentScreenHandler;
			DefaultedList<Slot> slots = handler.slots;
			String str = this.size()+"";
			for (int i=0; i<this.size(); ++i)
				str += "[i:"+i+",index:"+slots.get(i).getIndex()+",id:"+slots.get(i).id+",stack:"+slots.get(i).getStack()+"],";
			System.out.println("(Current) "+handler.getClass().getSimpleName()+": ");
			System.out.println(" - slots: "+str);
			System.out.println(" - mainSlots: "+((IScreenHandler)handler).getMainSlots().size()+((IScreenHandler)handler).getMainSlots());
			
			handler = ((PlayerInventory)(Object)this).player.playerScreenHandler;
			slots = handler.slots;
			str = this.size()+"";
			for (int i=0; i<this.size(); ++i)
				str += "[i:"+i+",index:"+slots.get(i).getIndex()+",id:"+slots.get(i).id+",stack:"+slots.get(i).getStack()+"],";
			System.out.println("(PlayerScreenHandler) "+handler.getClass().getSimpleName()+": ");
			System.out.println(" - slots: "+str);
			System.out.println(" - mainSlots: "+((IScreenHandler)handler).getMainSlots().size()+((IScreenHandler)handler).getMainSlots());
		}
	}

	@Unique
	@Override
	public void needToUpdateInfinitorySize() {
		this.needToUpdateInfinitorySize = true;
	}

	@Unique
	@Override
	public int getAdditionalSlots() {
		return this.additionalSlots;
	}

	@Override
	public int getMaxCountPerStack() {
		return Config.maxStackSize;
	}

	@Override
	public DefaultedList<ItemStack> getInfinitory() {
		return this.infinitory;
	}

	// ========== DROPS ON DEATH ==========

	/**Drop different items depending on config option*/
	@Inject(method = "dropAll", at = @At("HEAD"), cancellable = true)
	public void dropAll(CallbackInfo ci) {
		// up to stack of everything
		if (Config.dropsOnDeath == 1) {
			for (List<ItemStack> list : this.combinedInventory) 
				for (ItemStack stack : list) 
					if (!stack.isEmpty()) 
						((PlayerInventory)(Object)this).player.dropItem(stack.split(stack.getMaxCount()), true, false);
			ci.cancel();
		}
		// up to stack of hotbar and armor
		else if (Config.dropsOnDeath == 2) {
			List<ItemStack> list = Lists.newArrayList();
			list.addAll(this.offHand);
			list.addAll(this.armor);
			list.addAll(this.main.subList(0, 9));
			for (ItemStack stack : list) 
				if (!stack.isEmpty()) 
					((PlayerInventory)(Object)this).player.dropItem(stack.split(stack.getMaxCount()), true, false);
			ci.cancel();
		}
	}

	// ========== EXPANDING STACK SIZE ==========

	/**Have getOccupiedSlotWithRoomForStack check infinitory if it can't find a slot in main*/
	@Inject(method = "getOccupiedSlotWithRoomForStack", at = @At("RETURN"), cancellable = true)
	public void getOccupiedSlotWithRoomForStack(ItemStack stack, CallbackInfoReturnable<Integer> ci) {
		if (ci.getReturnValue() == -1) 
			for(int i = 0; i < this.infinitory.size(); ++i) 
				if (this.canStackAddMore((ItemStack)this.infinitory.get(i), stack)) 
					ci.setReturnValue(this.main.size() + 5 + i);
	}

	/**Have getEmptySlot check infinitory if it can't find an empty slot in main*/
	@Inject(method = "getEmptySlot", at = @At("RETURN"), cancellable = true)
	public void getEmptySlot(CallbackInfoReturnable<Integer> ci) {
		if (ci.getReturnValue() == -1) 
			for(int i = this.infinitory.size()-1; i >= 0 && i < this.infinitory.size(); --i) 
				if (((ItemStack)this.infinitory.get(i)).isEmpty()) 
					ci.setReturnValue(this.main.size() + 5 + i);
	}

	/**Remove a lot of restrictions on adding more to stack*/
	@Inject(method = "canStackAddMore", at = @At("RETURN"), cancellable = true)
	public void canStackAddMore(ItemStack existingStack, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
		ci.setReturnValue(canStackAddMore(existingStack, stack));
	}

	@Unique
	private boolean canStackAddMore(ItemStack existingStack, ItemStack stack) {
		return !existingStack.isEmpty() && ItemStack.canCombine(existingStack, stack);
	}

	/**Restrict Ctrl+Q outside of inventory to max stack size*/
	@Redirect(method = "dropSelectedItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;removeStack(II)Lnet/minecraft/item/ItemStack;"))
	public ItemStack removeStack(PlayerInventory inventory, int slot, int amount) {
		return inventory.removeStack(slot, Math.min(amount, 64)); // get item max count instead of just 64? kinda complex to get itemstack...
	}

	@Redirect(method = "offer", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"))
	public int getMaxCountOffer(ItemStack stack) {
		return Config.maxStackSize;
	}

	@Redirect(method = "addStack(ILnet/minecraft/item/ItemStack;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"))
	public int getMaxCountAddStack(ItemStack stack) {
		return Config.maxStackSize;
	}

	// ========== INCLUDING INFINITORY ==========

	/**Include infinitory in size*/
	@Inject(method = "size", at = @At("RETURN"), cancellable = true)
	public void size(CallbackInfoReturnable<Integer> ci) {
		ci.setReturnValue(ci.getReturnValue() + this.infinitory.size());
	}

	/**Include infinitory in isEmpty check*/
	@Inject(method = "isEmpty", at = @At("RETURN"), cancellable = true)
	public void isEmpty(CallbackInfoReturnable<Boolean> ci) {
		ci.setReturnValue(ci.getReturnValue() && this.infinitory.isEmpty());
	}

	/**Include infinitory in indexOf*/
	@Inject(method = "indexOf", at = @At("RETURN"), cancellable = true)
	public void indexOf(ItemStack stack, CallbackInfoReturnable<Integer> ci) {
		if (ci.getReturnValue() == -1) {
			for(int i = 0; i < this.infinitory.size(); ++i) {
				ItemStack itemStack = (ItemStack)this.infinitory.get(i);
				if (!((ItemStack)this.infinitory.get(i)).isEmpty() && ItemStack.canCombine(stack, (ItemStack)this.infinitory.get(i)) && !((ItemStack)this.infinitory.get(i)).isDamaged() && !itemStack.hasEnchantments() && !itemStack.hasCustomName()) 
					ci.setReturnValue(i);
			}
		}
	}

	// ========== NBT ==========

	@Inject(method = "readNbt", at = @At("RETURN"))
	public void readNbt(NbtList nbtList, CallbackInfo ci) {
		this.infinitory.clear();

		for(int i = 0; i < nbtList.size(); ++i) {
			NbtCompound nbtCompound = nbtList.getCompound(i);
			if (nbtCompound.contains("InfinitorySlot")) {
				int slot = nbtCompound.getInt("InfinitorySlot");
				ItemStack itemStack = ItemStack.fromNbt(nbtCompound);
				while (slot > this.infinitory.size()-1 && this.infinitory.size() < Config.maxExtraSlots) 
					this.infinitory.add(ItemStack.EMPTY);
				if (!itemStack.isEmpty() && slot >= 0 && slot < this.infinitory.size()) 
					this.infinitory.set(slot, itemStack);
			}
		}

		// for some reason this desyncs from client with expandable inventory, so we need to update client
		this.needToUpdateClient = true;
	}

	@Inject(method = "writeNbt", at = @At("RETURN"))
	public void writeNbt(NbtList nbtList, CallbackInfoReturnable<NbtCompound> ci) {
		for(int i = 0; i < this.infinitory.size(); ++i) {
			if (!((ItemStack)this.infinitory.get(i)).isEmpty()) {
				NbtCompound nbt = new NbtCompound();
				nbt.putByte("Slot", (byte) 250); // put in a slot value that won't be read by vanilla
				nbt.putInt("InfinitorySlot", i);
				((ItemStack)this.infinitory.get(i)).writeNbt(nbt);
				nbtList.add(nbt);
			}
		}
	}

}
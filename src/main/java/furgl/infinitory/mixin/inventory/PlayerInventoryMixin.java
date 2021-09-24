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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import furgl.infinitory.config.Config;
import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.impl.inventory.IScreenHandler;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import furgl.infinitory.impl.inventory.InfinitorySlot.SlotType;
import furgl.infinitory.impl.inventory.SortingType;
import furgl.infinitory.impl.lists.MainDefaultedList;
import furgl.infinitory.impl.lists.SlotDefaultedList;
import furgl.infinitory.impl.network.PacketManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements Inventory, IPlayerInventory {

	/**FIXME
	 *  Currently: updating size / sorting / everything on client and server
	 *  Problems:
	 *   - armor/offhand go blank for a second when picking up item in inventory
	 *   - client spazzes out when spam clicking sometimes, mixes up inventory, and desyncs from server
	 * */

	@Unique
	private SortingType sortingType;
	@Unique
	private boolean sortAscending;
	@Unique
	private int additionalSlots;
	/**Difference in additional slots in the last tick*/
	@Unique
	private int differenceInAdditionalSlots;
	@Unique
	private boolean needToUpdateInfinitorySize;
	@Unique
	private boolean needToUpdateClient;
	@Unique
	private boolean needToSort;

	@Shadow @Final
	public PlayerEntity player;
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
		this.combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand);
		this.updateInfinitorySize();
	}

	// ========== SORTING ==========

	@Unique
	private void sort() { // TODO sort on server and send additional size via packet?
		if (!this.player.world.isClient && this.getSortingType() != SortingType.NONE) {
			System.out.println("sorting"); // TODO remove
			//System.out.println("before sorting: "+this.main); // TODO remove
			// combine all items into separate list (only inventory - not hotbar!)
			ArrayList<ItemStack> list = Lists.newArrayList();
			for (int i=9; i<this.main.size(); ++i) {
				ItemStack addingStack = this.main.get(i).copy();
				outer:
					if (!addingStack.isEmpty()) {
						// try to stack with existing items in list
						for (ItemStack stack : list) {
							if (this.canStackAddMore(stack, addingStack)) {
								int amountToAdd = addingStack.getCount();
								if (amountToAdd > this.getMaxCountPerStack() - stack.getCount()) 
									amountToAdd = this.getMaxCountPerStack() - stack.getCount();
								if (amountToAdd > 0) {
									//System.out.println("combining "+amountToAdd+"x "+addingStack.getName().getString()); // TODO remove
									stack.increment(amountToAdd);
									addingStack.decrement(amountToAdd);
									// addingStack empty - we can skip to the next item
									if (addingStack.isEmpty())
										break outer;
								}
							}
						}
						// didn't find anything to stack with, add to list (don't worry about max size - should be handled already)
						//System.out.println("adding "+addingStack.getCount()+"x "+addingStack.getName().getString()); // TODO remove
						list.add(addingStack);
					}
			}
			// sort
			this.getSortingType().sort(list, sortAscending);
			// update main with list
			for (int i=9; i<this.main.size(); ++i) {
				ItemStack stack = ItemStack.EMPTY;
				if (i-9 < list.size())
					stack = list.get(i-9);
				((MainDefaultedList)this.main).delegate.set(i, stack.copy());
			}
			//System.out.println("after sorting: "+this.main); // TODO remove
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
	public void setSortingType(SortingType type) {
		if (type != null && type != this.sortingType) {
			this.sortingType = type;
			this.syncInfinitoryValues();
		}
	}

	@Unique
	@Override
	public boolean getSortingAscending() {
		return this.sortAscending;
	}

	@Unique
	@Override
	public void setSortAscending(boolean sortAscending) {
		if (sortAscending != this.sortAscending) {
			this.sortAscending = sortAscending;
			this.syncInfinitoryValues();
		}
	}

	@Unique
	@Override
	public void needToSort() {
		this.needToSort = true;
		this.needToUpdateInfinitorySize();
	}

	/**Sync additional slots, sorting type, and sorting ascending server -> client
	 * and TODO save values for player*/
	@Unique
	@Override
	public void syncInfinitoryValues() {
		System.out.println("sync values: additionalSlots: "+this.additionalSlots+", sortingType: "+this.sortingType+", sortAscending: "+this.sortAscending); // TODO remove
		// send packet server -> client
		if (!this.player.world.isClient && this.player instanceof ServerPlayerEntity) {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeInt(this.additionalSlots);
			buf.writeInt(this.getSortingType().ordinal());
			buf.writeBoolean(this.sortAscending);
			ServerPlayNetworking.send((ServerPlayerEntity) this.player, PacketManager.UPDATE_INFINITORY_PACKET_ID, buf);
		}
	}

	// ========== EXPANDING INVENTORY ==========

	/**Recalculate additional slots based on main and infinitory sizes / fullness*/
	@Unique
	@Override
	public void updateInfinitorySize() { 
		if (!this.player.world.isClient) {
			//System.out.println("main: "+this.main.size()+this.main); // TODO remove
			// get indexes of first and last items
			boolean isFull = true;
			boolean isFullBeforeLastItem = true;
			boolean lastRowEmpty = true;
			int lastItem = -1;
			for (int i = this.main.size()-1; i>=9; --i) { // last 5 of main are armor and offhand
				boolean empty = this.main.get(i).isEmpty();
				if (!empty && lastItem == -1) 
					lastItem = i;
				if (empty) {
					if (lastItem != -1)
						isFullBeforeLastItem = false;
					isFull = false;
				}
				if (i >= this.main.size()-9 && !empty) 
					lastRowEmpty = false;
				//System.out.println("checking: i: "+i+", empty: "+empty+", stack: "+this.main.get(i)); // TODO remove
			}
			// index of last item rounded up to multiple of 9 additional slots
			this.setAdditionalSlots(lastItem - 35 + (isFull || (isFullBeforeLastItem && lastRowEmpty && (lastItem+1) % 9 == 0) ? 9 : 0));

			System.out.println("additional slots = "+this.additionalSlots+", lastItem: "+lastItem+", isFull: "+isFull+", isFullBeforeLastItem: "+isFullBeforeLastItem+", lastRowEmpty: "+lastRowEmpty); // TODO remove
		}
	}

	@Unique
	private void updateExtraSlots() {
		((IScreenHandler)this.player.playerScreenHandler).updateExtraSlots();
		if (this.player.currentScreenHandler != null)
			((IScreenHandler)this.player.currentScreenHandler).updateExtraSlots();
	}

	@Override
	public void setAdditionalSlots(int additionalSlots) {
		// bound between 0 to config max
		additionalSlots = MathHelper.clamp(additionalSlots, 0, Config.maxExtraSlots);
		// must be multiple of 9
		if (additionalSlots % 9 != 0)
			additionalSlots = additionalSlots + (9 - additionalSlots % 9);

		// update main size
		while (main.size() < 36 + additionalSlots)
			main.add(ItemStack.EMPTY);
		while (main.size() > 36 + additionalSlots)
			main.remove(main.size() - 1);

		if (this.additionalSlots != additionalSlots) {
			this.differenceInAdditionalSlots = this.additionalSlots - additionalSlots;
			this.additionalSlots = additionalSlots;
			this.syncInfinitoryValues();
			// update extra slots if needed
			this.updateExtraSlots();
		}
	}

	@Inject(method = "updateItems", at = @At("TAIL"))
	public void updateItems(CallbackInfo ci) {
		// sort
		if (this.needToSort) {
			this.sort();
			this.needToSort = false;
		}
		// update infinitory size
		if (this.needToUpdateInfinitorySize) {
			this.updateInfinitorySize();
			this.needToUpdateInfinitorySize = false;
		}
		else // update extra slots if needed (called by updateInfinitorySize as well)
			this.updateExtraSlots();
		// update client
		if (this.needToUpdateClient) {
			this.player.playerScreenHandler.updateToClient();
			if (this.player.currentScreenHandler != null) 
				(this.player.currentScreenHandler).updateToClient();
			this.needToUpdateClient = false;
		}
		// reset difference in additional slots
		this.differenceInAdditionalSlots = 0;
		//this.debugPrint(); // TODO remove
	}

	@Unique // TODO remove
	public void debugPrint() {
		if (this.player.age % 50 == 0) {
			ScreenHandler handler = this.player.currentScreenHandler;
			List<Slot> slots = handler.slots;
			String str = slots.size()+"";
			for (int i=0; i<slots.size(); ++i)
				System.out.println("(i:"+i+"):"+(slots.get(i) instanceof InfinitorySlot ? slots.get(i) : "["+slots.get(i)+",id:"+slots.get(i).id+",index:"+slots.get(i).getIndex()+",stack:"+slots.get(i).getStack()+",x:"+slots.get(i).x+",y:"+slots.get(i).y+"] "+slots.get(i).inventory)+",");
			System.out.println("(Current) "+handler.getClass().getSimpleName()+": ");
			System.out.println(" - slots: "+str);
			System.out.println(" - infinitorySlots: "+((IScreenHandler)handler).getInfinitorySlots().size()+((IScreenHandler)handler).getInfinitorySlots());

			handler = MinecraftClient.getInstance().currentScreen instanceof HandledScreen ? ((HandledScreen)MinecraftClient.getInstance().currentScreen).getScreenHandler() : null;
			if (handler != null) {
				slots = ((SlotDefaultedList)handler.slots).delegate;
				str = slots.size()+"";
				for (int i=0; i<slots.size(); ++i)
					System.out.println("(i:"+i+"):"+(slots.get(i) instanceof InfinitorySlot ? slots.get(i) : "["+slots.get(i)+",id:"+slots.get(i).id+",index:"+slots.get(i).getIndex()+",stack:"+slots.get(i).getStack()+",x:"+slots.get(i).x+",y:"+slots.get(i).y+"] "+slots.get(i).inventory)+",");
				System.out.println("(Client) "+handler.getClass().getSimpleName()+": ");
				System.out.println(" - slots: "+str);
				System.out.println(" - infinitorySlots: "+((IScreenHandler)handler).getInfinitorySlots().size()+((IScreenHandler)handler).getInfinitorySlots());
			}

			handler = this.player.playerScreenHandler;
			slots = handler.slots;
			str = slots.size()+"";
			for (int i=0; i<slots.size(); ++i)
				System.out.println("(i:"+i+"):"+(slots.get(i) instanceof InfinitorySlot ? slots.get(i) : "["+slots.get(i)+",id:"+slots.get(i).id+",index:"+slots.get(i).getIndex()+",stack:"+slots.get(i).getStack()+",x:"+slots.get(i).x+",y:"+slots.get(i).y+"] "+slots.get(i).inventory)+",");
			System.out.println("(PlayerScreenHandler) "+handler.getClass().getSimpleName()+": ");
			System.out.println(" - slots: "+str);
			System.out.println(" - infinitorySlots: "+((IScreenHandler)handler).getInfinitorySlots().size()+((IScreenHandler)handler).getInfinitorySlots());

			System.out.println("main: "+this.main.size()+this.main);
		}
	}

	@Unique
	@Override
	public void needToUpdateInfinitorySize() {
		this.needToUpdateInfinitorySize = true;
	}

	@Unique
	@Override
	public void needToUpdateClient() {
		//if (!this.player.world.isClient)
		this.needToUpdateClient = true;
	}

	@Unique
	@Override
	public int getAdditionalSlots() {
		return this.additionalSlots;
	}

	@Unique
	@Override
	public int getDifferenceInAdditionalSlots() {
		return this.differenceInAdditionalSlots;
	}

	@Override
	public int getMaxCountPerStack() {
		return Config.maxStackSize;
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
						this.player.dropItem(stack.split(stack.getMaxCount()), true, false);
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
					this.player.dropItem(stack.split(stack.getMaxCount()), true, false);
			ci.cancel();
		}
	}

	// ========== EXPANDING STACK SIZE ==========

	/**Remove a lot of restrictions on adding more to stack*/
	@Inject(method = "canStackAddMore", at = @At("RETURN"), cancellable = true)
	public void canStackAddMore(ItemStack existingStack, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
		ci.setReturnValue(canStackAddMore(existingStack, stack));
	}

	@Unique
	private boolean canStackAddMore(ItemStack existingStack, ItemStack stack) {
		if (!existingStack.isEmpty() && ItemStack.canCombine(existingStack, stack)) {
			// if not stackable, return if this slot allows stacking non-stackables
			if (!stack.isStackable()) {
				for (int i=0; i<this.size(); ++i)
					if (this.getStack(i) == existingStack)  
						return SlotType.getType(i, this.getAdditionalSlots()).stackNonStackables;
			}
			return true;
		}
		return false;
	}

	/**Restrict Ctrl+Q outside of inventory to max stack size*/
	@Redirect(method = "dropSelectedItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;removeStack(II)Lnet/minecraft/item/ItemStack;"))
	public ItemStack removeStack(PlayerInventory inventory, int slot, int amount) {
		return inventory.removeStack(slot, Math.min(amount, 64)); // get item max count instead of just 64? kinda complex to get itemstack...
	}

	@Redirect(method = "offer", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"))
	public int offerGetMaxCount(ItemStack stack) {
		return Config.maxStackSize;
	}

	@Redirect(method = "addStack(ILnet/minecraft/item/ItemStack;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"))
	public int addStackGetMaxCount(ItemStack stack) {
		return Config.maxStackSize;
	}

	@Inject(method = "addStack(ILnet/minecraft/item/ItemStack;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;increment(I)V"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
	public void addStackPickingUpNonStackables(int slot, ItemStack stack, CallbackInfoReturnable<Integer> ci, Item item, int i, ItemStack itemStack, int j) {
		if (!stack.isStackable() && !SlotType.getType(slot, this.getAdditionalSlots()).stackNonStackables &&
				(i+j) > stack.getMaxCount() - itemStack.getCount()) {
			i += j; // undo
			j = stack.getMaxCount() - itemStack.getCount(); // set new stack size
			i -= j; // redo
		}
		itemStack.increment(j);
		itemStack.setCooldown(5);
		ci.setReturnValue(i);
	}

	@Inject(method = "removeStack(II)Lnet/minecraft/item/ItemStack;", at = @At(value = "RETURN"))
	public void removeStackUpdateSize(int slot, int amount, CallbackInfoReturnable<ItemStack> ci) {
		// need to update size manually after this is called bc this calls ItemStack#splitStack which won't trigger an update with ListeningDefaultedList
		this.updateInfinitorySize();
		this.updateExtraSlots();
	}

	// ========== NBT ==========

	@Inject(method = "readNbt", at = @At("RETURN"))
	public void readNbt(NbtList nbtList, CallbackInfo ci) {
		for(int i = 0; i < nbtList.size(); ++i) {
			NbtCompound nbtCompound = nbtList.getCompound(i);
			if (nbtCompound.contains("InfinitorySlot")) {
				int slot = nbtCompound.getInt("InfinitorySlot");
				ItemStack itemStack = ItemStack.fromNbt(nbtCompound);
				if (!itemStack.isEmpty()) {
					// increase main size if necessary
					while (slot > this.main.size()-1 && this.main.size() < Config.maxExtraSlots) 
						this.main.add(ItemStack.EMPTY);
					if (slot >= 0 && slot < this.main.size()) 
						this.main.set(slot, itemStack);
				}
			}
		}
		this.needToUpdateInfinitorySize();
		this.needToUpdateClient();
	}

	private DefaultedList<ItemStack> tempMain;

	@Inject(method = "writeNbt", at = @At("HEAD"))
	public void writeNbt1(NbtList nbtList, CallbackInfoReturnable<NbtCompound> ci) {
		// replace main with a copy that has normal size so extra slots aren't saved here
		this.tempMain = main;
		if (this.main.size() > 36)
			this.main = ((MainDefaultedList)main).subList(0, 36);
	}

	@Inject(method = "writeNbt", at = @At("RETURN"))
	public void writeNbt2(NbtList nbtList, CallbackInfoReturnable<NbtCompound> ci) {
		// change main back to normal
		this.main = this.tempMain;
		// write extra slots
		for(int i = 36; i < this.main.size(); ++i) {
			if (!((ItemStack)this.main.get(i)).isEmpty()) {
				NbtCompound nbt = new NbtCompound();
				nbt.putByte("Slot", (byte) 250); // put in a slot value that won't be read by vanilla
				nbt.putInt("InfinitorySlot", i);
				((ItemStack)this.main.get(i)).writeNbt(nbt);
				nbtList.add(nbt);
			}
		}
	}

}
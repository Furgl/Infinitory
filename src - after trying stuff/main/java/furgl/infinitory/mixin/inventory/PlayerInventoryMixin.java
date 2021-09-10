package furgl.infinitory.mixin.inventory;

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

import furgl.infinitory.config.Config;
import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.impl.inventory.ISlot;
import furgl.infinitory.impl.misc.MainDefaultedList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements Inventory, IPlayerInventory {

	/**Infinitory's extra inventory slots are tacked onto main*/
	@Shadow @Final @Mutable
	public DefaultedList<ItemStack> main;
	@Shadow @Final @Mutable
	public DefaultedList<ItemStack> armor;
	@Shadow @Final @Mutable
	public DefaultedList<ItemStack> offHand;
	@Shadow @Final @Mutable
	private List<DefaultedList<ItemStack>> combinedInventory;
	
	@Unique
	private PlayerEntity player;

	/**Change main to be MainDefaultedList that is not fixed size*/
	@Inject(method = "<init>", at = @At("TAIL"))
	public void constructor(PlayerEntity player, CallbackInfo ci) {
		this.player = player;
		this.main = MainDefaultedList.ofSize(36, ItemStack.EMPTY); 
		this.combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand);
	}

	@Unique
	@Override
	public int getAdditionalSlots() {
		int additionalSlots = 9;
		return MathHelper.clamp(additionalSlots - additionalSlots % 9, 0, Config.maxExtraSlots);
	}

	/**Keep main size updated with additional slots*/
	@Inject(method = "updateItems", at = @At("RETURN"))
	public void updateItems(CallbackInfo ci) {
		int difference = PlayerInventory.MAIN_SIZE+this.getAdditionalSlots() - this.main.size();
		if (difference != 0) {
			// increase size of main
			for (int i=0; i<difference; ++i)
				this.main.add(ItemStack.EMPTY);
			// update indexes of slots pointing to indexes > 36
			if (this.player.playerScreenHandler != null) {
				for (int i=36; i<this.player.playerScreenHandler.slots.size(); ++i) {
					Slot slot = this.player.playerScreenHandler.getSlot(i);
					//slot.id += difference;
					//if (slot instanceof ISlot)
					//	((ISlot)slot).setIndex(slot.getIndex()+difference);
				}
			}
		}

		// TODO remove
		//\System.out.println(this.combinedInventory);
	}

	@Override
	public int getMaxCountPerStack() {
		return Config.maxStackSize;
	}

	/**Remove a lot of restrictions on adding more to stack*/
	@Inject(method = "canStackAddMore", at = @At("RETURN"), cancellable = true)
	public void canStackAddMore(ItemStack existingStack, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
		ci.setReturnValue(!existingStack.isEmpty() && ItemStack.canCombine(existingStack, stack));
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

	@Inject(method = "readNbt", at = @At("RETURN"))
	public void readNbt(NbtList nbtList, CallbackInfo ci) {
		// TODO make nbt work for expanded main
		/*for(int i = 0; i < nbtList.size(); ++i) {
			NbtCompound nbtCompound = nbtList.getCompound(i);
			if (nbtCompound.contains("InfinitorySlot")) {
				int slot = nbtCompound.getInt("InfinitorySlot");
				ItemStack itemStack = ItemStack.fromNbt(nbtCompound);
				if (!itemStack.isEmpty() && slot >= 0 && slot < this.infinitory.size()) 
					this.infinitory.set(slot, itemStack);
			}
		}*/
	}

	@Inject(method = "writeNbt", at = @At("RETURN"))
	public void writeNbt(NbtList nbtList, CallbackInfoReturnable<NbtCompound> ci) {
		/*for(int i = 0; i < this.infinitory.size(); ++i) {
			if (!((ItemStack)this.infinitory.get(i)).isEmpty()) {
				NbtCompound nbt = new NbtCompound();
				nbt.putByte("Slot", (byte) 250); // put in a slot value that won't be read by vanilla
				nbt.putInt("InfinitorySlot", i);
				((ItemStack)this.infinitory.get(i)).writeNbt(nbt);
				nbtList.add(nbt);
			}
		}*/
	}

}
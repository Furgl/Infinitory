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

import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.utils.Utils;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements Inventory, IPlayerInventory {

	/**Infinitory's extra inventory slots*/
	@Unique
	private DefaultedList<ItemStack> infinitory;
	
	@Shadow @Final @Mutable
	public DefaultedList<ItemStack> main;
	@Shadow @Final @Mutable
	public DefaultedList<ItemStack> armor;
	@Shadow @Final @Mutable
	public DefaultedList<ItemStack> offHand;
	@Shadow @Final @Mutable
	private List<DefaultedList<ItemStack>> combinedInventory;

	@Inject(method = "<init>", at = @At("TAIL"))
	public void constructor(CallbackInfo ci) {
		this.infinitory = DefaultedList.ofSize(Utils.ADDITIONAL_SLOTS, ItemStack.EMPTY);
		this.combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand, this.infinitory);
	}

	@Override
	public int getMaxCountPerStack() {
		return Integer.MAX_VALUE;
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
		return Integer.MAX_VALUE;
	}

	@Redirect(method = "addStack(ILnet/minecraft/item/ItemStack;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"))
	public int getMaxCountAddStack(ItemStack stack) {
		return Integer.MAX_VALUE;
	}

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

	@Inject(method = "readNbt", at = @At("RETURN"))
	public void readNbt(NbtList nbtList, CallbackInfo ci) {
		this.infinitory.clear();

		for(int i = 0; i < nbtList.size(); ++i) {
			NbtCompound nbtCompound = nbtList.getCompound(i);
			if (nbtCompound.contains("InfinitorySlot")) {
				int slot = nbtCompound.getInt("InfinitorySlot");
				ItemStack itemStack = ItemStack.fromNbt(nbtCompound);
				if (!itemStack.isEmpty() && slot >= 0 && slot < this.infinitory.size()) 
					this.infinitory.set(slot, itemStack);
			}
		}
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
	
	@Override
	public DefaultedList<ItemStack> getInfinitory() {
		return this.infinitory;
	}

}
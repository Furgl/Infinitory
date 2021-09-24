package furgl.infinitory.mixin.inventory;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import furgl.infinitory.config.Config;
import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.utils.Utils;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin extends ScreenHandlerMixin implements ScreenHandlerAccessor {

	@Shadow @Final
	private PlayerEntity owner;

	@Unique
	@Override
	public int getMaxCountInsertItem(ItemStack stack) {
		return Config.maxStackSize;
	}

	@Unique 
	@Override
	public void updateSlotStacksAddExtraSlots(int revision, List<ItemStack> stacks, ItemStack cursorStack, CallbackInfo ci) {
		// add additional slots before reading from server, so everything reads in properly and no server-client desync
		//System.out.println(stacks.size()+stacks.toString()); // TODO remove
		if (stacks.size() > this.slots.size()) {
			System.out.println("updateSlotStacksAddExtraSlots before: stacksSize: "+stacks.size()+", slots size: "+this.slots.size()+", stacks: "+stacks); // TODO remove
			System.out.println("infinitory slots before: "+this.getInfinitorySlots().size()+this.getInfinitorySlots()); // TODO remove
			System.out.println("additional slots before: "+((IPlayerInventory)this.owner.getInventory()).getAdditionalSlots()); // TODO remove
			//((IPlayerInventory)this.owner.getInventory()).setAdditionalSlots(stacks.size()-this.slots.size()+((IPlayerInventory)this.owner.getInventory()).getAdditionalSlots()); // update additional slots and main
			//((IScreenHandler)this).updateExtraSlots(); // add extra slots
			System.out.println("additional slots after: "+((IPlayerInventory)this.owner.getInventory()).getAdditionalSlots()); // TODO remove
			System.out.println("infinitory slots after: "+this.getInfinitorySlots().size()+this.getInfinitorySlots()); // TODO remove
			System.out.println("updateSlotStacksAddExtraSlots after: stacksSize: "+stacks.size()+", slots size: "+this.slots.size()); // TODO remove
		}
	}

	/**Use this method instead of {@link ScreenHandler#transferSlot(PlayerEntity, int)}
	 * Mostly copied from original method, but modified to support more slots
	 */
	@Unique
	@Override
	protected ItemStack transferSlotCustom(PlayerEntity player, int index) {
		ItemStack itemStack = ItemStack.EMPTY;
		Slot slot = (Slot)((PlayerScreenHandler)(Object)this).slots.get(index);
		if (slot != null && slot.hasStack()) {
			ItemStack itemStack2 = slot.getStack();
			itemStack = itemStack2.copy();
			EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(itemStack);
			// craft output -> anywhere
			if (index == 0) {
				if (!this.callInsertItem(itemStack2, 9, 45+Utils.getAdditionalSlots(player), true)) 
					return ItemStack.EMPTY;
				slot.onQuickTransfer(itemStack2, itemStack);
			} 
			// craft input -> anywhere
			else if (index >= 1 && index < 5) { 
				if (!this.callInsertItem(itemStack2, 9, 45+Utils.getAdditionalSlots(player), false)) 
					return ItemStack.EMPTY;
			} 
			// armor -> anywhere
			else if (index >= 5 && index < 9) {
				if (!this.callInsertItem(itemStack2, 9, 45+Utils.getAdditionalSlots(player), false)) 
					return ItemStack.EMPTY;
			} 
			// anywhere -> armor
			else if (equipmentSlot.getType() == EquipmentSlot.Type.ARMOR && !((Slot)((PlayerScreenHandler)(Object)this).slots.get(8 - equipmentSlot.getEntitySlotId())).hasStack()) {
				int i = 8 - equipmentSlot.getEntitySlotId();
				if (!this.callInsertItem(itemStack2, i, i + 1, false)) 
					return ItemStack.EMPTY;
			} 
			// anywhere -> offhand 
			else if (equipmentSlot == EquipmentSlot.OFFHAND && !((Slot)((PlayerScreenHandler)(Object)this).slots.get(45+Utils.getAdditionalSlots(player))).hasStack()) {
				if (!this.callInsertItem(itemStack2, 45+Utils.getAdditionalSlots(player), 46+Utils.getAdditionalSlots(player), false)) 
					return ItemStack.EMPTY;
			} 
			// main inventory -> hotbar
			else if (index >= 9 && (index < 36+Utils.getAdditionalSlots(player) || index > 45+Utils.getAdditionalSlots(player))) {
				if (!this.callInsertItem(itemStack2, 36+Utils.getAdditionalSlots(player), 45+Utils.getAdditionalSlots(player), false)) 
					return ItemStack.EMPTY;
			} 
			// hotbar -> main inventory
			else if (index >= 36+Utils.getAdditionalSlots(player) && index < 45+Utils.getAdditionalSlots(player)) {
				if (!this.callInsertItem(itemStack2, 9, 36+Utils.getAdditionalSlots(player), false)) 
					return ItemStack.EMPTY;
			} 
			// anywhere -> anywhere
			else if (!this.callInsertItem(itemStack2, 9, 45+Utils.getAdditionalSlots(player), false)) 
				return ItemStack.EMPTY;

			if (itemStack2.isEmpty()) 
				slot.setStack(ItemStack.EMPTY);
			else 
				slot.markDirty();

			if (itemStack2.getCount() == itemStack.getCount()) 
				return ItemStack.EMPTY;

			slot.onTakeItem(player, itemStack2);
			if (index == 0) 
				player.dropItem(itemStack2, false);
		}

		return itemStack;
	}

}
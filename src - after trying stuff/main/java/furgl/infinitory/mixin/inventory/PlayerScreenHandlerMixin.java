package furgl.infinitory.mixin.inventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import furgl.infinitory.config.Config;
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

	@Override
	public int getMaxCountInsertItem(ItemStack stack) {
		return Config.maxStackSize;
	}

	/**Use this method instead of {@link ScreenHandler#transferSlot(PlayerEntity, int)}
	 * Mostly copied from original method, but modified to support more slots
	 * Slot indexes:
	 *   0 = crafting output
	 *   1-4 = crafting input
	 *   5-8 = armor
	 *   9-35 = main inventory
	 *   36-44 = hotbar 
	 *   45 = offhand
	 *   
	 * PlayerScreenHandler slots:
	 *   0 = crafting output
	 *   0-3 = crafting input
	 *   0-8 = hotbar
	 *   36-39 = armor
	 *   9-35 = main inventory
	 *   40 = offhand
	 *   >40 = infinitory
	 */
	@Unique
	@Override
	protected ItemStack transferSlot(PlayerEntity player, int index) {
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
			else if (equipmentSlot == EquipmentSlot.OFFHAND && !((Slot)((PlayerScreenHandler)(Object)this).slots.get(45)).hasStack()) {
				if (!this.callInsertItem(itemStack2, 45, 46, false)) 
					return ItemStack.EMPTY;
			} 
			// main inventory -> hotbar
			else if (index >= 9 && index < 36) {
				if (!this.callInsertItem(itemStack2, 36, 45, false)) 
					return ItemStack.EMPTY;
			} 
			// hotbar -> main inventory
			else if (index >= 36 && index < 45) {
				if (!this.callInsertItem(itemStack2, 9, 36, false) || 
						(Utils.getAdditionalSlots(player) > 0 && !this.callInsertItem(itemStack2, 46, 46+Utils.getAdditionalSlots(player), false))) 
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
package furgl.infinitory.mixin;

import org.spongepowered.asm.mixin.Mixin;

import furgl.infinitory.slots.InfinitorySlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;

@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin extends ScreenHandlerMixin {

	@Override
	public int getMaxCountInsertItem(ItemStack stack) {
		return Integer.MAX_VALUE;
	}

	/** Replace all inventory, hotbar, offhand, and crafting input slots with InfinitorySlots */
	@Override
	public Slot changeSlot(Slot slot) {
		if (!(slot instanceof CraftingResultSlot) && (slot.getIndex() < 36 || slot.getIndex() == 40))
			return new InfinitorySlot(slot.inventory, slot.getIndex(), slot.x, slot.y, slot.getBackgroundSprite());
		else 
			return slot;
	}

}
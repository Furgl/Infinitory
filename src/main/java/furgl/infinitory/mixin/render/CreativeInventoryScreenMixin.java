package furgl.infinitory.mixin.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import furgl.infinitory.config.Config;
import furgl.infinitory.impl.inventory.ISlot;
import furgl.infinitory.impl.render.IHandledScreen;
import furgl.infinitory.utils.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
@Mixin(value = CreativeInventoryScreen.class) 
public abstract class CreativeInventoryScreenMixin extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler> {

	public CreativeInventoryScreenMixin(CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
		super(screenHandler, playerInventory, text);
	}

	@ModifyVariable(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At(value = "HEAD"))
	public Slot onMouseClickChangeSlotToCreative(Slot slot) {
		CreativeSlot newSlot = slot != null && slot.inventory instanceof PlayerInventory ? new CreativeSlot(slot, slot.getIndex(), slot.x, slot.y) : null;
		if (newSlot != null) {
			newSlot.id = slot.id;
			((ISlot)newSlot).setIndex(slot.getIndex());
			return newSlot;
		}
		return slot;
	}

	@Inject(method = "setSelectedTab", at = @At(value = "RETURN"))
	public void setSelectedTabResetScroll(ItemGroup group, CallbackInfo ci) {
		if (this instanceof IHandledScreen)
			((IHandledScreen)this).resetScrollPosition();
	}

	/**Add extra slots to calculations for slot id that use 36 (size of normal inventory)
	 * Without this, changes to hotbar when in non-inventory tabs in Creative mode will update extra slots as well*/
	@ModifyConstant(method = "onMouseClick", constant = @Constant(intValue = 36))
	public int onMouseClickFix(int value) {
		return value + Utils.getAdditionalSlots(MinecraftClient.getInstance().player)+(Config.expandedCrafting ? 5 : 0);
	}

	@Inject(method = "setSelectedTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;<init>(Lnet/minecraft/inventory/Inventory;III)V"))
	public void setSelectedTabFixTrinketsDependency(ItemGroup group, CallbackInfo ci) {
		// copied from setSelectedTab to add the rest of the slots (Trinkets only lets it add 0-45 slots) and account for extra crafting slots
		ScreenHandler screenHandler = this.client.player.playerScreenHandler;
		((CreativeInventoryScreen.CreativeScreenHandler)this.handler).slots.clear();
		int extraCraftingSlots = Config.expandedCrafting ? 5 : 0; // added to account for extra crafting slots
        for(int index = 0; index < screenHandler.slots.size(); ++index) {
           int x = 0;
           int k;
           int j;
           int i;
           int y = 0;
           // armor slots
           if (index >= 5+extraCraftingSlots && index < 9+extraCraftingSlots) {
				k = index-extraCraftingSlots - 5;
				j = k / 2;
				i = k % 2;
				x = 54 + j * 54;
				y = 6 + i * 27;
           } 
           // crafting slots
           else if (index >= 0 && index < 5+extraCraftingSlots) {
              x = -2000;
              y = -2000;
           } 
           // offhand
           else if (index == 45+extraCraftingSlots) {
              x = 35;
              y = 20;
           } 
           // main inventory
           else {
              k = index-extraCraftingSlots - 9;
              j = k % 9;
              i = k / 9;
              x = 9 + j * 18;
              if (index >= 36+extraCraftingSlots) {
                 y = 112;
              } else {
                 y = 54 + i * 18;
              }
           }

           Slot slot = new CreativeInventoryScreen.CreativeSlot((Slot)screenHandler.slots.get(index), index, x, y);
           ((CreativeInventoryScreen.CreativeScreenHandler)this.handler).slots.add(slot);
        }
	}

}
package furgl.infinitory.mixin.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import furgl.infinitory.Infinitory;
import furgl.infinitory.impl.inventory.IPlayerInventory;
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
		return value + Utils.getAdditionalSlots(MinecraftClient.getInstance().player);
	}

	@Inject(method = "setSelectedTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;<init>(Lnet/minecraft/inventory/Inventory;III)V"))
	public void setSelectedTabFixTrinketsDependency(ItemGroup group, CallbackInfo ci) {
		ScreenHandler screenHandler = this.client.player.playerScreenHandler;
		int slotSize = 46 + ((IPlayerInventory)this.client.player.getInventory()).getAdditionalSlots();
		if (Infinitory.trinketsDependency != null && slotSize > 46) {
			// copied from setSelectedTab to add the rest of the slots (Trinkets only lets it add 0-45 slots)
			for(int l = 45; l < slotSize; ++l) {
				int t;
				int v;
				int w;
				int x;
				int aa;
				if (l >= 5 && l < 9) {
					v = l - 5;
					w = v / 2;
					x = v % 2;
					t = 54 + w * 54;
					aa = 6 + x * 27;
				} else if (l >= 0 && l < 5) {
					t = -2000;
					aa = -2000;
				} else if (l == 45) {
					t = 35;
					aa = 20;
				} else {
					v = l - 9;
					w = v % 9;
					x = v / 9;
					t = 9 + w * 18;
					if (l >= 36) {
						aa = 112;
					} else {
						aa = 54 + x * 18;
					}
				}

				Slot slot = new CreativeInventoryScreen.CreativeSlot((Slot)screenHandler.slots.get(l), l, t, aa);
				((CreativeInventoryScreen.CreativeScreenHandler)this.handler).slots.add(slot);
			}
		}
	}

}
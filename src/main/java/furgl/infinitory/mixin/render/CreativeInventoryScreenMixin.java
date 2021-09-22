package furgl.infinitory.mixin.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import furgl.infinitory.impl.render.IHandledScreen;
import furgl.infinitory.utils.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler> {

	public CreativeInventoryScreenMixin(CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
		super(screenHandler, playerInventory, text);
	}

	@Inject(method = "setSelectedTab", at = @At(value = "RETURN"))
	public void setSelectedTab(ItemGroup group, CallbackInfo ci) {
		if (this instanceof IHandledScreen)
			((IHandledScreen)this).resetScrollPosition();
	}
	
	/**Add extra slots to calculations for slot id that use 36 (size of normal inventory)
	 * Without this, changes to hotbar when in non-inventory tabs in Creative mode will update extra slots as well*/
	@ModifyConstant(method = "onMouseClick", constant = @Constant(intValue = 36))
	public int onMouseClick(int value) {
		return value + Utils.getAdditionalSlots(MinecraftClient.getInstance().player);
	}

}
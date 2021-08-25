package furgl.infinitory.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> {

	@Shadow @Final
	protected T handler;

	@Redirect(method = "drawSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"))
	public int getMaxCountDrawSlot(ItemStack stack) {
		if (handler instanceof PlayerScreenHandler || handler instanceof CreativeScreenHandler)
			return Integer.MAX_VALUE;
		else
			return stack.getMaxCount();
	}

	@Redirect(method = "calculateOffset", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"))
	public int getMaxCountCalculateOffset(ItemStack stack) {
		if (handler instanceof PlayerScreenHandler || handler instanceof CreativeScreenHandler)
			return Integer.MAX_VALUE;
		else
			return stack.getMaxCount();
	}

}
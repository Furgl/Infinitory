package furgl.infinitory.mixin.render;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import furgl.infinitory.impl.render.IHandledScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;

@Environment(EnvType.CLIENT)
@Mixin(Mouse.class)
public abstract class MouseMixin {

	@Shadow @Final
	private MinecraftClient client;

	/**Call HandledScreen#onMouseScroll for any HandledScreen for scrollbar in Infinitory 
	 * (overriding HandledScreen#mouseScrolled doesn't work for Screens that override it themselves and don't call super - 
	 * i.e. creative inventory)*/
	@Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseScrolled(DDD)Z"), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci, double d, double e, double f) {
		if (this.client.currentScreen instanceof IHandledScreen)
			((IHandledScreen)this.client.currentScreen).onMouseScroll(e, f, d);
	}

}
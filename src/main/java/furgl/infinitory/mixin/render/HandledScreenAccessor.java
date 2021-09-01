package furgl.infinitory.mixin.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screen.ingame.HandledScreen;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {

	@Accessor
	int getBackgroundWidth();
	@Accessor
	int getBackgroundHeight();

}
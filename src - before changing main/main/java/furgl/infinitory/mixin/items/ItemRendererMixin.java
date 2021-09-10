package furgl.infinitory.mixin.items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import furgl.infinitory.utils.Utils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Matrix4f;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

	@Redirect(method = "renderGuiItemOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;FFIZLnet/minecraft/util/math/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;ZII)I"))
	private int draw(TextRenderer renderer, String text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumers, boolean seeThrough, int backgroundColor, int light) {
		try {
			String newText = text;
			int count = Integer.valueOf(Formatting.strip(text));
			if (count >= 1000000000)
				newText = Formatting.AQUA+Utils.formatDouble(count / 1000000000d, 1)+"B";
			else if (count >= 1000000)
				newText = Formatting.GREEN+Utils.formatDouble(count / 1000000d, 1)+"M";
			else if (count >= 1000)
				newText = Formatting.YELLOW+Utils.formatDouble(count / 1000d, 1)+"K";

			float scale = 0.55f;
			matrix.multiply(Matrix4f.scale(scale, scale, 1));
			return renderer.draw(newText, (x+renderer.getWidth(text))/scale-renderer.getWidth(newText)-3, y/scale+4, color, shadow, matrix, vertexConsumers, seeThrough, backgroundColor, light);
		}
		catch (Exception e) {
			e.printStackTrace();
			return renderer.draw(text, x, y, color, shadow, matrix, vertexConsumers, seeThrough, backgroundColor, light);
		}
	}

}
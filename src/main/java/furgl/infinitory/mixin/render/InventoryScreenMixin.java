package furgl.infinitory.mixin.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.systems.RenderSystem;

import furgl.infinitory.utils.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreenMixin<PlayerScreenHandler> implements RecipeBookProvider {

	protected InventoryScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "isClickOutsideBounds(DDIII)Z", at = @At(value = "RETURN"), cancellable = true)
	protected void isClickOutsideBoundsFix(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> ci) {
		// allow clicking in crafting slot output
		if (ci.getReturnValueZ() && shouldShowExpandedCrafting() &&
				(mouseX-x) < Utils.CRAFTING_SLOTS_OUTPUT.getLeft()+18 && (mouseX-x) >= Utils.CRAFTING_SLOTS_OUTPUT.getLeft()
				&& (mouseY-y) < Utils.CRAFTING_SLOTS_OUTPUT.getRight()+18 && (mouseY-y) >= Utils.CRAFTING_SLOTS_OUTPUT.getRight())
			ci.setReturnValue(false);
	}

	@Inject(method = "drawBackground", at = @At(value = "INVOKE"))
	public void drawBackgroundStuff(MatrixStack matrix, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		// render 3x3 crafting area
		if (shouldShowExpandedCrafting()) {
			// hide title
			this.titleX = -2000;
			matrix.push();
			matrix.translate(x, y, 0.0D);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			// background
			RenderSystem.setShaderTexture(0, Utils.VANILLA_SEARCH_INVENTORY);
			int x = this.getScrollbarX();
			int y = 7;
			this.drawTexture(matrix, x-5, y-7, 174, 0, 21, 10); // top half
			this.drawTexture(matrix, x-5, y+10, 174, 86, 21, 50); // bottom half
			this.drawTexture(matrix, x-5, y-2, 174, 16, 21, 15); // scrollbar top
			this.drawTexture(matrix, x-5, y+59, 192, 3, 2, 1); // fix outline bottom
			this.drawTexture(matrix, x-5, y-1, 188, 17, 4, 54); // cover scrollbar
			this.drawTexture(matrix, x-1, y-1, 188, 17, 4, 54); // cover scrollbar
			this.drawTexture(matrix, x+3, y-1, 188, 17, 4, 54); // cover scrollbar
			this.drawTexture(matrix, x+7, y-1, 188, 17, 4, 54); // cover scrollbar
			RenderSystem.setShaderTexture(0, Utils.VANILLA_INVENTORY);
			// hide old output slot
			this.drawTexture(matrix, 153, 27, 153, 9, 18, 18);
			// crafting input slots
			for (int row=0; row<3; ++row)
				for (int col=0; col<3; ++col)
					this.drawTexture(matrix, Utils.CRAFTING_SLOTS_INPUT.getLeft()+col*18-1, Utils.CRAFTING_SLOTS_INPUT.getRight()+row*18-1, 97, 17, 18, 18);
			// crafting output slot
			this.drawTexture(matrix, Utils.CRAFTING_SLOTS_OUTPUT.getLeft()-1, Utils.CRAFTING_SLOTS_OUTPUT.getRight()-1, 97, 17, 18, 18);
			// arrow
			this.drawTexture(matrix, Utils.CRAFTING_SLOTS_OUTPUT.getLeft()-20, Utils.CRAFTING_SLOTS_OUTPUT.getRight()-1, 134, 27, 18, 18);
			matrix.pop();
		}
		/*// render searchbar
		matrix.push();
		matrix.translate(x, y, 0.0D);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, Utils.VANILLA_BACKGROUND);
		
		matrix.pop();*/
		
		// reset texture
		RenderSystem.setShaderTexture(0, Utils.VANILLA_INVENTORY);
	}

}
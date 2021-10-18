package furgl.infinitory.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**Same as TexturedButtonWidget - just with v public to edit*/
public class InfinitoryTexturedButtonWidget extends TexturedButtonWidget {
	
	public int v;
	private int u;
	private int hoveredVOffset;
	private Identifier texture;
	private int textureWidth;
	private int textureHeight;

	public InfinitoryTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier texture, int textureWidth, int textureHeight, PressAction pressAction, TooltipSupplier tooltipSupplier, Text text) {
		super(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, tooltipSupplier, text);
		this.u = u;
		this.v = v;
		this.hoveredVOffset = hoveredVOffset;
		this.texture = texture;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}
	
	@Override
	public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
	      RenderSystem.setShader(GameRenderer::getPositionTexShader);
	      RenderSystem.setShaderTexture(0, this.texture);
	      int i = this.v;
	      if (this.isHovered()) {
	         i += this.hoveredVOffset;
	      }

	      RenderSystem.enableDepthTest();
	      drawTexture(matrices, this.x, this.y, (float)this.u, (float)i, this.width, this.height, this.textureWidth, this.textureHeight);
	      if (this.isHovered()) {
	         this.renderTooltip(matrices, mouseX, mouseY);
	      }

	   }

}
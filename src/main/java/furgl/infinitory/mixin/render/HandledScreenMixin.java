package furgl.infinitory.mixin.render;

import java.util.HashMap;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;

import furgl.infinitory.Infinitory;
import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.impl.inventory.IScreenHandler;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import furgl.infinitory.impl.render.IHandledScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements IHandledScreen {

	@Shadow
	protected int x;
	@Shadow
	protected int y;
	@Shadow @Final
	protected T handler;
	@Unique
	private static final Identifier VANILLA_SCROLLBAR = new Identifier("textures/gui/container/creative_inventory/tabs.png");
	@Unique
	private static final Identifier SCROLLBAR_BACKGROUND = new Identifier(Infinitory.MODID, "textures/gui/container/inventory/inventory.png");
	@Unique   
	private float scrollPosition;
	@Unique
	private boolean scrolling;
	@Unique
	private PlayerInventory playerInventory;

	protected HandledScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void constructor(T handler, PlayerInventory inventory, Text title, CallbackInfo ci) {
		this.playerInventory = inventory;
	}
	
	@Unique
	@Override
	public void resetScrollPosition() {
		this.scrolling = false;
		this.scrollPosition = 0;
		this.scrollItems(0);
	}

	@Unique
	private HashMap<Integer, InfinitorySlot> getMainSlots() {
		if (this.handler instanceof IScreenHandler)
			return ((IScreenHandler)this.handler).getMainSlots();
		else
			return Maps.newHashMap();
	}

	@Unique
	private int getScrollbarX() {
		if (this.handler instanceof IScreenHandler)
			return ((IScreenHandler)this.handler).getScrollbarX();
		else
			return -999;
	}

	@Unique
	private int getScrollbarMinY() {
		if (this.handler instanceof IScreenHandler)
			return ((IScreenHandler)this.handler).getScrollbarMinY();
		else
			return -999;
	}

	@Unique
	private int getScrollbarMaxY() {
		if (this.handler instanceof IScreenHandler)
			return ((IScreenHandler)this.handler).getScrollbarMaxY();
		else
			return -999;
	}

	/**Don't draw slots that aren't visible*/
	@Inject(method = "drawSlot", at = @At(value = "HEAD"), cancellable = true)
	private void drawSlot(MatrixStack matrices, Slot slot, CallbackInfo ci) {
		if (slot instanceof InfinitorySlot && !(((InfinitorySlot)slot).isVisible()))
			ci.cancel();
	}

	@Unique
	@Override 
	public void onMouseScroll(double mouseX, double mouseY, double amount) {
		int i = (this.getMainSlots().size() + 9 - 1) / 9 - 3;
		this.scrollPosition = (float)((double)this.scrollPosition - amount / (double)i);
		this.scrollPosition = MathHelper.clamp(this.scrollPosition, 0.0F, 1.0F);
		this.scrollItems(this.scrollPosition);
	}

	@Inject(method = "mouseClicked", at = @At(value = "RETURN"), cancellable = true)
	public void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> ci) {
		if (isClickInScrollbar(mouseX, mouseY)) {
			this.scrolling = true;
			ci.setReturnValue(true);
		}
	}

	@Inject(method = "mouseReleased", at = @At(value = "RETURN"))
	public void mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> ci) {
		if (button == 0)
			this.scrolling = false;
	}

	@Unique
	private boolean isClickInScrollbar(double mouseX, double mouseY) {
		int minX = this.x + this.getScrollbarX() - 4;
		int maxX = minX + 13;
		int minY = this.y + this.getScrollbarMinY();
		int maxY = this.y + this.getScrollbarMaxY();
		return mouseX >= minX && mouseY >= minY && mouseX <= maxX && mouseY <= maxY;
	}

	@Inject(method = "mouseDragged", at = @At(value = "RETURN"), cancellable = true)
	public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> ci) {
		if (this.scrolling) {
			int minY = this.y + this.getScrollbarMinY();
			int maxY = this.y + this.getScrollbarMaxY();
			this.scrollPosition = ((float)mouseY - (float)minY - 7.5F) / ((float)(maxY - minY) - 15.0F);
			this.scrollPosition = MathHelper.clamp(this.scrollPosition, 0.0F, 1.0F);
			if (this.handler instanceof IScreenHandler)
				this.scrollItems(this.scrollPosition);
			ci.setReturnValue(true);
		}
	}

	/**Rearrange playerInventory#main to scroll*/
	@Unique
	public void scrollItems(float position) { 
		if (this.playerInventory instanceof IPlayerInventory) {
			int size = this.getMainSlots().size();

			int i = (size + 9 - 1) / 9 - 3;
			int rowsOffset = -MathHelper.clamp((int)((double)(position * (float)i) + 0.5D), 0, size / 9);

			for (InfinitorySlot slot : this.getMainSlots().values()) 
				slot.setRowsOffset(rowsOffset);
		}
	}

	@Unique
	public boolean shouldShowScrollbar() {
		boolean creativeScreen = ((Object)this) instanceof CreativeInventoryScreen;
		boolean creativeInventory = creativeScreen && ((CreativeInventoryScreen)(Object)this).getSelectedTab() == ItemGroup.INVENTORY.getIndex();
		return this.getMainSlots().size() > 27 && (!creativeScreen || creativeInventory);
	}

	@Inject(method = "render", at = @At(value = "TAIL"))
	private void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		// render scrollbar
		if (this.shouldShowScrollbar()) {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			int x = this.x + this.getScrollbarX();
			int minY = this.y + this.getScrollbarMinY();
			int maxY = this.y + this.getScrollbarMaxY();
			boolean creativeScreen = ((Object)this) instanceof CreativeInventoryScreen;
			// background
			RenderSystem.setShaderTexture(0, SCROLLBAR_BACKGROUND);
			if (creativeScreen) // only draw inside for creative screen
				this.drawTexture(matrices, x-7, minY-4, 0, 3, 23, 59);
			else // draw full
				this.drawTexture(matrices, x-7, minY-7, 0, 0, 23, 66);
			// foreground
			RenderSystem.setShaderTexture(0, VANILLA_SCROLLBAR);
			this.drawTexture(matrices, x-4, minY + (int)((float)(maxY - minY - 17) * this.scrollPosition), 232, 0, 12, 15);
		}
	}

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
	
	@Inject(method = "tick", at = @At("HEAD"))
	public void tick(CallbackInfo ci) { // will not work bc this is only called clientside!!!
		//if ((Object) this.handler instanceof IScreenHandler)
			//((IScreenHandler)this.handler).addExtraSlots();
	}

}
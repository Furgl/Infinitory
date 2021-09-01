package furgl.infinitory.impl.inventory;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class InfinitorySlot extends Slot {

	private static final int OFF_SCREEN_X = Integer.MAX_VALUE;
	private static final int OFF_SCREEN_Y = Integer.MAX_VALUE;

	@Nullable
	private Pair<Identifier, Identifier> backgroundSprite;
	private boolean isVisible;
	private int originalX;
	private int originalY;

	public InfinitorySlot(Inventory inventory, int index, int x, int y, Pair<Identifier, Identifier> backgroundSprite) {
		super(inventory, index, x, y);
		this.originalX = x;
		this.originalY = y;
		this.backgroundSprite = backgroundSprite;
		if (index < 41)
			this.isVisible = true;
		else {
			((ISlot)this).setX(InfinitorySlot.OFF_SCREEN_X);
			((ISlot)this).setY(InfinitorySlot.OFF_SCREEN_Y);
		}
	}

	@Override
	public int getMaxItemCount(ItemStack stack) {
		return Integer.MAX_VALUE;
	}

	@Override
	@Nullable
	public Pair<Identifier, Identifier> getBackgroundSprite() {
		return this.backgroundSprite;
	}

	public boolean isVisible() {
		return this.isVisible;
	}

	/**Offset the slot (while scrolling) - updates slot position and visibility*/
	public void setRowsOffset(int rowsOffset) {
		if (this instanceof ISlot) {
			int offsetIndex = this.getIndex() + 9 * rowsOffset;
			if (offsetIndex >= 9 && offsetIndex <= 40) {
				((ISlot) this).setX(this.originalX);
				((ISlot) this).setY(this.originalY + 18 * rowsOffset);
				this.isVisible = true;
			} 
			else {
				((ISlot)this).setX(InfinitorySlot.OFF_SCREEN_X);
				((ISlot)this).setY(InfinitorySlot.OFF_SCREEN_Y);
				this.isVisible = false;
			}
		}
	}

}
package furgl.infinitory.impl.inventory;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import furgl.infinitory.config.Config;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class InfinitorySlot extends Slot {

	public enum SlotType {
		MAIN_NORMAL(true), MAIN_EXTRA(true), ARMOR(false), OFFHAND(false), HOTBAR(false), CRAFTING_INPUT(true), CRAFTING_OUTPUT(true), UNKNOWN(false);

		public boolean stackNonStackables;
		
		private SlotType(boolean stackNonStackables) {
			this.stackNonStackables = stackNonStackables;
		}
		
		public static SlotType getType(int index, int additionalSlots) {
			if (index >= 0 && index <= 8)
				return HOTBAR;
			else if (index >= 9 && index <= 35)
				return MAIN_NORMAL;
			else if (additionalSlots > 0 && index >= 36 && index <= 36+additionalSlots)
				return MAIN_EXTRA;
			else if (index >= 36+additionalSlots && index <= 36+additionalSlots+3)
				return ARMOR;
			else if (index == 36+additionalSlots+3+1)
				return OFFHAND;
			else
				return UNKNOWN;
		} 
	}

	private static final int OFF_SCREEN_X = Integer.MAX_VALUE;
	private static final int OFF_SCREEN_Y = Integer.MAX_VALUE;

	@Nullable
	private Pair<Identifier, Identifier> backgroundSprite;
	private boolean isVisible;
	public int originalX;
	public int originalY;
	public PlayerEntity player;
	public SlotType type;

	public InfinitorySlot(Inventory inventory, PlayerEntity player, int id, int index, int x, int y, Pair<Identifier, Identifier> backgroundSprite, SlotType type) {
		super(inventory, index, x, y);
		this.player = player;
		this.id = id;
		this.originalX = x;
		this.originalY = y;
		this.backgroundSprite = backgroundSprite;
		this.type = type;
		if (this.type != SlotType.MAIN_EXTRA)
			this.isVisible = true;
		else {
			((ISlot)this).setX(InfinitorySlot.OFF_SCREEN_X);
			((ISlot)this).setY(InfinitorySlot.OFF_SCREEN_Y);
		}
	}

	@Override
	public int getMaxItemCount(ItemStack stack) {
		return !this.type.stackNonStackables && !stack.isStackable() ? stack.getMaxCount() : Config.maxStackSize;
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
			if (offsetIndex >= 9 && offsetIndex <= 35) {
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

	@Override
	public String toString() {
		return "[id:"+this.id+",index:"+this.getIndex()+",stack:"+this.getStack()+",type:"+this.type+",x:"+this.x+",y:"+this.y+"]";
	}
	
}
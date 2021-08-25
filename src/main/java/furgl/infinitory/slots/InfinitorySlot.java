package furgl.infinitory.slots;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class InfinitorySlot extends Slot {

	private Pair<Identifier, Identifier> backgroundSprite;

	public InfinitorySlot(Inventory inventory, int index, int x, int y, Pair<Identifier, Identifier> backgroundSprite) {
		super(inventory, index, x, y);
		this.backgroundSprite = backgroundSprite;
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

}
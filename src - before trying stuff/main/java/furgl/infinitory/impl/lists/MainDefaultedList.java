package furgl.infinitory.impl.lists;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;

import furgl.infinitory.impl.inventory.IPlayerInventory;
import net.minecraft.item.ItemStack;

/**Same as DefaultedList, except tries to add to PlayerInventory#infinitory if index is higher than size*/
public class MainDefaultedList extends ListeningDefaultedList {

	public static MainDefaultedList ofSize(int size, ItemStack defaultValue, IPlayerInventory playerInventory) {
		Validate.notNull(defaultValue);
		ItemStack[] objects = new ItemStack[size];
		Arrays.fill(objects, defaultValue);
		return new MainDefaultedList(Arrays.asList(objects), defaultValue, playerInventory);
	}

	protected MainDefaultedList(List<ItemStack> delegate, ItemStack initialSlot, IPlayerInventory playerInventory) {
		super(delegate, initialSlot, playerInventory);
	}

	@Override
	public void add(int index, ItemStack element) {
		if (index >= this.size())
			this.playerInventory.getInfinitory().add(index, element);
		else
			super.add(index, element);
	}

}
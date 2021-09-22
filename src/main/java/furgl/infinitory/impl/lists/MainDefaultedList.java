package furgl.infinitory.impl.lists;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.Lists;

import furgl.infinitory.impl.inventory.IPlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**Same as DefaultedList, except tries to add to PlayerInventory#infinitory if index is higher than size*/
public class MainDefaultedList extends ListeningDefaultedList {

	public static MainDefaultedList ofSize(int size, ItemStack defaultValue, IPlayerInventory playerInventory) {
		Validate.notNull(defaultValue);
		ArrayList<ItemStack> list = Lists.newArrayList();
		for (int i=0; i<size; ++i)
			list.add(defaultValue);
		return new MainDefaultedList(list, defaultValue, playerInventory);
	}

	private ItemStack initialElement;
	private List<ItemStack> delegate;

	protected MainDefaultedList(List<ItemStack> delegate, ItemStack initialElement, IPlayerInventory playerInventory) {
		super(delegate, initialElement, playerInventory);
		this.initialElement = initialElement;
		this.delegate = delegate;
	}

	@Override
	public DefaultedList subList(int fromIndex, int toIndex) {
        return new MainDefaultedList(this.delegate.subList(fromIndex, toIndex), this.initialElement, this.playerInventory);
    }

}
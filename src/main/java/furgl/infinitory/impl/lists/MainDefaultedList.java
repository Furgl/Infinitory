package furgl.infinitory.impl.lists;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.Lists;

import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.impl.inventory.SortingType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**Same as DefaultedList, except it's expandable and sorts on add/set*/
public class MainDefaultedList extends DefaultedList<ItemStack> {

	private final IPlayerInventory playerInventory;
	private final ItemStack initialElement;
	public final List<ItemStack> delegate;

	public static MainDefaultedList of(List<ItemStack> list, ItemStack defaultValue, IPlayerInventory playerInventory) {
		Validate.notNull(defaultValue);
		return new MainDefaultedList(list, defaultValue, playerInventory);
	}

	public static MainDefaultedList ofSize(int size, ItemStack defaultValue, IPlayerInventory playerInventory) {
		Validate.notNull(defaultValue);
		ArrayList<ItemStack> list = Lists.newArrayList();
		for (int i=0; i<size; ++i)
			list.add(defaultValue);
		return new MainDefaultedList(list, defaultValue, playerInventory);
	}


	protected MainDefaultedList(List<ItemStack> delegate, ItemStack initialElement, IPlayerInventory playerInventory) {
		super(delegate, initialElement);
		this.playerInventory = playerInventory;
		this.initialElement = initialElement;
		this.delegate = delegate;
	}

	@Override
	public DefaultedList subList(int fromIndex, int toIndex) {
		return new MainDefaultedList(this.delegate.subList(fromIndex, toIndex), this.initialElement, this.playerInventory);
	}

	// FIXME right-click splitting in creative may dupe
	@Override
	public ItemStack set(int index, ItemStack element) { 
		if (index > 8) {
			if (this.playerInventory.getSortingType() != SortingType.NONE)
				this.playerInventory.needToSort();
			this.playerInventory.needToUpdateInfinitorySize();
		}
		return super.set(index, element);
	}

	@Override
	public void add(int index, ItemStack element) {
		if (index > 8) {
			if (this.playerInventory.getSortingType() != SortingType.NONE) 
				this.playerInventory.needToSort();
			this.playerInventory.needToUpdateInfinitorySize();
		}
		super.add(index, element);
	}

}
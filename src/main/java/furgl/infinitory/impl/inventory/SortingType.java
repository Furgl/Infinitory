package furgl.infinitory.impl.inventory;

import java.util.ArrayList;
import java.util.Comparator;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public enum SortingType {

	NONE(null, null), 
	NAME(new Comparator<ItemStack>() {
		@Override
		public int compare(ItemStack i1, ItemStack i2) {
			return i1.getName().getString().compareTo(i2.getName().getString());
		}
	}, new Comparator<ItemStack>() {
		@Override
		public int compare(ItemStack i1, ItemStack i2) {
			return i2.getName().getString().compareTo(i1.getName().getString());
		}
	}), 
	QUANTITY(new Comparator<ItemStack>() {
		@Override
		public int compare(ItemStack i1, ItemStack i2) {
			int ret = Integer.compare(i1.getCount(), i2.getCount());
			if (ret == 0)
				ret = i1.getName().getString().compareTo(i2.getName().getString());
			return ret;
		}
	}, new Comparator<ItemStack>() {
		@Override
		public int compare(ItemStack i1, ItemStack i2) {
			int ret = Integer.compare(i2.getCount(), i1.getCount());
			if (ret == 0)
				ret = i1.getName().getString().compareTo(i2.getName().getString());
			return ret;
		}
	}),
	ID(new Comparator<ItemStack>() {
		@Override
		public int compare(ItemStack i1, ItemStack i2) {
			int ret = Integer.compare(Item.getRawId(i1.getItem()), Item.getRawId(i2.getItem()));
			if (ret == 0)
				ret = i1.getName().getString().compareTo(i2.getName().getString());
			return ret;
		}
	}, new Comparator<ItemStack>() {
		@Override
		public int compare(ItemStack i1, ItemStack i2) {
			int ret = Integer.compare(Item.getRawId(i2.getItem()), Item.getRawId(i1.getItem()));
			if (ret == 0)
				ret = i1.getName().getString().compareTo(i2.getName().getString());
			return ret;
		}
	});

	private Comparator<ItemStack> comparatorAscending;
	private Comparator<ItemStack> comparatorDescending;

	private SortingType(Comparator<ItemStack> comparatorAscending, Comparator<ItemStack> comparatorDescending) {
		this.comparatorAscending = comparatorAscending;
		this.comparatorDescending = comparatorDescending;
	}

	public void sort(ArrayList<ItemStack> list, boolean ascending) {
		Comparator<ItemStack> comparator = ascending ? this.comparatorAscending : this.comparatorDescending;
		if (comparator != null)
			list.sort(comparator);
	}

	/**Get next sorting type in order*/
	public SortingType getNextType() {
		int ordinal = this.ordinal()+1;
		if (ordinal >= SortingType.values().length)
			ordinal = 0;
		return SortingType.values()[ordinal];
	}

}
package furgl.infinitory.impl.lists;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.Lists;

import furgl.infinitory.impl.inventory.IPlayerInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**Same as DefaultedList, except listens to when items are added/removed and adjusts inventory size*/
public class ListeningDefaultedList extends DefaultedList<ItemStack> {

	protected IPlayerInventory playerInventory;
	public boolean isEmpty = true;
	public boolean isFull = true;
	
	public static ListeningDefaultedList of(IPlayerInventory playerInventory) {
		return new ListeningDefaultedList(Lists.newArrayList(), null, playerInventory);
	}

	public static ListeningDefaultedList ofSize(int size, ItemStack defaultValue, IPlayerInventory playerInventory) {
		Validate.notNull(defaultValue);
		ItemStack[] objects = new ItemStack[size];
		Arrays.fill(objects, defaultValue);
		return new ListeningDefaultedList(Arrays.asList(objects), defaultValue, playerInventory);
	}

	protected ListeningDefaultedList(List<ItemStack> delegate, ItemStack initialElement, IPlayerInventory playerInventory) {
		super(delegate, initialElement);
		this.playerInventory = playerInventory;
	}

	/**Update empty/full status*/
	private void updateStatus() {
		this.playerInventory.needToUpdateInfinitorySize();
	}

	@Override
	public ItemStack set(int index, ItemStack element) {
		ItemStack ret = super.set(index, element);
		updateStatus();
		return ret;
	}

	@Override
	public void add(int index, ItemStack element) {
		super.add(index, element);
		updateStatus();
	}

}
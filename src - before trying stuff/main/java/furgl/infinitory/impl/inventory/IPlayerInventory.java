package furgl.infinitory.impl.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface IPlayerInventory {

	/**Infinitory's extra inventory slots*/
	public DefaultedList<ItemStack> getInfinitory();
	
	/**Get items in combined main + Infinitory*/
	public void getCombinedInfinitory();

	/**Get additional slots for this player - always multiple of 9*/
	public int getAdditionalSlots();

	/**Mark as needing to update additional slots of infinitory*/
	public void needToUpdateInfinitorySize();
	
	public SortingType getSortingType();
	
	/**Mark as needing to sort inventory*/
	public void needToSort();
	
}
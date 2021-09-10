package furgl.infinitory.impl.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface IPlayerInventory {

	/**Infinitory's extra inventory slots*/
	public DefaultedList<ItemStack> getInfinitory();
	
	/**Get items in combined main + Infinitory*/
	public void getCombinedInfinitory();
	
	/**Set item in combined main + Infinitory*/
	public void setSetInfinitory(int index, ItemStack stack);

	/**Get additional slots for this player - always multiple of 9*/
	public int getAdditionalSlots();
	
}
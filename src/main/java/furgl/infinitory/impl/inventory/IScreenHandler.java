package furgl.infinitory.impl.inventory;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface IScreenHandler {

	public void updateExtraSlots();
	
	public int getScrollbarX();
	
	public int getScrollbarMinY();
	
	public int getScrollbarMaxY();

	public ArrayList<InfinitorySlot> getInfinitorySlots();
	
}
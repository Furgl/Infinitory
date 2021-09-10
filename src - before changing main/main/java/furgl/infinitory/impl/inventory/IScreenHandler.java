package furgl.infinitory.impl.inventory;

import java.util.HashMap;

public interface IScreenHandler {

	public void addExtraSlots();
	
	public HashMap<Integer, InfinitorySlot> getMainSlots();
	
	public void clearMainSlots();
	
	public int getScrollbarX();
	
	public int getScrollbarMinY();
	
	public int getScrollbarMaxY();
	
}
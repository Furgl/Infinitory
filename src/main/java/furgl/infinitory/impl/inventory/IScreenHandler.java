package furgl.infinitory.impl.inventory;

import java.util.ArrayList;

public interface IScreenHandler {

	public ArrayList<InfinitorySlot> getMainSlots();
	
	public int getScrollbarX();
	
	public int getScrollbarMinY();
	
	public int getScrollbarMaxY();
	
}
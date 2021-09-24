package furgl.infinitory.impl.inventory;

import java.util.ArrayList;

public interface IScreenHandler {

	public void updateExtraSlots();
	
	public int getScrollbarX();
	
	public int getScrollbarMinY();
	
	public int getScrollbarMaxY();

	public ArrayList<InfinitorySlot> getInfinitorySlots();
	
}
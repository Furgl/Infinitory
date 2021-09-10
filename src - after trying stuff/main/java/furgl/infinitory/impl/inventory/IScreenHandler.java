package furgl.infinitory.impl.inventory;

import java.util.HashMap;

public interface IScreenHandler {

	public void addExtraSlots();
	
	/**Main inventory - used for scrollbar TODO switch to inventory#main*/
	public HashMap<Integer, InfinitorySlot> getMainSlots();
	
	public void clearMainSlots();
	
	public int getScrollbarX();
	
	public int getScrollbarMinY();
	
	public int getScrollbarMaxY();
	
}
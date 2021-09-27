package furgl.infinitory.impl.inventory;

public interface IPlayerInventory {
	
	public int getDifferenceInAdditionalSlots();

	/**Get additional slots for this player - always multiple of 9*/
	public int getAdditionalSlots();
	
	public void setAdditionalSlots(int additionalSlots);

	/**Mark as needing to update additional slots of infinitory*/
	public void needToUpdateInfinitorySize();
		
	/**Mark as needing to sort inventory*/
	public void needToSort();

	/**Mark as needing to update client*/
	void needToUpdateClient();
	
	public SortingType getSortingType();
	
	public void setSortingType(SortingType type);
	
	public boolean getSortingAscending();
	
	public void setSortAscending(boolean sortAscending);

	/**Recalculate additional slots based on main and infinitory sizes / fullness*/
	public void updateInfinitorySize();

	/**Sync additional slots, sorting type, and sorting ascending server -> client*/
	public void syncInfinitoryValues();
	
}
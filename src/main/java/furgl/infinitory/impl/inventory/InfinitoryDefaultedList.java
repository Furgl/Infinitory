package furgl.infinitory.impl.inventory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

/**Same as DefaultedList, except listens to when slots are added to it*/
public class InfinitoryDefaultedList<E extends Slot> extends DefaultedList<E> {

	private List<E> delegate;
	private IScreenHandler handler;

	public static InfinitoryDefaultedList<Slot> of(IScreenHandler handler) {
		return new InfinitoryDefaultedList<>(handler, Lists.newArrayList(), null);
	}

	protected InfinitoryDefaultedList(IScreenHandler handler, List delegate, E initialSlot) {
		super(delegate, initialSlot);
		this.delegate = delegate;
		this.handler = handler;
	}

	/**Modify slots before adding them*/
	private E onAdd(E slot) {		
		if (slot.inventory instanceof PlayerInventory) {
			// adding additional slots above - add to mainSlots
			if (slot instanceof InfinitorySlot) 
				this.handler.getMainSlots().put(slot.id, (InfinitorySlot) slot);
			else {
				int index = slot.getIndex();
				int x = slot.x;
				int y = slot.y;
				int id = slot.id;
				if (slot instanceof CreativeSlot) {
					index = ((CreativeSlot)slot).slot.getIndex();
					id = ((CreativeSlot)slot).slot.id;
				}
				// create new InfinitorySlot
				InfinitorySlot newSlot = new InfinitorySlot(slot, id, index, x, y, slot.getBackgroundSprite());
				// if this is in the main inventory, add to mainSlots
				if (index > 8 && index < 36) 
					this.handler.getMainSlots().put(newSlot.id, newSlot);
				// sort this list by slot.id
				this.delegate.sort(new Comparator<E>() {
					@Override
					public int compare(E e1, E e2) {
						return Integer.compare(e1.id, e2.id);
					}
				});
				return (E) newSlot;
			}
		}

		return slot;
	}

	/**Add extra slots - has to be sometime after all normal slots are added to keep correct ids*/
	private void onGet() {
		this.handler.addExtraSlots();
	}

	private Collection<? extends E> onAdd(Collection<? extends E> c) {
		List<E> ret = Lists.newArrayList();
		for (E slot : c)
			ret.add(onAdd(slot));
		return ret;
	}

	@Override
	public void add(int index, E slot) {
		super.add(index, onAdd(slot));
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return super.addAll(onAdd(c));
	}

	@Override
	public E get(int index) {
		onGet();
		return super.get(index);
	}
	
	@Override
	public void clear() {
		super.clear();
		this.handler.clearMainSlots();
	}

}
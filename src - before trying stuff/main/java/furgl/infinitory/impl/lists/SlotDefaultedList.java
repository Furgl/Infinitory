package furgl.infinitory.impl.lists;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import furgl.infinitory.impl.inventory.IScreenHandler;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

/**Same as DefaultedList, except listens to when slots are added to it*/
public class SlotDefaultedList<E extends Slot> extends DefaultedList<E> {

	private List<E> delegate;
	private IScreenHandler handler;

	public static SlotDefaultedList<Slot> of(IScreenHandler handler) {
		return new SlotDefaultedList<>(handler, Lists.newArrayList(), null);
	}

	protected SlotDefaultedList(IScreenHandler handler, List delegate, E initialSlot) {
		super(delegate, initialSlot);
		this.delegate = delegate;
		this.handler = handler;
	}

	public boolean alreadyExists(Slot slotIn) {
		for (Slot slot : delegate)
			if (slot.inventory == slotIn.inventory && slot.getIndex() == slotIn.getIndex())
				return true;
		return false;
	}

	/**Modify slots before adding them*/
	private E onAdd(E slot) {		
		System.out.println("onAdd class:"+slot.getClass().getSimpleName()+", inv: "+slot.inventory+", index: "+slot.getIndex()+", id: "+slot.id+", stack: "+slot.getStack()); // TODO remove
		if (slot.inventory instanceof PlayerInventory) {
			// adding additional slots - add to mainSlots
			if (slot instanceof InfinitorySlot) {
				if (slot.getIndex() > 40) {
					this.handler.getMainSlots().put(slot.id, (InfinitorySlot) slot);
					System.out.println("add to main slots inv: "+slot.inventory+", index: "+slot.getIndex()+", id: "+slot.id); // TODO remove
					System.out.println("main slots: "+this.handler.getMainSlots()); // TODO remove
				}
			}
			else {
				int index = slot.getIndex();
				int x = slot instanceof InfinitorySlot ? ((InfinitorySlot)slot).originalX : slot.x;
				int y = slot instanceof InfinitorySlot ? ((InfinitorySlot)slot).originalY : slot.y;
				int id = slot.id;
				if (slot instanceof CreativeSlot) {
					index = ((CreativeSlot)slot).slot.getIndex();
					id = ((CreativeSlot)slot).slot.id;
				}
				// create new InfinitorySlot
				InfinitorySlot newSlot = new InfinitorySlot(slot, id, index, x, y, slot.getBackgroundSprite());
				// if this is in the main inventory, add to mainSlots
				if (index > 8 && index < 36/* || (index > 45 && slot instanceof CreativeSlot && ((CreativeSlot)slot).slot instanceof InfinitorySlot)*/) {
					this.handler.getMainSlots().put(newSlot.id, newSlot);
					System.out.println("add to main slots2 inv: "+newSlot.inventory+", index: "+newSlot.getIndex()+", id: "+newSlot.id); // TODO remove
					System.out.println("main slots: "+this.handler.getMainSlots()); // TODO remove
				}
				// sort this list by slot.id TEST why?
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
	public boolean add(E e) {
		/*// if this slot already exists, just update it instead of adding twice (happens with creative mode removing/re-adding slots)
		for (int i=0; i<this.delegate.size(); ++i) {
			Slot slot = this.delegate.get(i);
			if (slot.getStack() == e.getStack() && slot.id == e.id && slot.inventory == e.inventory && slot.getIndex() == e.getIndex()) {
				System.out.println("ALREADY EXISTS, UPDATING: index:"+slot.getIndex()+", id:"+slot.id+", stack:"+slot.getStack()+", inv:"+slot.inventory); // TODO remove
				this.set(i, onAdd(e));
				return true;
			}
		}*/
		
		return super.add(e);
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
		if (index < this.size()) // TEST
			return super.get(index);
		else {
			System.out.println(index+" IS OUT OF BOUNDS: "+this.size()+this); // TODO remove
			return super.get(this.size()-1);
		}
	}

	@Override
	public void clear() {
		super.clear();
		this.handler.clearMainSlots();
	}

}
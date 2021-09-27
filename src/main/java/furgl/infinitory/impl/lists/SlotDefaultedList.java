package furgl.infinitory.impl.lists;

import java.util.List;

import com.google.common.collect.Lists;

import furgl.infinitory.Infinitory;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import furgl.infinitory.impl.inventory.InfinitorySlot.SlotType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

/** Same as DefaultedList, except listens to when slots are added to it */
public class SlotDefaultedList<E extends Slot> extends DefaultedList<E> {

	public List<E> delegate;

	public static SlotDefaultedList<Slot> of() {
		return new SlotDefaultedList<>(Lists.newArrayList(), null);
	}

	protected SlotDefaultedList(List delegate, E initialSlot) {
		super(delegate, initialSlot);
		this.delegate = delegate;
	}

	/** Modify slots before adding them */
	private E onAdd(E slot) {
		if (slot.inventory instanceof PlayerInventory && !(slot instanceof InfinitorySlot)) {
			int currentAdditionalSlots = this.getCurrentAdditionalSlots();
			int index = slot.getIndex();
			int x = slot.x;
			int y = slot.y;
			int id = slot.id;
			SlotType type = null;
			Slot innerSlot = Infinitory.proxy.getCreativeSlotInnerSlot(slot);
			// if creative slot
			if (innerSlot != null) {
				// copy values from slot inside CreativeSlot
				index = innerSlot.getIndex();
				id = innerSlot.id;
				if (innerSlot instanceof InfinitorySlot)
					type = ((InfinitorySlot) innerSlot).type;
				// fix slot positions for CreativeScreenHandler bc it doesn't account for extra slots
				// modified from CreativeInventoryScreen$setSelectedTab
				if (type == SlotType.OFFHAND) { // offhand
					x = 35;
					y = 20;
				}
				else if (index == 0) { // first slot of hotbar
					x = 9;
					y = 112;
				}
				else if (type == SlotType.MAIN_EXTRA) { // extra slots
					x = 9 + ((index-9) % 9) * 18;
					y = 54 + ((index-9) / 9) * 18;
				}
			}
			if (type == null)
				type = SlotType.getType(index, currentAdditionalSlots);
			// create new InfinitorySlot
			InfinitorySlot newSlot = new InfinitorySlot((PlayerInventory) slot.inventory, id, index, x, y, slot.getBackgroundSprite(), type);
			return (E) newSlot;
		}

		return slot;
	}

	/** Get current additional added slots (bc this can be called before additional slots are added) */
	public int getCurrentAdditionalSlots() {
		int i = 0;
		for (Slot slot : this.delegate) {
			Slot innerSlot = Infinitory.proxy.getCreativeSlotInnerSlot(slot);
			InfinitorySlot infinitorySlot = slot instanceof InfinitorySlot ? (InfinitorySlot) slot : innerSlot instanceof InfinitorySlot ? (InfinitorySlot) innerSlot : null;
			if (infinitorySlot != null && ((InfinitorySlot) slot).type == SlotType.MAIN_EXTRA)
				++i;
		}
		return i;
	}

	@Override
	public void add(int index, E slot) {
		super.add(index, onAdd(slot));
	}

	@Override
	public E get(int index) {
		try {
			return super.get(index);
		}
		catch (IndexOutOfBoundsException e) { // does this still happen ever?
			e.printStackTrace();
			return super.get(this.size() - 1);
		}
	}

	@Override
	public void clear() {
		// super.clear(); // normal clear will trigger onGet() and mess things up (i.e. when going from inventory creative tab -> other creative tab)
		this.delegate.clear();
	}

}
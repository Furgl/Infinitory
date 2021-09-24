package furgl.infinitory.impl.lists;

import java.util.List;

import com.google.common.collect.Lists;

import furgl.infinitory.impl.inventory.IScreenHandler;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import furgl.infinitory.impl.inventory.InfinitorySlot.SlotType;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

/** Same as DefaultedList, except listens to when slots are added to it */
public class SlotDefaultedList<E extends Slot> extends DefaultedList<E> {

	public List<E> delegate;
	private IScreenHandler handler;

	public static SlotDefaultedList<Slot> of(IScreenHandler handler) {
		return new SlotDefaultedList<>(handler, Lists.newArrayList(), null);
	}

	protected SlotDefaultedList(IScreenHandler handler, List delegate, E initialSlot) {
		super(delegate, initialSlot);
		this.delegate = delegate;
		this.handler = handler;
	}

	/** Modify slots before adding them */
	private E onAdd(E slot) {
		// System.out.println("onAdd class:" + slot.getClass().getSimpleName() + ", inv: " + slot.inventory + ", index: " + slot.getIndex() + ", id: " + slot.id + ", stack: " + slot.getStack()); // TODO remove
		if (slot.inventory instanceof PlayerInventory && !(slot instanceof InfinitorySlot)) {
			int currentAdditionalSlots = this.getCurrentAdditionalSlots();
			int index = slot.getIndex();
			int x = slot.x;
			int y = slot.y;
			int id = slot.id;
			SlotType type = null;
			if (slot instanceof CreativeSlot) {
				// copy values from slot inside CreativeSlot
				index = ((CreativeSlot) slot).slot.getIndex();
				id = ((CreativeSlot) slot).slot.id;
				if (((CreativeSlot) slot).slot instanceof InfinitorySlot)
					type = ((InfinitorySlot) ((CreativeSlot) slot).slot).type;
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
			// create new InfinitorySlot
			if (type == null)
				type = SlotType.getType(index, currentAdditionalSlots);
			//System.out.println("replacing slot:" + slot + ", stack:" + slot.getStack() + ", x:" + slot.x + ", y:" + slot.y + ", additional:" + ((IPlayerInventory) slot.inventory).getAdditionalSlots() + ", current additional: " + currentAdditionalSlots + ", id:" + id + ", index:" + index + ", type:" + type); // TODO remove
			InfinitorySlot newSlot = new InfinitorySlot(slot, id, index, x, y, slot.getBackgroundSprite(), type);
			return (E) newSlot;
		}

		return slot;
	}

	/** Get current additional added slots (bc this can be called before additional slots are added) */
	public int getCurrentAdditionalSlots() {
		int i = 0;
		// System.out.println("checking current additional: "+this.delegate); // TODO remove
		for (Slot slot : this.delegate) {
			InfinitorySlot infinitorySlot = slot instanceof InfinitorySlot ? (InfinitorySlot) slot : slot instanceof CreativeSlot && ((CreativeSlot) slot).slot instanceof InfinitorySlot ? (InfinitorySlot) ((CreativeSlot) slot).slot : null;
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
		catch (IndexOutOfBoundsException e) { // happens when logging in with extra slots bc only server knows about them - client gets told later
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
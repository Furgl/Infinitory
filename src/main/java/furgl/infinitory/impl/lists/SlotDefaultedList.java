package furgl.infinitory.impl.lists;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.impl.inventory.IScreenHandler;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import furgl.infinitory.impl.inventory.InfinitorySlot.SlotType;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
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
		if (slot.inventory instanceof PlayerInventory) {
			// adding additional slots - add to mainSlots
			/*if (slot instanceof InfinitorySlot) {
				//if (slot.getIndex() > 40) {
					//this.handler.getMainSlots().put(slot.id, (InfinitorySlot) slot);
					//System.out.println("add to main slots inv: "+slot.inventory+", index: "+slot.getIndex()+", id: "+slot.id); // TODO remove
					//System.out.println("main slots: "+this.handler.getMainSlots()); // TODO remove
				//}
			}
			else*/
			if (!(slot instanceof InfinitorySlot)) {
				int currentAdditionalSlots = this.getCurrentAdditionalSlots();
				int index = slot.getIndex();
				int x = /*slot instanceof InfinitorySlot ? ((InfinitorySlot) slot).originalX : */slot.x;
				int y = /*slot instanceof InfinitorySlot ? ((InfinitorySlot) slot).originalY : */slot.y;
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
					/*int l = index;
					int v,w,z;
					// armor
					if (l >= 36+currentAdditionalSlots && l <= 39+currentAdditionalSlots) {
						v = l - 5;
						w = v / 2;
						z = v % 2;
						x = 54 + w * 54;
						y = 6 + z * 27;
					}
					else if (l >= 0 && l < 5) {
						x = -2000;
						y = -2000;
					} 
					// fix offhand slot coords
					else if (l == 40+currentAdditionalSlots) {
						x = 35;
						y = 20;
					} 
					else {
						v = l - 9;
						w = v % 9;
						z = v / 9;
						x = 9 + w * 18;
						// fix additional slot y
						if (l >= 36+currentAdditionalSlots) 
							y = 112;
						else
							y = 54 + z * 18;
					}*/
				}
				// create new InfinitorySlot
				if (type == null)
					type = SlotType.getType(index, currentAdditionalSlots);
				//System.out.println("replacing slot:" + slot + ", stack:" + slot.getStack() + ", x:" + slot.x + ", y:" + slot.y + ", additional:" + ((IPlayerInventory) slot.inventory).getAdditionalSlots() + ", current additional: " + currentAdditionalSlots + ", id:" + id + ", index:" + index + ", type:" + type); // TODO remove
				InfinitorySlot newSlot = new InfinitorySlot(slot, id, index, x, y, slot.getBackgroundSprite(), type);
				// if this is in the main inventory, add to mainSlots
				// if (index > 8 && index < 36/* || (index > 45 && slot instanceof CreativeSlot && ((CreativeSlot)slot).slot instanceof InfinitorySlot)*/) {
				// this.handler.getMainSlots().put(newSlot.id, newSlot);
				// System.out.println("add to main slots2 inv: "+newSlot.inventory+", index: "+newSlot.getIndex()+", id: "+newSlot.id); // TODO remove
				// System.out.println("main slots: "+this.handler.getMainSlots()); // TODO remove
				// }
				return (E) newSlot;
			}
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
		if (index < this.size()) // TEST
			return super.get(index);
		else { // happens when logging in with extra slots bc only server knows about them - client gets told later
			System.out.println(index + " IS OUT OF BOUNDS: " + this.size() + this); // TODO remove
			return super.get(this.size() - 1);
		}
	}

	@Override
	public void clear() {
		// super.clear(); // normal clear will trigger onGet() and mess things up (i.e. when going from inventory creative tab -> other creative tab)
		this.delegate.clear();
	}

}
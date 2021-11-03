package furgl.infinitory.impl.lists;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;

import furgl.infinitory.Infinitory;
import furgl.infinitory.config.Config;
import furgl.infinitory.impl.inventory.IPlayerScreenHandler;
import furgl.infinitory.impl.inventory.ISlot;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import furgl.infinitory.impl.inventory.InfinitorySlot.SlotType;
import furgl.infinitory.mixin.accessors.ScreenHandlerAccessor;
import furgl.infinitory.utils.Utils;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

/** Same as DefaultedList, except listens to when slots are added to it */
public class SlotDefaultedList<E extends Slot> extends DefaultedList<E> {

	public List<E> delegate;
	/**Null in non-player inventories*/
	@Nullable
	private IPlayerScreenHandler handler;
	/**Added crafting slots - needed to know when and where to add more slots up to 3x3*/
	private ArrayList<Slot> addedCraftingSlots = Lists.newArrayList();

	public static SlotDefaultedList<Slot> of(ScreenHandler handler) {
		return new SlotDefaultedList<>(Lists.newArrayList(), null, handler);
	}

	protected SlotDefaultedList(List delegate, E initialSlot, ScreenHandler handler) {
		super(delegate, initialSlot);
		this.delegate = delegate;
		if (handler instanceof IPlayerScreenHandler)
			this.handler = (IPlayerScreenHandler)handler;
	}

	/** Modify slots before adding them */
	private E onAdd(E slot) {
		if (!(slot instanceof InfinitorySlot)) {
			// replace inventory slots with Infinitory slots
			if (slot.inventory instanceof PlayerInventory) {
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
				return (E) new InfinitorySlot(slot.inventory, ((PlayerInventory)slot.inventory).player, id, index, x, y, slot.getBackgroundSprite(), type);
			}
			// move crafting result slot for 3x3
			else if (Config.expandedCrafting && slot instanceof CraftingResultSlot) {
				((ISlot)slot).setX(Utils.CRAFTING_SLOTS_OUTPUT.getLeft());
				((ISlot)slot).setY(Utils.CRAFTING_SLOTS_OUTPUT.getRight());
			}
			// replace crafting input slots with Infinitory slots
			else if (Config.expandedCrafting && slot.inventory instanceof CraftingInventory && handler != null && !(slot instanceof CraftingResultSlot) && !handler.getPlayer().getAbilities().creativeMode) {
				int x = Utils.CRAFTING_SLOTS_INPUT.getLeft();
				int y = Utils.CRAFTING_SLOTS_INPUT.getRight();
				int index = slot.getIndex();
				if (addedCraftingSlots.size() == 3) {
					y += 18;
					index += 3;
				}
				else if (addedCraftingSlots.size() == 4) {
					x += 18;
					y += 18;
					index += 3;
				}
				else if (addedCraftingSlots.size() == 6) {
					y += 36;
					index += 4;
				}
				else if (addedCraftingSlots.size() == 7) {
					x += 18;
					y += 36;
					index += 4;
				}
				// create new InfinitorySlot
				InfinitorySlot newReturnSlot = new InfinitorySlot(slot.inventory, handler.getPlayer(), slot.id, index, x, y, slot.getBackgroundSprite(), SlotType.CRAFTING_INPUT);
				addedCraftingSlots.add(newReturnSlot);
				return (E) newReturnSlot;
			}
		}

		return slot;
	}

	/** Get current additional added slots (bc this can be called before additional slots are added) */
	public int getCurrentAdditionalSlots() {
		int i = 0;
		for (Slot slot : this.delegate) {
			Slot innerSlot = Infinitory.proxy.getCreativeSlotInnerSlot(slot);
			InfinitorySlot infinitorySlot = slot instanceof InfinitorySlot ? (InfinitorySlot) slot : 
				innerSlot instanceof InfinitorySlot ? (InfinitorySlot) innerSlot : null;
			if (infinitorySlot != null && infinitorySlot.type == SlotType.MAIN_EXTRA)
				++i;
		}
		return i;
	}

	@Override
	public void add(int index, E slot) {
		super.add(index, onAdd(slot));

		// add extra crafting slots up to 3x3
		if (Config.expandedCrafting && handler != null && !handler.getPlayer().getAbilities().creativeMode) {
			// add first row
			if (this.addedCraftingSlots.isEmpty() && slot instanceof CraftingResultSlot) {
				int x = Utils.CRAFTING_SLOTS_INPUT.getLeft();
				int y = Utils.CRAFTING_SLOTS_INPUT.getRight();
				this.addCraftingSlot(slot.id+1, 0, x, y);
				this.addCraftingSlot(slot.id+2, 1, x+18, y);
				this.addCraftingSlot(slot.id+3, 2, x+36, y);
			}
			// add last 1 to second row
			else if (this.addedCraftingSlots.size() == 5)
				this.addCraftingSlot(this.addedCraftingSlots.get(4).id+1, this.addedCraftingSlots.get(4).getIndex()+1, this.addedCraftingSlots.get(4).x+18, this.addedCraftingSlots.get(4).y);
			// add last 1 to second row
			else if (this.addedCraftingSlots.size() == 8)
				this.addCraftingSlot(this.addedCraftingSlots.get(7).id+1, this.addedCraftingSlots.get(7).getIndex()+1, this.addedCraftingSlots.get(7).x+18, this.addedCraftingSlots.get(7).y);
		}
	}

	private void addCraftingSlot(int id, int index, int x, int y) {
		Slot newSlot = new InfinitorySlot(((PlayerScreenHandler)this.handler).getCraftingInput(), handler.getPlayer(), id, index, x, y, null, SlotType.CRAFTING_INPUT);
		this.addedCraftingSlots.add(newSlot);
		((ScreenHandlerAccessor)handler).callAddSlot(newSlot);
		// fix id, bc addSlot changes it
		newSlot.id = id;
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
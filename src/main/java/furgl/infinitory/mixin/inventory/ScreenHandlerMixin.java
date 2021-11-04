package furgl.infinitory.mixin.inventory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Lists;

import furgl.infinitory.Infinitory;
import furgl.infinitory.config.Config;
import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.impl.inventory.IScreenHandler;
import furgl.infinitory.impl.inventory.ISlot;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import furgl.infinitory.impl.inventory.InfinitorySlot.SlotType;
import furgl.infinitory.impl.inventory.SortingType;
import furgl.infinitory.impl.lists.SlotDefaultedList;
import furgl.infinitory.mixin.accessors.ScreenHandlerAccessor;
import furgl.infinitory.mixin.accessors.SlotAccessor;
import furgl.infinitory.utils.Utils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ClickType;
import net.minecraft.util.collection.DefaultedList;

// <1000 priority to fix crash with Carpet mod
@Mixin(value = ScreenHandler.class, priority = 999)
public abstract class ScreenHandlerMixin implements IScreenHandler, ScreenHandlerAccessor {

	@Unique
	public int scrollbarX;
	@Unique
	public int scrollbarMinY;
	@Unique
	public int scrollbarMaxY;
	/**Make sure to adjust trinket slots to account for 3x3 crafting*/
	@Unique
	private boolean needToAdjustTrinketsSlots;

	@Shadow
	@Final
	private DefaultedList<ItemStack> trackedStacks;
	@Shadow
	@Final
	private DefaultedList<ItemStack> previousTrackedStacks;
	@Shadow
	private int quickCraftButton;
	@Shadow
	private int quickCraftStage;
	@Shadow
	@Final
	private Set<Slot> quickCraftSlots;
	/** Replace slots with our version that listens to added slots */
	@Shadow
	@Final
	@Mutable
	public DefaultedList<Slot> slots = SlotDefaultedList.of((ScreenHandler)(Object)this);

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void constructor(CallbackInfo ci) {
		needToAdjustTrinketsSlots = true;
	}
	
	@Shadow
	protected abstract void endQuickCraft();

	@Shadow
	protected abstract StackReference getCursorStackReference();

	@Unique
	@Override
	public int getScrollbarX() {
		return this.scrollbarX;
	}

	@Unique
	@Override
	public int getScrollbarMinY() {
		return this.scrollbarMinY;
	}

	@Unique
	@Override
	public int getScrollbarMaxY() {
		return this.scrollbarMaxY;
	}

	/** Get main + extra slots in main part of inventory */
	@Unique
	@Override
	public ArrayList<InfinitorySlot> getInfinitorySlots() {
		ArrayList<InfinitorySlot> slots = Lists.newArrayList();
		for (Slot slot : (List<Slot>) ((SlotDefaultedList) this.slots).delegate)
			if (slot instanceof InfinitorySlot && (((InfinitorySlot) slot).type == SlotType.MAIN_NORMAL || ((InfinitorySlot) slot).type == SlotType.MAIN_EXTRA))
				slots.add((InfinitorySlot) slot);
		return slots;
	}

	/** Same as addSlot, except inserting at index */
	@Unique
	protected InfinitorySlot addSlot(InfinitorySlot slot) {
		if (slot.id >= this.trackedStacks.size())
			this.trackedStacks.add(ItemStack.EMPTY);
		else
			this.trackedStacks.add(slot.id, ItemStack.EMPTY);
		if (slot.id >= this.previousTrackedStacks.size())
			this.previousTrackedStacks.add(ItemStack.EMPTY);
		else
			this.previousTrackedStacks.add(slot.id, ItemStack.EMPTY);
		// increment slot ids and indexes after this slot
		for (int i = 0; i < ((SlotDefaultedList) this.slots).delegate.size(); ++i) {
			Slot existingSlot = (Slot) ((SlotDefaultedList) this.slots).delegate.get(i);
			if (existingSlot.id >= slot.id)
				++existingSlot.id;
			// adjust index if slot is after this one and part of player inventory
			if (existingSlot.getIndex() >= slot.getIndex() && existingSlot.inventory instanceof PlayerInventory) 
				((ISlot) existingSlot).setIndex(existingSlot.getIndex() + 1);
		}
		if (slot.id >= this.slots.size())
			((SlotDefaultedList) this.slots).delegate.add(slot);
		else
			((SlotDefaultedList) this.slots).delegate.add(slot.id, slot);
		return slot;
	}

	@Unique
	protected InfinitorySlot removeSlot(InfinitorySlot slot) {
		if (slot.id < this.trackedStacks.size())
			this.trackedStacks.remove(slot.id);
		if (slot.id < this.previousTrackedStacks.size())
			this.previousTrackedStacks.remove(slot.id);
		if (slot.id < this.slots.size())
			((SlotDefaultedList) this.slots).delegate.remove(slot.id);
		// decrement slot ids and indexes after this slot
		for (int i = 0; i < ((SlotDefaultedList) this.slots).delegate.size(); ++i) {
			Slot existingSlot = (Slot) ((SlotDefaultedList) this.slots).delegate.get(i);
			if (existingSlot.id >= slot.id)
				--existingSlot.id;
			// adjust index if slot is after this one and part of player inventory
			if (existingSlot.getIndex() >= slot.getIndex() && existingSlot.inventory instanceof PlayerInventory) 
				((ISlot) existingSlot).setIndex(existingSlot.getIndex() - 1);
		}
		return slot;
	}

	/** Add/remove extra slots according to PlayerInventory#getAdditionalSlots */
	@Unique
	@Override
	public void updateExtraSlots() {
		// add extra slots
		ArrayList<InfinitorySlot> infinitorySlots = this.getInfinitorySlots();
		if (!infinitorySlots.isEmpty()) {
			InfinitorySlot slot = infinitorySlots.get(0); // get first slot
			int x = slot instanceof InfinitorySlot ? ((InfinitorySlot) slot).originalX : slot.x;
			int y = slot instanceof InfinitorySlot ? ((InfinitorySlot) slot).originalY : slot.y;

			// set scrollbar location
			this.scrollbarX = x + 170;
			this.scrollbarMinY = y;
			this.scrollbarMaxY = y + 54;
			// if current amount of slots doesn't match additionalSlots
			if (infinitorySlots.size() >= 27 && (infinitorySlots.size() - 27) != Utils.getAdditionalSlots(slot.player)) {				
				int difference = Utils.getAdditionalSlots(slot.player) - (infinitorySlots.size() - 27);
				
				// add extra slots
				for (int i = infinitorySlots.size() - 27; i < Utils.getAdditionalSlots(slot.player); ++i) {
					InfinitorySlot addSlot = new InfinitorySlot(slot.inventory, ((PlayerInventory) slot.inventory).player, slot.id + 27 + i, 36 + i/*41+i*/, x + (i % 9) * 18, y + (i / 9 + 3) * 18, slot.getBackgroundSprite(), SlotType.MAIN_EXTRA);
					this.addSlot(addSlot);
				}

				// remove extra slots
				for (int i = infinitorySlots.size() - 27; i > Utils.getAdditionalSlots(slot.player); --i)
					this.removeSlot(infinitorySlots.get(27 + i - 1));

				// adjust Trinkets slot IDs
				if (Infinitory.trinketsDependency != null) {
					Infinitory.trinketsDependency.adjustTrinketSlots((ScreenHandler)(Object)this, difference, slot);
					needToAdjustTrinketsSlots = false;
				}
			}
			// adjust Trinket slots to account for 3x3 crafting
			else if (Infinitory.trinketsDependency != null && needToAdjustTrinketsSlots) {
				Infinitory.trinketsDependency.adjustTrinketSlots((ScreenHandler)(Object)this, 0, slot);
				needToAdjustTrinketsSlots = false;
			}
		}
	}

	@Inject(method = "setStackInSlot", at = @At(value = "INVOKE"), cancellable = true)
	public void setStackInSlotCancelIfInvalidSlot(int slot, int revision, ItemStack stack, CallbackInfo ci) {
		if (slot >= this.slots.size()) 
			ci.cancel();
	}

	/** Copied entire method and edited by the comments because there are so many changes 
	 * @author Furgl
	 * @reason Because there are so many changes*/
	@Overwrite
	protected void internalOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		// invalid index (may happen with changing inventory size)
		if (slotIndex > ((ScreenHandler) (Object) this).slots.size() - 1)
			return;

		PlayerInventory playerInventory = player.getInventory();
		Slot slot4;
		ItemStack itemStack6;
		ItemStack itemStack2;
		int j;
		int k;
		if (actionType == SlotActionType.QUICK_CRAFT) {
			int i = this.quickCraftStage;
			this.quickCraftStage = ScreenHandler.unpackQuickCraftStage(button);
			if ((i != 1 || this.quickCraftStage != 2) && i != this.quickCraftStage) {
				this.endQuickCraft();
			} else if (((ScreenHandler) (Object) this).getCursorStack().isEmpty()) {
				this.endQuickCraft();
			} else if (this.quickCraftStage == 0) {
				this.quickCraftButton = ScreenHandler.unpackQuickCraftButton(button);
				if (ScreenHandler.shouldQuickCraftContinue(this.quickCraftButton, player)) {
					this.quickCraftStage = 1;
					this.quickCraftSlots.clear();
				} else {
					this.endQuickCraft();
				}
			} else if (this.quickCraftStage == 1) {
				slot4 = (Slot) ((ScreenHandler) (Object) this).slots.get(slotIndex);
				itemStack6 = ((ScreenHandler) (Object) this).getCursorStack();
				if (ScreenHandler.canInsertItemIntoSlot(slot4, itemStack6, true) && slot4.canInsert(itemStack6) && (this.quickCraftButton == 2 || itemStack6.getCount() > this.quickCraftSlots.size()) && ((ScreenHandler) (Object) this).canInsertIntoSlot(slot4)) {
					this.quickCraftSlots.add(slot4);
				}
			} else if (this.quickCraftStage == 2) {
				if (!this.quickCraftSlots.isEmpty()) {
					if (this.quickCraftSlots.size() == 1) {
						j = ((Slot) this.quickCraftSlots.iterator().next()).id;
						this.endQuickCraft();
						this.internalOnSlotClick(j, this.quickCraftButton, SlotActionType.PICKUP, player);
						return;
					}

					itemStack2 = ((ScreenHandler) (Object) this).getCursorStack().copy();
					k = ((ScreenHandler) (Object) this).getCursorStack().getCount();
					Iterator var9 = this.quickCraftSlots.iterator();

					label305: while (true) {
						Slot slot2;
						ItemStack itemStack3;
						do {
							do {
								do {
									do {
										if (!var9.hasNext()) {
											itemStack2.setCount(k);
											((ScreenHandler) (Object) this).setCursorStack(itemStack2);
											break label305;
										}

										slot2 = (Slot) var9.next();
										itemStack3 = ((ScreenHandler) (Object) this).getCursorStack();
									} while (slot2 == null);
								} while (!ScreenHandler.canInsertItemIntoSlot(slot2, itemStack3, true));
							} while (!slot2.canInsert(itemStack3));
						} while (this.quickCraftButton != 2 && itemStack3.getCount() < this.quickCraftSlots.size());

						if (((ScreenHandler) (Object) this).canInsertIntoSlot(slot2)) {
							ItemStack itemStack4 = itemStack2.copy();
							int l = slot2.hasStack() ? slot2.getStack().getCount() : 0;
							ScreenHandler.calculateStackSize(this.quickCraftSlots, this.quickCraftButton, itemStack4, l);
							int m = Math.min(/*itemStack4.getMaxCount()*/Config.maxStackSize, slot2.getMaxItemCount(itemStack4));
							if (itemStack4.getCount() > m) {
								itemStack4.setCount(m);
							}

							k -= itemStack4.getCount() - l;
							slot2.setStack(itemStack4);
						}
					}
				}

				this.endQuickCraft();
			} else {
				this.endQuickCraft();
			}
		} else if (this.quickCraftStage != 0) {
			this.endQuickCraft();
		} else {
			int v;
			// normal clicks or shift-click
			if ((actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) && (button == 0 || button == 1)) {
				ClickType clickType = button == 0 ? ClickType.LEFT : ClickType.RIGHT;
				if (slotIndex == ScreenHandler.EMPTY_SPACE_SLOT_INDEX) {
					if (!((ScreenHandler) (Object) this).getCursorStack().isEmpty()) {
						if (clickType == ClickType.LEFT) {
							// only allow up to 64 to be dropped at a time
							ItemStack cursorStack = ((ScreenHandler) (Object) this).getCursorStack();
							ItemStack dropStack = cursorStack.split(Math.min(cursorStack.getCount(), 64));
							player.dropItem(dropStack, true);
							// ((ScreenHandler)(Object)this).setCursorStack(ItemStack.EMPTY);
						} else {
							player.dropItem(((ScreenHandler) (Object) this).getCursorStack().split(1), true);
						}
					}
				}
				// shift-click
				else if (actionType == SlotActionType.QUICK_MOVE) {
					if (slotIndex < 0) {
						return;
					}

					slot4 = (Slot) ((ScreenHandler) (Object) this).slots.get(slotIndex);
					if (!slot4.canTakeItems(player)) {
						return;
					}

					for (itemStack6 = this.transferSlotCustom(player, slotIndex); !itemStack6.isEmpty() && ItemStack.areItemsEqualIgnoreDamage(slot4.getStack(), itemStack6); itemStack6 = this.transferSlotCustom(player, slotIndex)) {}
				}
				// normal click
				else {
					if (slotIndex < 0) {
						return;
					}

					slot4 = (Slot) ((ScreenHandler) (Object) this).slots.get(slotIndex);
					itemStack6 = slot4.getStack();
					ItemStack cursorStack = ((ScreenHandler) (Object) this).getCursorStack();
					player.onPickupSlotClick(cursorStack, slot4.getStack(), clickType); // just used for tutorial
					if (!cursorStack.onStackClicked(slot4, clickType, player) && !itemStack6.onClicked(cursorStack, slot4, clickType, player, this.getCursorStackReference())) {
						// empty slot
						if (itemStack6.isEmpty()) {
							if (!cursorStack.isEmpty()) {
								v = clickType == ClickType.LEFT ? cursorStack.getCount() : 1;
								((ScreenHandler) (Object) this).setCursorStack(slot4.insertStack(cursorStack, v));
							}
						} else if (slot4.canTakeItems(player)) {
							if (cursorStack.isEmpty()) {
								v = clickType == ClickType.LEFT ? itemStack6.getCount() : (itemStack6.getCount() + 1) / 2;
								Optional<ItemStack> optional = slot4.tryTakeStackRange(v, Integer.MAX_VALUE, player);
								optional.ifPresent((stack) -> {
									((ScreenHandler) (Object) this).setCursorStack(stack);
									slot4.onTakeItem(player, stack);
								});
							} else if (slot4.canInsert(cursorStack)) {
								if (ItemStack.canCombine(itemStack6, cursorStack)) {
									v = clickType == ClickType.LEFT ? cursorStack.getCount() : 1;
									((ScreenHandler) (Object) this).setCursorStack(slot4.insertStack(cursorStack, v));
								} else if (cursorStack.getCount() <= slot4.getMaxItemCount(cursorStack)) {
									// while sorting, instead of swapping, just insert cursorStack
									if (slot4.inventory instanceof IPlayerInventory && ((IPlayerInventory) slot4.inventory).getSortingType() != SortingType.NONE && slot4 instanceof InfinitorySlot && (((InfinitorySlot) slot4).type == SlotType.MAIN_NORMAL || ((InfinitorySlot) slot4).type == SlotType.MAIN_EXTRA)) {
										ItemStack cursorStackCopy = cursorStack.copy();
										ItemStack insertingStack = cursorStackCopy.split(clickType == ClickType.LEFT ? cursorStackCopy.getCount() : 1);
										if (this.callInsertItem(insertingStack, 9, 36 + Utils.getAdditionalSlots(player), false))
											((ScreenHandler) (Object) this).setCursorStack(cursorStackCopy);
									} else {
										slot4.setStack(cursorStack);
										((ScreenHandler) (Object) this).setCursorStack(itemStack6);
									}
								}
							} else if (ItemStack.canCombine(itemStack6, cursorStack)) {
								Optional<ItemStack> optional2 = slot4.tryTakeStackRange(itemStack6.getCount(), /*itemStack7.getMaxCount()*/Config.maxStackSize - cursorStack.getCount(), player);
								optional2.ifPresent((stack) -> {
									cursorStack.increment(stack.getCount());
									slot4.onTakeItem(player, stack);
								});
							}
						}
					}

					slot4.markDirty();
				}
			} else {
				Slot slot5;
				int u;
				if (actionType == SlotActionType.SWAP) {
					slot5 = (Slot) ((ScreenHandler) (Object) this).slots.get(slotIndex);
					itemStack2 = playerInventory.getStack(button);
					itemStack6 = slot5.getStack();
					if (!itemStack2.isEmpty() || !itemStack6.isEmpty()) {
						if (itemStack2.isEmpty()) {
							if (slot5.canTakeItems(player)) {
								playerInventory.setStack(button, itemStack6);
								((SlotAccessor) slot5).callOnTake(itemStack6.getCount());
								slot5.setStack(ItemStack.EMPTY);
								slot5.onTakeItem(player, itemStack6);
							}
						} else if (itemStack6.isEmpty()) {
							if (slot5.canInsert(itemStack2)) {
								u = slot5.getMaxItemCount(itemStack2);
								if (itemStack2.getCount() > u) {
									slot5.setStack(itemStack2.split(u));
								} else {
									slot5.setStack(itemStack2);
									playerInventory.setStack(button, ItemStack.EMPTY);
								}
							}
						} else if (slot5.canTakeItems(player) && slot5.canInsert(itemStack2)) {
							u = slot5.getMaxItemCount(itemStack2);
							if (itemStack2.getCount() > u) {
								slot5.setStack(itemStack2.split(u));
								slot5.onTakeItem(player, itemStack6);
								if (!playerInventory.insertStack(itemStack6)) {
									player.dropItem(itemStack6, true);
								}
							} else {
								slot5.setStack(itemStack2);
								playerInventory.setStack(button, itemStack6);
								slot5.onTakeItem(player, itemStack6);
							}
						}
					}
				} else if (actionType == SlotActionType.CLONE && player.getAbilities().creativeMode && ((ScreenHandler) (Object) this).getCursorStack().isEmpty() && slotIndex >= 0) {
					slot5 = (Slot) ((ScreenHandler) (Object) this).slots.get(slotIndex);
					if (slot5.hasStack()) {
						itemStack2 = slot5.getStack().copy();
						// cloning gives max of count or normal max count (rather than just normal max count)
						itemStack2.setCount(Math.max(itemStack2.getCount(), itemStack2.getMaxCount()));
						((ScreenHandler) (Object) this).setCursorStack(itemStack2);
					}
				} else if (actionType == SlotActionType.THROW && ((ScreenHandler) (Object) this).getCursorStack().isEmpty() && slotIndex >= 0) {
					slot5 = (Slot) ((ScreenHandler) (Object) this).slots.get(slotIndex);
					j = button == 0 ? 1 : slot5.getStack().getCount();
					// only allow up to normal max count to be thrown at a time
					itemStack6 = slot5.takeStackRange(j, 64/*Config.maxStackSize*/, player);
					player.dropItem(itemStack6, true);
				} else if (actionType == SlotActionType.PICKUP_ALL && slotIndex >= 0) {
					slot5 = ((ScreenHandler) (Object) this).slots.get(slotIndex);
					itemStack2 = ((ScreenHandler) (Object) this).getCursorStack();
					if (!itemStack2.isEmpty() && (!slot5.hasStack() || !slot5.canTakeItems(player))) {
						k = button == 0 ? 0 : ((ScreenHandler) (Object) this).slots.size() - 1;
						u = button == 0 ? 1 : -1;

						for (v = 0; v < 2; ++v) {
							for (int w = k; w >= 0 && w < ((ScreenHandler) (Object) this).slots.size() && itemStack2.getCount() < /*itemStack2.getMaxCount()*/Config.maxStackSize; w += u) {
								Slot slot9 = (Slot) ((ScreenHandler) (Object) this).slots.get(w);
								if (slot9.hasStack() && ScreenHandler.canInsertItemIntoSlot(slot9, itemStack2, true) && slot9.canTakeItems(player) && ((ScreenHandler) (Object) this).canInsertIntoSlot(itemStack2, slot9)) {
									ItemStack itemStack13 = slot9.getStack();
									if (v != 0 || itemStack13.getCount() != /*itemStack13.getMaxCount()*/Config.maxStackSize) {
										ItemStack itemStack14 = slot9.takeStackRange(itemStack13.getCount(), /*itemStack2.getMaxCount()*/Config.maxStackSize - itemStack2.getCount(), player);
										itemStack2.increment(itemStack14.getCount());
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Overwrite
	public boolean insertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast) {
		// if trying to insert into main inventory, change endIndex to include expanded main inventory
		Slot slot = this.slots.get(startIndex);
		if (slot instanceof InfinitorySlot && ((InfinitorySlot)slot).type == SlotType.MAIN_NORMAL &&
				(endIndex == startIndex + 27 || endIndex == startIndex + 36)) 
			//   ^-- inserting into only main inventory or main + hotbar
			endIndex += ((IPlayerInventory)slot.inventory).getAdditionalSlots();

		boolean bl = false;
		int i = startIndex;
		if (fromLast) {
			i = endIndex - 1;
		}

		Slot slot2;
		ItemStack itemStack;
		/** Allow non-stackables to stack when shift-clicking */
		if (true/*stack.isStackable()*/) {
			while(!stack.isEmpty()) {
				if (fromLast) {
					if (i < startIndex) {
						break;
					}
				} else if (i >= endIndex) {
					break;
				}

				slot2 = (Slot)this.slots.get(i);
				itemStack = slot2.getStack();
				if (!itemStack.isEmpty() && canCombineAndInsertIntoSlot(stack, itemStack)) {
					int j = itemStack.getCount() + stack.getCount();
					if (j <= this.getMaxCountWhileInserting(stack)) {
						stack.setCount(0);
						itemStack.setCount(j);
						slot2.markDirty();
						bl = true;
					} else if (itemStack.getCount() < this.getMaxCountWhileInserting(stack)) {
						stack.decrement(this.getMaxCountWhileInserting(stack) - itemStack.getCount());
						itemStack.setCount(this.getMaxCountWhileInserting(stack));
						slot2.markDirty();
						bl = true;
					}
				}

				if (fromLast) {
					--i;
				} else {
					++i;
				}
			}
		}

		if (!stack.isEmpty()) {
			if (fromLast) {
				i = endIndex - 1;
			} else {
				i = startIndex;
			}

			while(true) {
				if (fromLast) {
					if (i < startIndex) {
						break;
					}
				} else if (i >= endIndex) {
					break;
				}

				slot2 = (Slot)this.slots.get(i);
				itemStack = slot2.getStack();
				if (itemStack.isEmpty() && slot2.canInsert(stack)) {	  
					/** Only insert what slot allows (i.e. moving stack of non-stackables from main inventory to hotbar) */
					if (stack.getCount() > slot2.getMaxItemCount(stack)) // MC just uses slot2.getMaxItemCount()
						slot2.setStack(stack.split(slot2.getMaxItemCount(stack))); // MC just uses slot2.getMaxItemCount()
					else 
						slot2.setStack(stack.split(stack.getCount()));

					slot2.markDirty();
					bl = true;
					break;
				}

				if (fromLast) {
					--i;
				} else {
					++i;
				}
			}
		}

		return bl;
	}

	/** Only insert what slot allows (i.e. moving stack of non-stackables from main inventory to hotbar) */
	@Unique
	private boolean canCombineAndInsertIntoSlot(ItemStack stack, ItemStack otherStack) {
		boolean ret = ItemStack.canCombine(stack, otherStack);
		if (ret)
			for (Slot slot : this.slots)
				if (slot != null && slot.inventory instanceof PlayerInventory && slot.getStack() == otherStack) {
					ret &= slot.getMaxItemCount(stack) > otherStack.getCount();
					break;
				}
		return ret;
	}

	/** Inserting item from other inventory -> player inventory, allow up to MAX_VALUE stacks */
	@Unique
	protected int getMaxCountWhileInserting(ItemStack stack) {
		// if this stack is currently in the player's inventory, then we're inserting into another inventory, so only allow up to stack.getMaxCount()
		boolean inPlayerInventory = false;
		for (Slot slot : this.slots)
			if (slot != null && slot.inventory instanceof PlayerInventory && slot.getStack() == stack) {
				inPlayerInventory = true;
				break;
			}
		return inPlayerInventory ? stack.getMaxCount() : Integer.MAX_VALUE;
	}

	@Inject(method = "canInsertItemIntoSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"), cancellable = true)
	private static void getMaxCountCanInsertItemIntoSlot(@Nullable Slot slot, ItemStack stack, boolean allowOverflow, CallbackInfoReturnable<Boolean> ci) {
		if (slot instanceof InfinitorySlot)
			ci.setReturnValue(slot.getStack().getCount() + (allowOverflow ? 0 : stack.getCount()) <= Config.maxStackSize);
		else
			ci.setReturnValue(slot.getStack().getCount() + (allowOverflow ? 0 : stack.getCount()) <= stack.getMaxCount());
	}

	@Inject(method = "syncState()V", at = @At(value = "INVOKE"))
	public void syncStateAddExtraSlots(CallbackInfo ci) {
		this.updateExtraSlots(); // update extra slots before syncing
		ArrayList<InfinitorySlot> infinitorySlots = this.getInfinitorySlots();
		if (!infinitorySlots.isEmpty()) {
			InfinitorySlot slot = infinitorySlots.get(0); // get first slot
			((IPlayerInventory) slot.inventory).syncInfinitoryValues(); // sync values
		}
	}

	/** Use this method instead of {@link ScreenHandler#transferSlot(PlayerEntity, int)} */
	@Unique
	protected ItemStack transferSlotCustom(PlayerEntity player, int slotIndex) {
		return ((ScreenHandler) (Object) this).transferSlot(player, slotIndex);
	}

}
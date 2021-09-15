package furgl.infinitory.mixin.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import furgl.infinitory.config.Config;
import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.impl.inventory.IScreenHandler;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import furgl.infinitory.impl.lists.SlotDefaultedList;
import furgl.infinitory.utils.Utils;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ClickType;
import net.minecraft.util.collection.DefaultedList;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin implements IScreenHandler, ScreenHandlerAccessor {

	/**Main inventory slots*/
	@Unique
	public HashMap<Integer, InfinitorySlot> mainSlots = Maps.newHashMap();
	@Unique
	public int scrollbarX;
	@Unique
	public int scrollbarMinY;
	@Unique
	public int scrollbarMaxY;
	@Unique
	private ArrayList<Slot> addedSlots = Lists.newArrayList();

	@Shadow
	private int quickCraftButton;
	@Shadow  
	private int quickCraftStage;
	@Shadow @Final
	private Set<Slot> quickCraftSlots;
	/**Replace slots with our version that listens to added slots*/
	@Shadow @Final @Mutable
	public DefaultedList<Slot> slots = SlotDefaultedList.of(this);

	@Shadow
	protected abstract void endQuickCraft();
	@Shadow
	protected abstract StackReference getCursorStackReference();

	@Unique
	@Override
	public HashMap<Integer, InfinitorySlot> getMainSlots() {
		return this.mainSlots;
	}

	@Unique
	@Override
	public void clearMainSlots() {
		this.mainSlots.clear();
		this.addedSlots.clear();
	}

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

	@Unique
	@Override
	public void addExtraSlots() { 
		//System.out.println("check extra slots: "+this.slots.size()); // TODO remove
		// add extra slots
		if (this.mainSlots.size() >= 27) {
			InfinitorySlot slot = this.mainSlots.entrySet().iterator().next().getValue(); // get first slot
			// if current amount of slots doesn't match additionalSlots
			if (this.addedSlots.size() != Utils.getAdditionalSlots(slot.player)) {
				//System.out.println("add extra slots: "+this.slots.size()+", difference: "+(Utils.getAdditionalSlots(slot.player)-this.addedSlots.size())+", additional: "+Utils.getAdditionalSlots(slot.player)+", main size: "+this.mainSlots.size()+", infinitory: "+((IPlayerInventory)slot.player.getInventory()).getInfinitory()); // TODO remove
				int x = slot instanceof InfinitorySlot ? ((InfinitorySlot)slot).originalX : slot.x;
				int y = slot instanceof InfinitorySlot ? ((InfinitorySlot)slot).originalY : slot.y;
				// add extra slots
				for (int i=this.addedSlots.size(); i<Utils.getAdditionalSlots(slot.player); ++i) { // is this the correct id to use? does it matter?
					InfinitorySlot addSlot = new InfinitorySlot(slot, slot.id+37+i, 41+i, x + (i % 9) * 18, y + (i / 9 + 3) * 18, slot.getBackgroundSprite());
					if ((Object)this instanceof CreativeScreenHandler) 
						((ScreenHandler)(Object)this).slots.add(addSlot); // creative slots don't call addSlot() when added (they're just wrapped in CreativeSlot)
					else 
						this.callAddSlot(addSlot); // THIS NEEDS TO HAPPEN AFTER ALL VANILLA SLOTS ARE ADDED OR ELSE ID -> SLOT GETS MIXED UP
					this.addedSlots.add(addSlot);
				}
				// remove extra slots
				while (this.addedSlots.size() > Utils.getAdditionalSlots(slot.player)) {
					Slot removeSlot = this.addedSlots.remove(this.addedSlots.size()-1);
					this.slots.remove(removeSlot);
					this.mainSlots.remove(removeSlot.id);
				}
				// update infinitory size if needed
				DefaultedList<ItemStack> infinitory = ((IPlayerInventory)slot.player.getInventory()).getInfinitory();
				while (infinitory.size() < Utils.getAdditionalSlots(slot.player))
					infinitory.add(ItemStack.EMPTY);
				while (infinitory.size() > Utils.getAdditionalSlots(slot.player) && infinitory.get(infinitory.size()-1).isEmpty())
					infinitory.remove(infinitory.size()-1);
				// set scrollbar location
				this.scrollbarX = x + 170;
				this.scrollbarMinY = y;
				this.scrollbarMaxY = y + 54;
				//System.out.println("after add extra slots: "+this.slots.size()+", difference: "+(Utils.getAdditionalSlots(slot.player)-this.addedSlots.size())+", additional: "+Utils.getAdditionalSlots(slot.player)+", main size: "+this.mainSlots.size()+", infinitory: "+((IPlayerInventory)slot.player.getInventory()).getInfinitory()); // TODO remove
			}
		}
	}

	/**Copied entire method and edited by the comments because there are so many changes*/
	@Inject(method = "internalOnSlotClick", at = @At(value = "INVOKE"), cancellable = true)
	public void internalOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
		//System.out.println("index: "+slotIndex); // TODO remove
		// invalid index (may happen with changing inventory size)
		if (slotIndex > ((ScreenHandler)(Object)this).slots.size()-1)
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
			} else if (((ScreenHandler)(Object)this).getCursorStack().isEmpty()) {
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
				slot4 = (Slot)((ScreenHandler)(Object)this).slots.get(slotIndex);
				itemStack6 = ((ScreenHandler)(Object)this).getCursorStack();
				if (ScreenHandler.canInsertItemIntoSlot(slot4, itemStack6, true) && slot4.canInsert(itemStack6) && (this.quickCraftButton == 2 || itemStack6.getCount() > this.quickCraftSlots.size()) && ((ScreenHandler)(Object)this).canInsertIntoSlot(slot4)) {
					this.quickCraftSlots.add(slot4);
				}
			} else if (this.quickCraftStage == 2) {
				if (!this.quickCraftSlots.isEmpty()) {
					if (this.quickCraftSlots.size() == 1) {
						j = ((Slot)this.quickCraftSlots.iterator().next()).id;
						this.endQuickCraft();
						this.internalOnSlotClick(j, this.quickCraftButton, SlotActionType.PICKUP, player, ci);
						return;
					}

					itemStack2 = ((ScreenHandler)(Object)this).getCursorStack().copy();
					k = ((ScreenHandler)(Object)this).getCursorStack().getCount();
					Iterator var9 = this.quickCraftSlots.iterator();

					label305:
						while(true) {
							Slot slot2;
							ItemStack itemStack3;
							do {
								do {
									do {
										do {
											if (!var9.hasNext()) {
												itemStack2.setCount(k);
												((ScreenHandler)(Object)this).setCursorStack(itemStack2);
												break label305;
											}

											slot2 = (Slot)var9.next();
											itemStack3 = ((ScreenHandler)(Object)this).getCursorStack();
										} while(slot2 == null);
									} while(!ScreenHandler.canInsertItemIntoSlot(slot2, itemStack3, true));
								} while(!slot2.canInsert(itemStack3));
							} while(this.quickCraftButton != 2 && itemStack3.getCount() < this.quickCraftSlots.size());

							if (((ScreenHandler)(Object)this).canInsertIntoSlot(slot2)) {
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
					if (!((ScreenHandler)(Object)this).getCursorStack().isEmpty()) {
						if (clickType == ClickType.LEFT) {
							// only allow up to normal max stack count to be dropped at a time
							ItemStack cursorStack = ((ScreenHandler)(Object)this).getCursorStack();
							ItemStack dropStack = cursorStack.split(Math.min(cursorStack.getCount(), cursorStack.getMaxCount()));
							player.dropItem(dropStack, true);
							//((ScreenHandler)(Object)this).setCursorStack(ItemStack.EMPTY);
						} 
						else {
							player.dropItem(((ScreenHandler)(Object)this).getCursorStack().split(1), true);
						}
					}
				} 
				// shift-click
				else if (actionType == SlotActionType.QUICK_MOVE) {
					if (slotIndex < 0) {
						return;
					}

					slot4 = (Slot)((ScreenHandler)(Object)this).slots.get(slotIndex);
					if (!slot4.canTakeItems(player)) {
						return;
					}						

					for(itemStack6 = this.transferSlot(player, slotIndex); !itemStack6.isEmpty() && ItemStack.areItemsEqualIgnoreDamage(slot4.getStack(), itemStack6); itemStack6 = this.transferSlot(player, slotIndex)) {
					}
				} 
				// normal click
				else {
					if (slotIndex < 0) {
						return;
					}

					slot4 = (Slot)((ScreenHandler)(Object)this).slots.get(slotIndex);
					itemStack6 = slot4.getStack();
					ItemStack cursorStack = ((ScreenHandler)(Object)this).getCursorStack();
					player.onPickupSlotClick(cursorStack, slot4.getStack(), clickType); // just used for tutorial
					if (!cursorStack.onStackClicked(slot4, clickType, player) && !itemStack6.onClicked(cursorStack, slot4, clickType, player, this.getCursorStackReference())) {
						// empty slot
						if (itemStack6.isEmpty()) {
							if (!cursorStack.isEmpty()) {
								v = clickType == ClickType.LEFT ? cursorStack.getCount() : 1;
								((ScreenHandler)(Object)this).setCursorStack(slot4.insertStack(cursorStack, v));
							}
						} 
						else if (slot4.canTakeItems(player)) {
							if (cursorStack.isEmpty()) {
								v = clickType == ClickType.LEFT ? itemStack6.getCount() : (itemStack6.getCount() + 1) / 2;
								Optional<ItemStack> optional = slot4.tryTakeStackRange(v, Integer.MAX_VALUE, player);
								optional.ifPresent((stack) -> {
									((ScreenHandler)(Object)this).setCursorStack(stack);
									slot4.onTakeItem(player, stack);
								});
							} 
							else if (slot4.canInsert(cursorStack)) {
								if (ItemStack.canCombine(itemStack6, cursorStack)) {
									v = clickType == ClickType.LEFT ? cursorStack.getCount() : 1;
									((ScreenHandler)(Object)this).setCursorStack(slot4.insertStack(cursorStack, v));
								} else if (cursorStack.getCount() <= slot4.getMaxItemCount(cursorStack)) {
									slot4.setStack(cursorStack);
									((ScreenHandler)(Object)this).setCursorStack(itemStack6);
								}
							} 
							else if (ItemStack.canCombine(itemStack6, cursorStack)) {
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
			} 
			else {
				Slot slot5;
				int u;
				if (actionType == SlotActionType.SWAP) {
					slot5 = (Slot)((ScreenHandler)(Object)this).slots.get(slotIndex);
					itemStack2 = playerInventory.getStack(button);
					itemStack6 = slot5.getStack();
					if (!itemStack2.isEmpty() || !itemStack6.isEmpty()) {
						if (itemStack2.isEmpty()) {
							if (slot5.canTakeItems(player)) {
								playerInventory.setStack(button, itemStack6);
								((SlotAccessor)slot5).callOnTake(itemStack6.getCount());
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
				} else if (actionType == SlotActionType.CLONE && player.getAbilities().creativeMode && ((ScreenHandler)(Object)this).getCursorStack().isEmpty() && 
						slotIndex >= 0) {
					slot5 = (Slot)((ScreenHandler)(Object)this).slots.get(slotIndex);
					if (slot5.hasStack()) {
						itemStack2 = slot5.getStack().copy();
						// cloning gives max of count or normal max count (rather than just normal max count)
						itemStack2.setCount(Math.max(itemStack2.getCount(), itemStack2.getMaxCount()));
						((ScreenHandler)(Object)this).setCursorStack(itemStack2);
					}
				} else if (actionType == SlotActionType.THROW && ((ScreenHandler)(Object)this).getCursorStack().isEmpty() && slotIndex >= 0) {
					slot5 = (Slot)((ScreenHandler)(Object)this).slots.get(slotIndex);
					j = button == 0 ? 1 : slot5.getStack().getCount();
					// only allow up to normal max count to be thrown at a time
					itemStack6 = slot5.takeStackRange(j, slot5.getStack().getMaxCount()/*Config.maxStackSize*/, player);
					player.dropItem(itemStack6, true);
				} else if (actionType == SlotActionType.PICKUP_ALL && slotIndex >= 0) {
					slot5 = ((ScreenHandler)(Object)this).slots.get(slotIndex);
					itemStack2 = ((ScreenHandler)(Object)this).getCursorStack();
					if (!itemStack2.isEmpty() && (!slot5.hasStack() || !slot5.canTakeItems(player))) {
						k = button == 0 ? 0 : ((ScreenHandler)(Object)this).slots.size() - 1;
						u = button == 0 ? 1 : -1;

						for(v = 0; v < 2; ++v) {
							for(int w = k; w >= 0 && w < ((ScreenHandler)(Object)this).slots.size() && itemStack2.getCount() < /*itemStack2.getMaxCount()*/Config.maxStackSize; w += u) {
								Slot slot9 = (Slot)((ScreenHandler)(Object)this).slots.get(w);
								if (slot9.hasStack() && ScreenHandler.canInsertItemIntoSlot(slot9, itemStack2, true) && slot9.canTakeItems(player) && ((ScreenHandler)(Object)this).canInsertIntoSlot(itemStack2, slot9)) {
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

		ci.cancel();
	}

	/**Only insert what slot allows (i.e. moving stack of non-stackables from main inventory to hotbar)*/
	@Inject(method = "insertItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;canInsert(Lnet/minecraft/item/ItemStack;)Z", shift = Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
	public void canInsertInsertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast, CallbackInfoReturnable<Boolean> ci, boolean bl, int i, Slot slot2) {
		// MC just uses slot2.getMaxItemCount()
		if (stack.getCount() > slot2.getMaxItemCount(stack)) {
			slot2.setStack(stack.split(slot2.getMaxItemCount(stack)));
			slot2.markDirty();
			ci.setReturnValue(true);
			ci.cancel();
		}
	}

	/**Only insert what slot allows (i.e. moving stack of non-stackables from main inventory to hotbar)*/
	@Redirect(method = "insertItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;canCombine(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
	public boolean canCombineInsertItem(ItemStack stack, ItemStack otherStack) {
		boolean ret = ItemStack.canCombine(stack, otherStack);
		if (ret) 
			for (Slot slot : this.slots)
				if (slot != null && slot.inventory instanceof PlayerInventory && slot.getStack() == otherStack) {
					ret &= slot.getMaxItemCount(stack) > otherStack.getCount();
					break;
				}
		return ret;
	}

	/**Allow non-stackables to stack when shift-clicking*/
	@Redirect(method = "insertItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isStackable()Z"))
	public boolean isStackableInsertItem(ItemStack stack) {
		return true;
	}

	/**Inserting item from other inventory -> player inventory, allow up to MAX_VALUE stacks*/
	@Redirect(method = "insertItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"))
	public int getMaxCountInsertItem(ItemStack stack) {
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

	/**Use this method instead of {@link ScreenHandler#transferSlot(PlayerEntity, int)}*/
	@Unique
	protected ItemStack transferSlot(PlayerEntity player, int slotIndex) {
		return ((ScreenHandler)(Object)this).transferSlot(player, slotIndex);
	}

}
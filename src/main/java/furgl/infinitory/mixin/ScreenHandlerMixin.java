package furgl.infinitory.mixin;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import furgl.infinitory.interfaces.ISlotMixin;
import furgl.infinitory.slots.InfinitorySlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ClickType;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

	// FIXME Ctrl+Q outside inventory dropping full stack

	@Shadow
	private int quickCraftButton;
	@Shadow  
	private int quickCraftStage;
	@Shadow @Final
	private Set<Slot> quickCraftSlots;

	@Shadow
	protected abstract void endQuickCraft();
	@Shadow
	protected abstract StackReference getCursorStackReference();

	/**Copied entire method and edited by the comments because there are so many changes*/
	@Inject(method = "internalOnSlotClick", at = @At(value = "INVOKE"), cancellable = true)
	public void internalOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
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
								int m = Math.min(/*itemStack4.getMaxCount()*/Integer.MAX_VALUE, slot2.getMaxItemCount(itemStack4));
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
						} else {
							player.dropItem(((ScreenHandler)(Object)this).getCursorStack().split(1), true);
						}
					}
				} else if (actionType == SlotActionType.QUICK_MOVE) {
					if (slotIndex < 0) {
						return;
					}

					slot4 = (Slot)((ScreenHandler)(Object)this).slots.get(slotIndex);
					if (!slot4.canTakeItems(player)) {
						return;
					}

					for(itemStack6 = ((ScreenHandler)(Object)this).transferSlot(player, slotIndex); !itemStack6.isEmpty() && ItemStack.areItemsEqualIgnoreDamage(slot4.getStack(), itemStack6); itemStack6 = ((ScreenHandler)(Object)this).transferSlot(player, slotIndex)) {
					}
				} else {
					if (slotIndex < 0) {
						return;
					}

					slot4 = (Slot)((ScreenHandler)(Object)this).slots.get(slotIndex);
					itemStack6 = slot4.getStack();
					ItemStack itemStack7 = ((ScreenHandler)(Object)this).getCursorStack();
					player.onPickupSlotClick(itemStack7, slot4.getStack(), clickType);
					if (!itemStack7.onStackClicked(slot4, clickType, player) && !itemStack6.onClicked(itemStack7, slot4, clickType, player, this.getCursorStackReference())) {
						if (itemStack6.isEmpty()) {
							if (!itemStack7.isEmpty()) {
								v = clickType == ClickType.LEFT ? itemStack7.getCount() : 1;
								((ScreenHandler)(Object)this).setCursorStack(slot4.insertStack(itemStack7, v));
							}
						} else if (slot4.canTakeItems(player)) {
							if (itemStack7.isEmpty()) {
								v = clickType == ClickType.LEFT ? itemStack6.getCount() : (itemStack6.getCount() + 1) / 2;
								Optional<ItemStack> optional = slot4.tryTakeStackRange(v, Integer.MAX_VALUE, player);
								optional.ifPresent((stack) -> {
									((ScreenHandler)(Object)this).setCursorStack(stack);
									slot4.onTakeItem(player, stack);
								});
							} else if (slot4.canInsert(itemStack7)) {
								if (ItemStack.canCombine(itemStack6, itemStack7)) {
									v = clickType == ClickType.LEFT ? itemStack7.getCount() : 1;
									((ScreenHandler)(Object)this).setCursorStack(slot4.insertStack(itemStack7, v));
								} else if (itemStack7.getCount() <= slot4.getMaxItemCount(itemStack7)) {
									slot4.setStack(itemStack7);
									((ScreenHandler)(Object)this).setCursorStack(itemStack6);
								}
							} else if (ItemStack.canCombine(itemStack6, itemStack7)) {
								Optional<ItemStack> optional2 = slot4.tryTakeStackRange(itemStack6.getCount(), /*itemStack7.getMaxCount()*/Integer.MAX_VALUE - itemStack7.getCount(), player);
								optional2.ifPresent((stack) -> {
									itemStack7.increment(stack.getCount());
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
					slot5 = (Slot)((ScreenHandler)(Object)this).slots.get(slotIndex);
					itemStack2 = playerInventory.getStack(button);
					itemStack6 = slot5.getStack();
					if (!itemStack2.isEmpty() || !itemStack6.isEmpty()) {
						if (itemStack2.isEmpty()) {
							if (slot5.canTakeItems(player)) {
								playerInventory.setStack(button, itemStack6);
								((ISlotMixin)slot5).onTake(itemStack6.getCount());
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
				} else if (actionType == SlotActionType.CLONE && player.getAbilities().creativeMode && ((ScreenHandler)(Object)this).getCursorStack().isEmpty() && slotIndex >= 0) {
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
					itemStack6 = slot5.takeStackRange(j, slot5.getStack().getMaxCount()/*Integer.MAX_VALUE*/, player);
					player.dropItem(itemStack6, true);
				} else if (actionType == SlotActionType.PICKUP_ALL && slotIndex >= 0) {
					slot5 = (Slot)((ScreenHandler)(Object)this).slots.get(slotIndex);
					itemStack2 = ((ScreenHandler)(Object)this).getCursorStack();
					if (!itemStack2.isEmpty() && (!slot5.hasStack() || !slot5.canTakeItems(player))) {
						k = button == 0 ? 0 : ((ScreenHandler)(Object)this).slots.size() - 1;
						u = button == 0 ? 1 : -1;

						for(v = 0; v < 2; ++v) {
							for(int w = k; w >= 0 && w < ((ScreenHandler)(Object)this).slots.size() && itemStack2.getCount() < /*itemStack2.getMaxCount()*/Integer.MAX_VALUE; w += u) {
								Slot slot9 = (Slot)((ScreenHandler)(Object)this).slots.get(w);
								if (slot9.hasStack() && ScreenHandler.canInsertItemIntoSlot(slot9, itemStack2, true) && slot9.canTakeItems(player) && ((ScreenHandler)(Object)this).canInsertIntoSlot(itemStack2, slot9)) {
									ItemStack itemStack13 = slot9.getStack();
									if (v != 0 || itemStack13.getCount() != /*itemStack13.getMaxCount()*/Integer.MAX_VALUE) {
										ItemStack itemStack14 = slot9.takeStackRange(itemStack13.getCount(), /*itemStack2.getMaxCount()*/Integer.MAX_VALUE - itemStack2.getCount(), player);
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

	@Redirect(method = "insertItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"))
	public int getMaxCountInsertItem(ItemStack stack) {
		return stack.getMaxCount();
	}

	@Inject(method = "canInsertItemIntoSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"), cancellable = true)
	private static void getMaxCountCanInsertItemIntoSlot(@Nullable Slot slot, ItemStack stack, boolean allowOverflow,
			CallbackInfoReturnable<Boolean> ci) {
		if (slot instanceof InfinitorySlot)
			ci.setReturnValue(slot.getStack().getCount() + (allowOverflow ? 0 : stack.getCount()) <= Integer.MAX_VALUE);
		else
			ci.setReturnValue(slot.getStack().getCount() + (allowOverflow ? 0 : stack.getCount()) <= stack.getMaxCount());
	}

	/**Replace player slots in containers (not player inventory - that's handled in PlayerScreenHandlerMixin*/
	@ModifyVariable(method = "addSlot", at = @At(value = "HEAD"), ordinal = 0)
	public Slot changeSlot(Slot slot) {
		if (slot.inventory instanceof PlayerInventory) 
			return new InfinitorySlot(slot.inventory, slot.getIndex(), slot.x, slot.y, slot.getBackgroundSprite());
		else 
			return slot;
	}

}
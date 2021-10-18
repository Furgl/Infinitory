package furgl.infinitory.impl.dependencies;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.data.EntitySlotLoader;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import furgl.infinitory.utils.Utils;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;

public class TrinketsDependency implements Dependency {
	
	/**Original slotIds for trinket SlotGroups*/
	private HashMap<String, Integer> trinketSlotGroupIds = Maps.newHashMap();

	/**Adjust trinket slot IDs*/
	@Override
	public void adjustTrinketSlots(ScreenHandler handler, int difference, InfinitorySlot slot) {
		try {
			// adjust trinketSlotStart/End (to fix transferring items to/from trinket slots)
			if ((Object) handler instanceof PlayerScreenHandler) {
				Field start = handler.getClass().getDeclaredField("trinketSlotStart");
				start.setAccessible(true);
				Field end = handler.getClass().getDeclaredField("trinketSlotEnd");
				end.setAccessible(true);
				start.set(handler, start.getInt(handler)+difference);
				end.set(handler, end.getInt(handler)+difference);
			}

			// adjust slot group ids (to fix slots that trinket groups are attached to not expanding to show trinket slots) Does this need fixing? race condition?
			if (handler instanceof TrinketPlayerScreenHandler) {
				Field slotsField = EntitySlotLoader.class.getDeclaredField("slots");
				slotsField.setAccessible(true);
				Map<EntityType<?>, Map<String, SlotGroup>> slots = (Map<EntityType<?>, Map<String, SlotGroup>>) slotsField.get(EntitySlotLoader.INSTANCE);
				Map<String, SlotGroup> newGroups = Maps.newHashMap();
				for (Entry<String, SlotGroup> entry : slots.get(EntityType.PLAYER).entrySet()) {
					NbtCompound nbt = new NbtCompound();
					entry.getValue().write(nbt);
					NbtCompound innerNbt = nbt.getCompound("GroupData");
					int slotId = innerNbt.getInt("SlotId");
					if (slotId != -1) {
						int newSlotId; // newSlotId = original slot id + additional slots
						// get original slot id
						if (trinketSlotGroupIds.containsKey(entry.getKey()))
							newSlotId = trinketSlotGroupIds.get(entry.getKey())+Utils.getAdditionalSlots(slot.player);
						// handler is original slot id
						else {
							trinketSlotGroupIds.put(entry.getKey(), slotId);
							newSlotId = slotId + +Utils.getAdditionalSlots(slot.player);
						}
						innerNbt.putInt("SlotId", newSlotId);
					}
					newGroups.put(entry.getKey(), SlotGroup.read(nbt));
				}
				slots.put(EntityType.PLAYER, newGroups);
				((TrinketPlayerScreenHandler) handler).updateTrinketSlots(true);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
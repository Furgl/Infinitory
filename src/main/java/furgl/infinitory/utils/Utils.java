package furgl.infinitory.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Maps;

import furgl.infinitory.Infinitory;
import furgl.infinitory.impl.inventory.IPlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

public class Utils {

	public static final Identifier VANILLA_INVENTORY = new Identifier("textures/gui/container/inventory.png");
	public static final Identifier VANILLA_SEARCH_INVENTORY = new Identifier("textures/gui/container/creative_inventory/tab_item_search.png"); 
	public static final Identifier VANILLA_SCROLLBAR = new Identifier("textures/gui/container/creative_inventory/tabs.png");
	public static final Identifier TEXTURES = new Identifier(Infinitory.MODID, "textures/gui/container/inventory/inventory.png");
	/**Coords of 3x3 top-left crafting input slot*/
	public static final Pair<Integer, Integer> CRAFTING_SLOTS_INPUT = new Pair(98, 6);
	/**Coords of 3x3 crafting output slot*/
	public static final Pair<Integer, Integer> CRAFTING_SLOTS_OUTPUT = new Pair(172, 24);
	private static HashMap<UUID, Float> scrollPositions = Maps.newHashMap();

	/** Gets String from double without trailing zeroes */ 
	public static String formatDouble(double num, int decimalPlaces) {
		BigDecimal decimal = new BigDecimal(num);
		decimal = decimal.setScale(decimalPlaces, RoundingMode.HALF_EVEN);
		if (decimal.doubleValue() == decimal.intValue())
			return String.valueOf(decimal.intValue());
		else
			return String.valueOf(decimal.doubleValue());
	}

	/**Get additional slots for this player - always multiple of 9*/
	public static int getAdditionalSlots(PlayerEntity player) {
		if (player != null && player.getInventory() instanceof IPlayerInventory)
			return ((IPlayerInventory)player.getInventory()).getAdditionalSlots();
		else
			return 0;
	}

	/**Get inventory scroll position for player*/
	public static float getScrollPosition(PlayerEntity player) {
		if (player != null && scrollPositions.containsKey(player.getUuid()))
			return scrollPositions.get(player.getUuid());
		else
			return 0;
	}
	
	/**Set inventory scroll position for player*/
	public static void setScrollPosition(PlayerEntity player, float scrollPosition) {
		if (player != null)
			scrollPositions.put(player.getUuid(), scrollPosition);
	}
	
	/**
	 * Case insensitive mapping from string -> enum tests camel case, caps, and
	 * lowercase (NEEDS TO MATCH THE ENUM)
	 */
	public static <T extends Enum<T>> Optional<T> getEnumFromString(Class<T> clazz, String string) {
		T ret = null;
		if (clazz != null && string != null && !string.isEmpty()) {
			try {
				ret = Enum.valueOf(clazz, string.trim());
			} catch (IllegalArgumentException ex) {}
			try {
				ret = Enum.valueOf(clazz, string.trim().toUpperCase());
			} catch (IllegalArgumentException ex) {}
			try {
				ret = Enum.valueOf(clazz,
						string.trim().substring(0, 1).toUpperCase() + string.trim().substring(1).toLowerCase());
			} catch (IllegalArgumentException ex) {}
			try {
				ret = Enum.valueOf(clazz, string.trim().toLowerCase());
			} catch (IllegalArgumentException ex) {}
		}
		return Optional.ofNullable(ret);
	}

}
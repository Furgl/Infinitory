package furgl.infinitory.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;

public class Config {

	private static final Gson GSON = new GsonBuilder()
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.serializeNulls()
			.create();
	private static final String FILE = "./config/infinitory.cfg";
	private static File file;

	public static int maxStackSize;
	public static int maxExtraSlots;
	public static DropsOnDeath dropsOnDeath = DropsOnDeath.UP_TO_STACK;
	public static boolean expandedCrafting;
	
	public enum DropsOnDeath {
		EVERYTHING, UP_TO_STACK, UP_TO_STACK_LIMITED;
		
		/**Get DropsOnDeath for this ordinal / index*/
		public static DropsOnDeath get(int ordinal) {
			if (ordinal >= 0 && ordinal < DropsOnDeath.values().length)
				return DropsOnDeath.values()[ordinal];
			else
				return DropsOnDeath.UP_TO_STACK;
		}
	}

	public static void init() {
		try {
			// create file if it doesn't already exist
			file = new File(FILE);
			if (!file.exists()) {
				file.createNewFile();
				writeToFile(true);
			}
			readFromFile();
			// write current values / defaults to file
			writeToFile(false);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void readFromFile() {
		try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			JsonObject parser = (JsonObject) JsonHelper.deserialize(reader);

			JsonElement element = parser.get("Max Item Stack Size");
			if (element == null)
				maxStackSize = Integer.MAX_VALUE;
			else
				maxStackSize = MathHelper.clamp(element.getAsInt(), 64, Integer.MAX_VALUE);

			element = parser.get("Max Extra Inventory Slots");
			if (element == null)
				maxExtraSlots = Integer.MAX_VALUE;
			else
				maxExtraSlots = MathHelper.clamp(element.getAsInt(), 0, Integer.MAX_VALUE);

			element = parser.get("Items to Drop on Death (when keepInventory = false) (0 = Everything, 1 = Up to a stack of each item, 2 = Up to a stack of each item in your hotbar, offhand, and armor slots)");
			if (element == null)
				dropsOnDeath = DropsOnDeath.UP_TO_STACK;
			else
				dropsOnDeath = DropsOnDeath.get(MathHelper.clamp(element.getAsInt(), 0, 2));
			
			element = parser.get("3x3 Crafting Area");
			if (element == null)
				expandedCrafting = true;
			else
				expandedCrafting = element.getAsBoolean();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void writeToFile(boolean writeDefaults) {
		try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
			JsonObject obj = new JsonObject();

			obj.addProperty("Max Item Stack Size", writeDefaults ? Integer.MAX_VALUE : maxStackSize);
			obj.addProperty("Max Extra Inventory Slots", writeDefaults ? Integer.MAX_VALUE : maxExtraSlots);
			obj.addProperty("Items to Drop on Death (when keepInventory = false) (0 = Everything, 1 = Up to a stack of each item, 2 = Up to a stack of each item in your hotbar, offhand, and armor slots)", writeDefaults ? 1 : dropsOnDeath.ordinal());
			obj.addProperty("3x3 Crafting Area", writeDefaults ? true : expandedCrafting);
			
			writer.write(GSON.toJson(obj));
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
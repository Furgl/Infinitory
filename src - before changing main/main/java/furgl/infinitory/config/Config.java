package furgl.infinitory.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;

public class Config {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.serializeNulls()
			.create();
	private static final String FILE = "./config/infinitory.cfg";
	private static File file;

	public static int maxStackSize;
	public static int maxExtraSlots;

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
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			JsonObject parser = (JsonObject) JsonHelper.deserialize(reader);

			JsonElement element = parser.get("Max Item Stack Size");
			maxStackSize = MathHelper.clamp(element.getAsInt(), 64, Integer.MAX_VALUE);
			
			element = parser.get("Max Extra Inventory Slots");
			maxExtraSlots = MathHelper.clamp(element.getAsInt(), 0, Integer.MAX_VALUE);
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void writeToFile(boolean writeDefaults) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			JsonObject obj = new JsonObject();

			obj.addProperty("Max Item Stack Size", writeDefaults ? Integer.MAX_VALUE : maxStackSize);
			obj.addProperty("Max Extra Inventory Slots", writeDefaults ? Integer.MAX_VALUE : maxExtraSlots);

			writer.write(GSON.toJson(obj));
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
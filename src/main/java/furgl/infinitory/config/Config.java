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

public class Config {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.serializeNulls()
			.create();
	private static final String FILE = "./config/improvedHoes.cfg";
	private static File file;

	public static boolean leftClickWithHoeToBreak;
	public static boolean rightClickToHarvest;
	public static boolean rightClickWithHoeToHarvest;
	public static boolean rightClickWithHoeToTill;
	public static boolean preventTrampling;
	public static boolean replantOnHarvest;
	public static boolean workWhileSneaking;

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

			JsonElement element = parser.get("Left-Click Crops with Improved Hoe to Break in Range");
			leftClickWithHoeToBreak = element.getAsBoolean();
			
			element = parser.get("Right-Click Crops without Improved Hoe to Harvest");
			rightClickToHarvest = element.getAsBoolean();
			
			element = parser.get("Right-Click Crops with Improved Hoe to Harvest in Range");
			rightClickWithHoeToHarvest = element.getAsBoolean();
			
			element = parser.get("Right-Click Ground with Improved Hoe to Till in Range");
			rightClickWithHoeToTill = element.getAsBoolean();
		
			element = parser.get("Prevent Trampling Crops");
			preventTrampling = element.getAsBoolean();
			
			element = parser.get("Replant on Harvest");
			replantOnHarvest = element.getAsBoolean();
			
			element = parser.get("Work while Sneaking");
			workWhileSneaking = element.getAsBoolean();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void writeToFile(boolean writeDefaults) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			JsonObject obj = new JsonObject();

			obj.addProperty("Left-Click Crops with Improved Hoe to Break in Range", writeDefaults ? true : leftClickWithHoeToBreak);
			obj.addProperty("Right-Click Crops without Improved Hoe to Harvest", writeDefaults ? true : rightClickToHarvest);
			obj.addProperty("Right-Click Crops with Improved Hoe to Harvest in Range", writeDefaults ? true : rightClickWithHoeToHarvest);
			obj.addProperty("Right-Click Ground with Improved Hoe to Till in Range", writeDefaults ? true : rightClickWithHoeToTill);
			obj.addProperty("Prevent Trampling Crops", writeDefaults ? true : preventTrampling);
			obj.addProperty("Replant on Harvest", writeDefaults ? true : replantOnHarvest);
			obj.addProperty("Work while Sneaking", writeDefaults ? false : workWhileSneaking);

			writer.write(GSON.toJson(obj));
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
package furgl.infinitory;

import furgl.infinitory.config.Config;
import net.fabricmc.api.ModInitializer;

public class Infinitory implements ModInitializer {
	
	// TEST on dedicated server
	// TODO what happens when player dies?

	public static final String MODNAME = "Infinitory";
	public static final String MODID = "infinitory";

	@Override
	public void onInitialize() {
		Config.init();
	}
	
}
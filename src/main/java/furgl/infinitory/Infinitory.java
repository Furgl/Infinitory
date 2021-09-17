package furgl.infinitory;

import furgl.infinitory.config.Config;
import furgl.infinitory.impl.network.PacketManager;
import net.fabricmc.api.ModInitializer;

public class Infinitory implements ModInitializer {
	
	// TEST on dedicated server

	public static final String MODNAME = "Infinitory";
	public static final String MODID = "infinitory";

	@Override
	public void onInitialize() {
		Config.init();
		PacketManager.init();
	}
	
}
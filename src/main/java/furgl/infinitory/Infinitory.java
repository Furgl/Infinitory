package furgl.infinitory;

import furgl.infinitory.config.Config;
import furgl.infinitory.impl.network.PacketManager;
import furgl.infinitory.proxies.ClientProxy;
import furgl.infinitory.proxies.CommonProxy;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;

public class Infinitory implements ModInitializer, ClientModInitializer {
	
	// TODO try to use vanilla texture for scrollbar?
	// TODO add textures to sorting buttons
	
	public static final String MODNAME = "Infinitory";
	public static final String MODID = "infinitory";
	
	public static CommonProxy proxy;

	@Override
	public void onInitialize() {
		proxy = new CommonProxy();
		PacketManager.initServerPackets();
		Config.init();
	}
	
	@Override
	@Environment(EnvType.CLIENT)
	public void onInitializeClient() {
		proxy = new ClientProxy();
		PacketManager.initClientPackets();
	}

	
}
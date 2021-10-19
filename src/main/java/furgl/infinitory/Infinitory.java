package furgl.infinitory;

import org.jetbrains.annotations.Nullable;

import furgl.infinitory.config.Config;
import furgl.infinitory.impl.dependencies.Dependency;
import furgl.infinitory.impl.network.PacketManager;
import furgl.infinitory.proxies.ClientProxy;
import furgl.infinitory.proxies.CommonProxy;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;

public class Infinitory implements ModInitializer, ClientModInitializer {
	 	
	/**
	 * Fixed an issue causing Infinitory to fail to load sometimes
	 * Fixed an issue causing some Trinket slots to be offset when the inventory expands
	 * 
	 * TEST charm mod?
	 * FIXME creative inventory fucked with Charms?
	 * 
	 * TODO 3x3 crafting grid
	 * TODO expand inventory
	 * TODO search bar
	 */
	
	public static final String MODNAME = "Infinitory";
	public static final String MODID = "infinitory";
	
	public static CommonProxy proxy;
	@Nullable
	public static Dependency trinketsDependency;

	@Override
	public void onInitialize() {
		proxy = new CommonProxy();
		PacketManager.initServerPackets();
		Config.init();
		Dependency.init();
	}
	
	@Override
	@Environment(EnvType.CLIENT)
	public void onInitializeClient() {
		proxy = new ClientProxy();
		PacketManager.initClientPackets();
	}

}
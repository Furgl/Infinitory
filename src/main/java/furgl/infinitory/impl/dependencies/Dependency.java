package furgl.infinitory.impl.dependencies;

import furgl.infinitory.Infinitory;
import furgl.infinitory.impl.inventory.InfinitorySlot;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.screen.ScreenHandler;

public interface Dependency {
	
	// trinkets
	/**Adjust trinket slot IDs*/
	default void adjustTrinketSlots(ScreenHandler handler, int difference, InfinitorySlot slot) {} 

	/**Create dependencies*/
	public static void init() {
		// trinkets
		try {
			if (FabricLoader.getInstance().isModLoaded("trinkets"))
				Infinitory.trinketsDependency = Class.forName("furgl.infinitory.impl.dependencies.TrinketsDependency").asSubclass(Dependency.class).getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
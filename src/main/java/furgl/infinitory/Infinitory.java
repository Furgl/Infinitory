package furgl.infinitory;

import furgl.infinitory.config.Config;
import net.fabricmc.api.ModInitializer;

public class Infinitory implements ModInitializer {
	
	// FIXME large stacks don't seem to persist on save 
	//    (prob same issue when going from creative -> survival stacks disappearing)
	// FIXME dragging stacks doesn't show correct number

	public static final String MODNAME = "Infinitory";
	public static final String MODID = "infinitory";

	@Override
	public void onInitialize() {
		Config.init();
	}
	
}
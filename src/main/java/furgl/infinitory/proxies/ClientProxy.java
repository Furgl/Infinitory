package furgl.infinitory.proxies;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;

@Environment(EnvType.CLIENT)
public class ClientProxy extends CommonProxy {

	/**Get the inner slot in a CreativeSlot, or null if not on client / not CreativeSlot*/
	@Nullable
	@Override
	public Slot getCreativeSlotInnerSlot(Slot slot) {
		return slot instanceof CreativeSlot ? ((CreativeSlot)slot).slot : null;
	}
	
	/**Get the client player, or null if on dedicated server*/
	@Nullable
	@Override
	public PlayerEntity getClientPlayer() {
		return MinecraftClient.getInstance().player;
	}
	
}
package furgl.infinitory.proxies;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;

public class CommonProxy {

	/**Get the inner slot in a CreativeSlot, or null if not on client / not CreativeSlot*/
	@Nullable
	public Slot getCreativeSlotInnerSlot(Slot slot) {
		return null;
	}

	/**Get the client player, or null if on dedicated server*/
	@Nullable
	public PlayerEntity getClientPlayer() {
		return null;
	}

}
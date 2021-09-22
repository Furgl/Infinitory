package furgl.infinitory.impl.network;

import furgl.infinitory.Infinitory;
import furgl.infinitory.impl.inventory.IPlayerInventory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class PacketManager {

	public static final Identifier SORT_PACKET_ID = new Identifier(Infinitory.MODID, "sort");
	
	public static void init() {
		ServerPlayNetworking.registerGlobalReceiver(SORT_PACKET_ID, (server, player, handler, buf, responseSender) -> {
		    server.execute(() -> {
		        ((IPlayerInventory)player.getInventory()).needToSort();
		    });
		});
	}
	
}
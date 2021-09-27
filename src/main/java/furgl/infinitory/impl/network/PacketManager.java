package furgl.infinitory.impl.network;

import furgl.infinitory.Infinitory;
import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.impl.inventory.IScreenHandler;
import furgl.infinitory.impl.inventory.SortingType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class PacketManager {

	// server -> client
	public static final Identifier UPDATE_INFINITORY_PACKET_ID = new Identifier(Infinitory.MODID, "update_infinitory_values");
	
	/**Server -> Client packets*/
	@Environment(EnvType.CLIENT)
	public static void initClientPackets() {
		// update infinitory values
		ClientPlayNetworking.registerGlobalReceiver(UPDATE_INFINITORY_PACKET_ID, (client, handler, buf, responseSender) -> {
			int additionalSlots = buf.readInt();
			SortingType sortingType = SortingType.values()[buf.readInt()];
			boolean sortAscending = buf.readBoolean();

			client.execute(() -> {
				IPlayerInventory inv = ((IPlayerInventory)client.player.getInventory());
				inv.setSortingType(sortingType);
				inv.setSortAscending(sortAscending);
				inv.needToSort();
				inv.setAdditionalSlots(additionalSlots);
				inv.needToUpdateInfinitorySize();
				((IScreenHandler)client.player.playerScreenHandler).updateExtraSlots();
				if (client.player.currentScreenHandler != null)
					((IScreenHandler)client.player.currentScreenHandler).updateExtraSlots();
			});
		});
	}
	
	// client -> server
	public static final Identifier SORTING_TYPE_PACKET_ID = new Identifier(Infinitory.MODID, "sorting_type");
	public static final Identifier SORTING_ASCENDING_PACKET_ID = new Identifier(Infinitory.MODID, "sorting_ascending");

	/**Client -> Server packets*/
	public static void initServerPackets() {
		// client clicked sorting type button
		ServerPlayNetworking.registerGlobalReceiver(SORTING_TYPE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				IPlayerInventory inv = ((IPlayerInventory)player.getInventory());
				inv.setSortingType(inv.getSortingType().getNextType());
				inv.needToSort();
			});
		});
		// client clicked sorting ascending button
		ServerPlayNetworking.registerGlobalReceiver(SORTING_ASCENDING_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				IPlayerInventory inv = ((IPlayerInventory)player.getInventory());
				inv.setSortAscending(!inv.getSortingAscending());
				inv.needToSort();
			});
		});		
	}

}
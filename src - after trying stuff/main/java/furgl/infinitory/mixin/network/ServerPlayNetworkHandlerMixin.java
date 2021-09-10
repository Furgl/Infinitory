package furgl.infinitory.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import furgl.infinitory.config.Config;
import net.minecraft.server.network.ServerPlayNetworkHandler;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
	
	@ModifyConstant(method = "onCreativeInventoryAction", constant = @Constant(intValue = 45))
	public int increaseMaxSlots(int maxSlot) {
		return maxSlot + Integer.MAX_VALUE;
	}
	
	@ModifyConstant(method = "onCreativeInventoryAction", constant = @Constant(intValue = 64))
	public int increaseMaxStackSize(int maxStackSize) {
		return Config.maxStackSize;
	}

}
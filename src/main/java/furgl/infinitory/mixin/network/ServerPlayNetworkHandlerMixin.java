package furgl.infinitory.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import furgl.infinitory.config.Config;
import net.minecraft.server.network.ServerPlayNetworkHandler;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 999)
public abstract class ServerPlayNetworkHandlerMixin {
	
	@ModifyConstant(method = "onCreativeInventoryAction", constant = @Constant(intValue = 45))
	public int increaseMaxSlots(int maxSlot) {
		return Integer.MAX_VALUE;
	}
	
	@ModifyConstant(method = "onCreativeInventoryAction", constant = @Constant(intValue = 64), require = 0)
	public int increaseMaxStackSize(int maxStackSize) {
		return Config.maxStackSize;
	}

}
package furgl.infinitory.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import furgl.infinitory.config.Config;
import furgl.infinitory.config.Config.DropsOnDeath;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

	/**On death, copy old inventory depending on config option*/
	@Inject(method="copyFrom", at=@At("RETURN"), cancellable = true)
	public void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		if (Config.dropsOnDeath != DropsOnDeath.EVERYTHING && 
				!((ServerPlayerEntity)(Object)this).world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY) && 
				!oldPlayer.isSpectator()) 
			((ServerPlayerEntity)(Object)this).getInventory().clone(oldPlayer.getInventory());
	}
	
}
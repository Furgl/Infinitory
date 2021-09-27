package furgl.infinitory.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import furgl.infinitory.utils.Utils;
import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	/**Fix pick item syncing wrong slot id*/
	@ModifyConstant(method = "doItemPick()V", constant = @Constant(intValue = 36))
	public int doItemPickFixSlot(int value) {
		return value + Utils.getAdditionalSlots(((MinecraftClient)(Object)this).player);
	}	
	
}
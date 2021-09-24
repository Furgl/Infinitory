package furgl.infinitory.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import furgl.infinitory.impl.inventory.IPlayerInventory;
import furgl.infinitory.impl.inventory.IScreenHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

@Mixin(targets = "net/minecraft/server/network/ServerPlayerEntity$1")
public class ScreenHandlerSyncHandlerMixin {

	@Shadow
	private ServerPlayerEntity field_29182;
	
	@Inject(method="updateState", at=@At("INVOKE"))
	public void updateStateUpdateInfinitoryValues(ScreenHandler handler, DefaultedList<ItemStack> stacks, ItemStack cursorStack, int[] properties, CallbackInfo ci) {
		// sync infinitory values first before syncing inventory
		if (this.field_29182 != null) {
			//System.out.println("updateState sending sync values"); // TODO remove
			((IPlayerInventory)this.field_29182.getInventory()).syncInfinitoryValues();
		}
	}
	
}
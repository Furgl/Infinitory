package furgl.infinitory.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

@Mixin(PacketByteBuf.class)
public abstract class PacketByteBufMixin {
	// FIXME this doesn't fix the issue with cloning > 64 in creative not being carried over to survival.. not sure if this is needed
	@ModifyArg(method = "readItemStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;<init>(Lnet/minecraft/item/ItemConvertible;I)V"), index = 1)
	public int readItemStack(int j) {
		int ret = ((PacketByteBuf)(Object)this).readVarInt();
		System.out.println("reading: "+ret); // TODO remove
		return ret;
	}

	@Inject(method = "writeItemStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;writeByte(I)Lio/netty/buffer/ByteBuf;"))
	public void writeItemStack(ItemStack stack, CallbackInfoReturnable ci) {
		System.out.println("writing: "+stack.getCount()); // TODO remove
		((PacketByteBuf)(Object)this).writeVarInt(stack.getCount());
	}

}
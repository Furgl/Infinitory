package furgl.infinitory.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

@Mixin(PacketByteBuf.class)
public abstract class PacketByteBufMixin {
	
	/**Fixes {@link PacketByteBuf#readItemStack()} for larger stacks (in MP)*/
	@ModifyArg(method = "readItemStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;<init>(Lnet/minecraft/item/ItemConvertible;I)V"), index = 1)
	public int readItemStack(int j) {
		return ((PacketByteBuf)(Object)this).readVarInt();
	}

	/**Fixes {@link PacketByteBuf#writeItemStack()} for larger stacks (in MP)*/
	@Inject(method = "writeItemStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;writeByte(I)Lio/netty/buffer/ByteBuf;", shift = Shift.AFTER))
	public void writeItemStack(ItemStack stack, CallbackInfoReturnable ci) {
		((PacketByteBuf)(Object)this).writeVarInt(stack.getCount());
	}

}
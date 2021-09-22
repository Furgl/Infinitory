package furgl.infinitory.mixin.items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	
	@Unique
	private static final String KEY = "Infinitory Count";

	@Shadow
	private int count;

	@Inject(method = "<init>(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("RETURN"))
	public void constructor(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.contains(KEY)) {
			int count = nbt.getInt(KEY);
			if (count > this.count)
				((ItemStack)(Object)this).setCount(count);
		}

	}

	@Inject(method = "writeNbt", at = @At("RETURN"))
	public void writeNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> ci) {
		nbt.putInt(KEY, this.count);
	}

}
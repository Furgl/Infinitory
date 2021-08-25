package furgl.infinitory.interfaces;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.screen.slot.Slot;

@Mixin(Slot.class)
public interface ISlotMixin {

	@Invoker
	void onTake(int amount);
	
}
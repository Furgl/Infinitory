package furgl.infinitory.mixin.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.screen.slot.Slot;

@Mixin(Slot.class)
public interface SlotAccessor {

	@Invoker
	void callOnTake(int amount);
	
}
package furgl.infinitory.mixin.inventory;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import furgl.infinitory.impl.inventory.ISlot;
import net.minecraft.screen.slot.Slot;

@Mixin(Slot.class)
public abstract class SlotMixin implements ISlot {

	@Shadow @Final @Mutable
	private int index;
	@Shadow @Final @Mutable
	private int x;
	@Shadow @Final @Mutable
	private int y;
	
	@Unique
	@Override
	public void setIndex(int index) {
		this.index = index;
	}
	
	@Unique
	@Override
	public void setX(int x) {
		this.x = x;
	}
	
	@Unique
	@Override
	public void setY(int y) {
		this.y = y;
	}

}
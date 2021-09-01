package furgl.infinitory.impl.inventory;

import org.spongepowered.asm.mixin.Unique;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface IPlayerInventory {

	/**Infinitory's extra inventory slots*/
	@Unique
	public DefaultedList<ItemStack> getInfinitory();
	
	/**Get items in combined main + Infinitory*/
	@Unique
	public void getCombinedInfinitory();
	
	/**Set item in combined main + Infinitory*/
	@Unique
	public void setSetInfinitory(int index, ItemStack stack);
	
}
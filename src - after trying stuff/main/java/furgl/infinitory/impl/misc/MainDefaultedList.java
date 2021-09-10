package furgl.infinitory.impl.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.Lists;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class MainDefaultedList extends DefaultedList<ItemStack> {

	protected MainDefaultedList(List<ItemStack> delegate, ItemStack initialElement) {
		super(delegate, initialElement);
	}

	/**Changed to not be fixed size*/
	public static MainDefaultedList ofSize(int size, ItemStack defaultValue) {
		Validate.notNull(defaultValue);
		ArrayList<ItemStack> list = Lists.newArrayList();
		for (int i=0; i<size; ++i)
			list.add(defaultValue);
		return new MainDefaultedList(list, defaultValue);
	}

	/**Change clear() to set everything to air instead of clearing size (bc normally main would be fixed size and clear would just set to air)*/
	@Override
	public void clear() {
		for(int i = 0; i < this.size(); ++i) 
			this.set(i, ItemStack.EMPTY);
	}

}
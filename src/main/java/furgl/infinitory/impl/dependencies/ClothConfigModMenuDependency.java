package furgl.infinitory.impl.dependencies;

import java.util.Optional;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import furgl.infinitory.config.Config;
import furgl.infinitory.config.Config.DropsOnDeath;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class ClothConfigModMenuDependency implements Dependency, ModMenuApi {
	
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ConfigBuilder builder = ConfigBuilder.create()
					.setParentScreen(parent)
					.setTitle(new TranslatableText("config.infinitory.name"))
					.setSavingRunnable(() -> Config.writeToFile(false));
			ConfigCategory category = builder.getOrCreateCategory(new TranslatableText("config.infinitory.category.general"));
			category.addEntry(builder.entryBuilder()
					.startIntField(new TranslatableText("config.infinitory.option.maxItemStackSize"), Config.maxStackSize)
					.setTooltip(new TranslatableText("config.infinitory.option.maxItemStackSize.tooltip"))
					.setDefaultValue(Integer.MAX_VALUE)
					.setSaveConsumer(value -> Config.maxStackSize = value)
					.build());
			category.addEntry(builder.entryBuilder()
					.startIntField(new TranslatableText("config.infinitory.option.maxExtraInventorySlots"), Config.maxExtraSlots)
					.setTooltip(new TranslatableText("config.infinitory.option.maxExtraInventorySlots.tooltip"))
					.setDefaultValue(Integer.MAX_VALUE)
					.setSaveConsumer(value -> Config.maxExtraSlots = value)
					.build());
			category.addEntry(builder.entryBuilder()
					.startEnumSelector(new TranslatableText("config.infinitory.option.itemsToDropOnDeath"), DropsOnDeath.class, DropsOnDeath.UP_TO_STACK)
					.setEnumNameProvider(e -> new TranslatableText("config.infinitory.option", e.ordinal()+1))
					.setTooltip(new TranslatableText("config.infinitory.option.itemsToDropOnDeath.tooltip"))
					.setTooltipSupplier(drop -> Optional.of(new Text[] {new TranslatableText("config.infinitory.option.itemsToDropOnDeath."+drop.ordinal())}))
					.setDefaultValue(DropsOnDeath.UP_TO_STACK)
					.setSaveConsumer(value -> Config.dropsOnDeath = value)
					.build());
			category.addEntry(builder.entryBuilder()
					.startBooleanToggle(new TranslatableText("config.infinitory.option.expandedCrafting"), Config.expandedCrafting)
					.setTooltip(new TranslatableText("config.infinitory.option.expandedCrafting.tooltip"))
					.setDefaultValue(true)
					.setSaveConsumer(value -> Config.expandedCrafting = value)
					.requireRestart()
					.build());
			return builder.build();
		};
	}
	
}
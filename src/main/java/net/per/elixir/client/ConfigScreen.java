package net.per.elixir.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.per.elixir.ElixirConfig;

import static net.per.elixir.ElixirConfig.*;

public class ConfigScreen {
    public static Screen create(ModContainer container, Screen parent) {
        var builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("config.elixir.title"));
        var general = builder.getOrCreateCategory(Component.translatable("config.elixir.category.general"));
        var entryBuilder = builder.entryBuilder();
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.pharma_limited"), pharmaLimited)
                .setDefaultValue(1000)
                .setTooltip(Component.translatable("config.elixir.option.pharma_limited.tooltip"))
                .setSaveConsumer(v -> pharmaLimited = v)
                .build());
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.pharma_conversion_rate"), pharmaConversionRate)
                .setDefaultValue(10)
                .setTooltip(Component.translatable("config.elixir.option.pharma_conversion_rate.tooltip"))
                .setSaveConsumer(v -> pharmaConversionRate = v)
                .build());
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.extreme_temperature_range"), extremeTemperatureRange)
                .setDefaultValue(100)
                .setTooltip(Component.translatable("config.elixir.option.extreme_temperature_range.tooltip"))
                .setSaveConsumer(v -> extremeTemperatureRange = v)
                .build());
        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.elixir.option.bad_elixir_compensation"), badElixirCompensation)
                .setDefaultValue(0.3)
                .setTooltip(Component.translatable("config.elixir.option.bad_elixir_compensation.tooltip"))
                .setSaveConsumer(v -> badElixirCompensation = v)
                .build());
        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.elixir.option.stability_loss_rate"), stabilityLossRate)
                .setDefaultValue(10)
                .setTooltip(Component.translatable("config.elixir.option.stability_loss_rate.tooltip"))
                .setSaveConsumer(v -> stabilityLossRate = v)
                .build());
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.refine_ticks"), refineTicks)
                .setDefaultValue(20)
                .setSaveConsumer(v -> refineTicks = v)
                .build());
        var pharma = builder.getOrCreateCategory(Component.translatable("config.elixir.category.pharma"));
        pharma.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.highest_pharma_limited"), highestPharmaLimited)
                .setDefaultValue(1000)
                .setTooltip(Component.translatable("config.elixir.option.highest_pharma_limited.tooltip"))
                .setSaveConsumer(v -> highestPharmaLimited = v)
                .build());
        pharma.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.time_conversion_rate"), timeConversionRate)
                .setDefaultValue(100)
                .setSaveConsumer(v -> timeConversionRate = v)
                .build());
        pharma.addEntry(entryBuilder.startDoubleField(Component.translatable("config.elixir.option.attribute_modifier_dilute"), attributeModifierDilute)
                .setDefaultValue(100.0f)
                .setSaveConsumer(v -> attributeModifierDilute = v)
                .build());
        pharma.addEntry(entryBuilder.startDoubleField(Component.translatable("config.elixir.option.effect_dilute"), effectDilute)
                .setDefaultValue(10.0f)
                .setSaveConsumer(v -> effectDilute = v)
                .build());
        builder.setSavingRunnable(ElixirConfig::save);
        return builder.build();
    }
}

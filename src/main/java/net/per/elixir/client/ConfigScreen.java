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
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.temp_target_margin"), tempTargetMargin)
                .setDefaultValue(5)
                .setMin(0)
                .setMax(250)
                .setTooltip(Component.translatable("config.elixir.option.temp_target_margin.tooltip"))
                .setSaveConsumer(v -> tempTargetMargin = v)
                .build());
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.temp_safe_margin"), tempSafeMargin)
                .setDefaultValue(100)
                .setMin(1)
                .setTooltip(Component.translatable("config.elixir.option.temp_safe_margin.tooltip"))
                .setSaveConsumer(v -> tempSafeMargin = v)
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
                .setDefaultValue(5)
                .setSaveConsumer(v -> refineTicks = v)
                .build());
        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.elixir.option.hud_scale"), hudScale)
                .setDefaultValue(1.5)
                .setMin(1.0)
                .setMax(4.0)
                .setTooltip(Component.translatable("config.elixir.option.hud_scale.tooltip"))
                .setSaveConsumer(v -> hudScale = v)
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
        var exp = builder.getOrCreateCategory(Component.translatable("config.elixir.category.exp"));
        exp.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.exp_success_gain"), expSuccessGain)
                .setDefaultValue(3)
                .setMin(0)
                .setTooltip(Component.translatable("config.elixir.option.exp_success_gain.tooltip"))
                .setSaveConsumer(v -> expSuccessGain = v)
                .build());
        exp.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.exp_failure_gain"), expFailureGain)
                .setDefaultValue(1)
                .setMin(0)
                .setTooltip(Component.translatable("config.elixir.option.exp_failure_gain.tooltip"))
                .setSaveConsumer(v -> expFailureGain = v)
                .build());
        exp.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.exp_growth_rate"), expGrowthRate)
                .setDefaultValue(128)
                .setMin(1)
                .setTooltip(Component.translatable("config.elixir.option.exp_growth_rate.tooltip"))
                .setSaveConsumer(v -> expGrowthRate = v)
                .build());
        exp.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.temp_range_base"), tempRangeBase)
                .setDefaultValue(10)
                .setMin(0)
                .setTooltip(Component.translatable("config.elixir.option.temp_range_base.tooltip"))
                .setSaveConsumer(v -> tempRangeBase = v)
                .build());
        exp.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.temp_range_max"), tempRangeMax)
                .setDefaultValue(200)
                .setMin(1)
                .setTooltip(Component.translatable("config.elixir.option.temp_range_max.tooltip"))
                .setSaveConsumer(v -> tempRangeMax = v)
                .build());
        exp.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.explode_delay_base"), explodeDelayBase)
                .setDefaultValue(100)
                .setMin(1)
                .setTooltip(Component.translatable("config.elixir.option.explode_delay_base.tooltip"))
                .setSaveConsumer(v -> explodeDelayBase = v)
                .build());
        exp.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.explode_delay_gain"), explodeDelayGain)
                .setDefaultValue(100)
                .setMin(0)
                .setTooltip(Component.translatable("config.elixir.option.explode_delay_gain.tooltip"))
                .setSaveConsumer(v -> explodeDelayGain = v)
                .build());
        exp.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.failed_delay_base"), failedDelayBase)
                .setDefaultValue(140)
                .setMin(1)
                .setTooltip(Component.translatable("config.elixir.option.failed_delay_base.tooltip"))
                .setSaveConsumer(v -> failedDelayBase = v)
                .build());
        exp.addEntry(entryBuilder.startIntField(Component.translatable("config.elixir.option.failed_delay_gain"), failedDelayGain)
                .setDefaultValue(60)
                .setMin(0)
                .setTooltip(Component.translatable("config.elixir.option.failed_delay_gain.tooltip"))
                .setSaveConsumer(v -> failedDelayGain = v)
                .build());
        exp.addEntry(entryBuilder.startDoubleField(Component.translatable("config.elixir.option.maid_exp_success_gain"), maidExpSuccessGain)
                .setDefaultValue(0.1)
                .setMin(0)
                .setTooltip(Component.translatable("config.elixir.option.maid_exp_success_gain.tooltip"))
                .setSaveConsumer(v -> maidExpSuccessGain = v)
                .build());
        exp.addEntry(entryBuilder.startDoubleField(Component.translatable("config.elixir.option.maid_exp_failure_gain"), maidExpFailureGain)
                .setDefaultValue(0.1)
                .setMin(0)
                .setTooltip(Component.translatable("config.elixir.option.maid_exp_failure_gain.tooltip"))
                .setSaveConsumer(v -> maidExpFailureGain = v)
                .build());
        exp.addEntry(entryBuilder.startDoubleField(Component.translatable("config.elixir.option.maid_negligence_chance"), maidNegligenceChance)
                .setDefaultValue(0.25)
                .setMin(0)
                .setMax(1)
                .setTooltip(Component.translatable("config.elixir.option.maid_negligence_chance.tooltip"))
                .setSaveConsumer(v -> maidNegligenceChance = v)
                .build());
        builder.setSavingRunnable(ElixirConfig::save);
        return builder.build();
    }
}

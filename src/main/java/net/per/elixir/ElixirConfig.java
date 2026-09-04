package net.per.elixir;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import net.neoforged.fml.loading.FMLPaths;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class ElixirConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("elixir.toml");
    public static int pharmaLimited;
    public static int pharmaConversionRate;
    public static int highestPharmaLimited;
    public static int timeConversionRate;
    public static int danEatFastTicks;
    public static int danEatMidTicks;
    public static int danEatSlowTicks;
    public static double attributeModifierDilute;
    public static double effectDilute;
    public static final int EXTREME_TEMP_RANGE_FALLBACK = 10;
    public static int tempRangeMax;
    public static double badElixirCompensation;
    public static double stabilityLossRate;
    public static int refineTicks;
    public static int expSuccessGain;
    public static int expFailureGain;
    public static int expGrowthRate;
    public static int tempTargetMargin;
    public static int tempSafeMargin;
    public static int tempRangeBase;
    public static int explodeDelayBase;
    public static int explodeDelayGain;
    public static int failedDelayBase;
    public static int failedDelayGain;
    public static double hudScale;
    public static double maidExpSuccessGain;
    public static double maidExpFailureGain;
    public static double maidNegligenceChance;
    public static double multifurnaceCoolBase;
    public static double multifurnaceCoolFactor;
    public static int multifurnaceStabilityPenalty;
    public static int multifurnacePharmaGain;
    public static int maxFurnaceSize;
    public static int multifurnaceSlotsBase;
    public static int multifurnaceSlotsGain;
    public static int multifurnaceSlotsCap;
    public static int multifurnaceLightLevel;
    public static boolean serverSynced;
    private static int localMaxFurnaceSize;
    private static int localMultifurnaceSlotsBase;
    private static int localMultifurnaceSlotsGain;
    private static int localMultifurnaceSlotsCap;
    private static int localMultifurnaceLightLevel;

    public static void save() {
        if (!serverSynced) {
            localMaxFurnaceSize = maxFurnaceSize;
            localMultifurnaceSlotsBase = multifurnaceSlotsBase;
            localMultifurnaceSlotsGain = multifurnaceSlotsGain;
            localMultifurnaceSlotsCap = multifurnaceSlotsCap;
            localMultifurnaceLightLevel = multifurnaceLightLevel;
        }
        var map = new HashMap<String, Object>();
        map.put("pharma_limited", pharmaLimited);
        map.put("pharma_conversion_rate", pharmaConversionRate);
        map.put("highest_pharma_limited", highestPharmaLimited);
        map.put("time_conversion_rate", timeConversionRate);
        map.put("dan_eat_fast_ticks", danEatFastTicks);
        map.put("dan_eat_mid_ticks", danEatMidTicks);
        map.put("dan_eat_slow_ticks", danEatSlowTicks);
        map.put("attribute_modifier_dilute", attributeModifierDilute);
        map.put("effect_dilute", effectDilute);
        map.put("temp_range_max", tempRangeMax);
        map.put("bad_elixir_compensation", badElixirCompensation);
        map.put("stability_loss_rate", stabilityLossRate);
        map.put("refine_ticks", refineTicks);
        map.put("exp_success_gain", expSuccessGain);
        map.put("exp_failure_gain", expFailureGain);
        map.put("exp_growth_rate", expGrowthRate);
        map.put("temp_target_margin", tempTargetMargin);
        map.put("temp_safe_margin", tempSafeMargin);
        map.put("temp_range_base", tempRangeBase);
        map.put("explode_delay_base", explodeDelayBase);
        map.put("explode_delay_gain", explodeDelayGain);
        map.put("failed_delay_base", failedDelayBase);
        map.put("failed_delay_gain", failedDelayGain);
        map.put("hud_scale", hudScale);
        map.put("maid_exp_success_gain", maidExpSuccessGain);
        map.put("maid_exp_failure_gain", maidExpFailureGain);
        map.put("maid_negligence_chance", maidNegligenceChance);
        map.put("multifurnace_cool_base", multifurnaceCoolBase);
        map.put("multifurnace_cool_factor", multifurnaceCoolFactor);
        map.put("multifurnace_stability_penalty", multifurnaceStabilityPenalty);
        map.put("multifurnace_pharma_gain", multifurnacePharmaGain);
        map.put("max_furnace_size", localMaxFurnaceSize);
        map.put("multifurnace_slots_base", localMultifurnaceSlotsBase);
        map.put("multifurnace_slots_gain", localMultifurnaceSlotsGain);
        map.put("multifurnace_slots_cap", localMultifurnaceSlotsCap);
        map.put("multifurnace_light_level", localMultifurnaceLightLevel);
        new TomlWriter().write(Config.of(() -> map, InMemoryFormat.defaultInstance()).unmodifiable(), CONFIG_PATH, WritingMode.REPLACE);
    }

    public static void load() {
        var c = readConfig();
        pharmaLimited = c.getOrElse("pharma_limited", 1000);
        pharmaConversionRate = c.getOrElse("pharma_conversion_rate", 10);
        highestPharmaLimited = c.getOrElse("highest_pharma_limited", 1000);
        timeConversionRate = c.getOrElse("time_conversion_rate", 100);
        danEatFastTicks = Math.max(1, c.getOrElse("dan_eat_fast_ticks", 10));
        danEatSlowTicks = Math.max(danEatFastTicks, Math.max(1, c.getOrElse("dan_eat_slow_ticks", 64)));
        danEatMidTicks = Math.clamp(Math.max(1, c.getOrElse("dan_eat_mid_ticks", 32)), danEatFastTicks, danEatSlowTicks);
        attributeModifierDilute = c.getOrElse("attribute_modifier_dilute", 100.0);
        effectDilute = c.getOrElse("effect_dilute", 10.0);
        tempRangeMax = Math.max(1, c.getOrElse("temp_range_max", 200));
        badElixirCompensation = c.getOrElse("bad_elixir_compensation", 0.3);
        stabilityLossRate = c.getOrElse("stability_loss_rate", 10.0);
        refineTicks = c.getOrElse("refine_ticks", 5);
        expSuccessGain = c.getOrElse("exp_success_gain", 3);
        expFailureGain = c.getOrElse("exp_failure_gain", 1);
        expGrowthRate = Math.max(1, c.getOrElse("exp_growth_rate", 128));
        tempTargetMargin = Math.clamp(c.getOrElse("temp_target_margin", 5), 0, 250);
        tempSafeMargin = Math.max(1, c.getOrElse("temp_safe_margin", 100));
        tempRangeBase = Math.max(0, c.getOrElse("temp_range_base", 10));
        explodeDelayBase = Math.max(1, c.getOrElse("explode_delay_base", 100));
        explodeDelayGain = Math.max(0, c.getOrElse("explode_delay_gain", 100));
        failedDelayBase = Math.max(1, c.getOrElse("failed_delay_base", 140));
        failedDelayGain = Math.max(0, c.getOrElse("failed_delay_gain", 60));
        hudScale = Math.clamp(c.getOrElse("hud_scale", 1.5), 1.0, 4.0);
        maidExpSuccessGain = Math.max(0, c.getOrElse("maid_exp_success_gain", 0.1));
        maidExpFailureGain = Math.max(0, c.getOrElse("maid_exp_failure_gain", 0.1));
        maidNegligenceChance = Math.clamp(c.getOrElse("maid_negligence_chance", 0.25), 0.0, 1.0);
        multifurnaceCoolBase = Math.max(0, c.getOrElse("multifurnace_cool_base", 1.5));
        multifurnaceCoolFactor = Math.max(1.0, c.getOrElse("multifurnace_cool_factor", 1.6));
        multifurnaceStabilityPenalty = Math.max(0, c.getOrElse("multifurnace_stability_penalty", 40));
        multifurnacePharmaGain = Math.max(0, c.getOrElse("multifurnace_pharma_gain", 500));
        maxFurnaceSize = Math.clamp(c.getOrElse("max_furnace_size", 7), 3, 31);
        multifurnaceSlotsBase = Math.max(1, c.getOrElse("multifurnace_slots_base", 6));
        multifurnaceSlotsGain = Math.max(1, c.getOrElse("multifurnace_slots_gain", 3));
        multifurnaceSlotsCap = Math.max(multifurnaceSlotsBase, c.getOrElse("multifurnace_slots_cap", 128));
        multifurnaceLightLevel = Math.clamp(c.getOrElse("multifurnace_light_level", 15), 0, 15);
        save();
    }

    private static Config readConfig() {
        try {
            var text = Files.readString(CONFIG_PATH);
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') text = text.substring(1);
            return new TomlParser().parse(new StringReader(text));
        } catch (Exception e) {
            Elixir.LOGGER.error("[E]读取配置文件失败，将使用默认配置", e);
            return Config.of(() -> new HashMap<String, Object>(), InMemoryFormat.defaultInstance());
        }
    }

    public static void restoreLocal() {
        serverSynced = false;
        maxFurnaceSize = localMaxFurnaceSize;
        multifurnaceSlotsBase = localMultifurnaceSlotsBase;
        multifurnaceSlotsGain = localMultifurnaceSlotsGain;
        multifurnaceSlotsCap = localMultifurnaceSlotsCap;
        multifurnaceLightLevel = localMultifurnaceLightLevel;
    }
}

package net.per.elixir;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.HashMap;

public class ElixirConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("elixir.toml");
    public static int pharmaLimited;
    public static int pharmaConversionRate;
    public static int highestPharmaLimited;
    public static int timeConversionRate;
    public static double attributeModifierDilute;
    public static double effectDilute;
    public static int extremeTemperatureRange;
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

    public static void save() {
        var map = new HashMap<String, Object>();
        map.put("pharma_limited", pharmaLimited);
        map.put("pharma_conversion_rate", pharmaConversionRate);
        map.put("highest_pharma_limited", highestPharmaLimited);
        map.put("time_conversion_rate", timeConversionRate);
        map.put("attribute_modifier_dilute", attributeModifierDilute);
        map.put("effect_dilute", effectDilute);
        map.put("extreme_temperature_range", extremeTemperatureRange);
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
        new TomlWriter().write(Config.of(() -> map, InMemoryFormat.defaultInstance()).unmodifiable(), CONFIG_PATH, WritingMode.REPLACE);
    }

    public static void load() {
        var c = new TomlParser().parse(CONFIG_PATH, (file, configFormat) -> false);
        pharmaLimited = c.getOrElse("pharma_limited", 1000);
        pharmaConversionRate = c.getOrElse("pharma_conversion_rate", 10);
        highestPharmaLimited = c.getOrElse("highest_pharma_limited", 1000);
        timeConversionRate = c.getOrElse("time_conversion_rate", 100);
        attributeModifierDilute = c.getOrElse("attribute_modifier_dilute", 100.0);
        effectDilute = c.getOrElse("effect_dilute", 10.0);
        extremeTemperatureRange = Math.max(6, ((Number) c.getOrElse("extreme_temperature_range", 30)).intValue());
        badElixirCompensation = c.getOrElse("bad_elixir_compensation", 0.3);
        stabilityLossRate = c.getOrElse("stability_loss_rate", 10.0);
        refineTicks = c.getOrElse("refine_ticks", 20);
        expSuccessGain = c.getOrElse("exp_success_gain", 3);
        expFailureGain = c.getOrElse("exp_failure_gain", 1);
        expGrowthRate = Math.max(1, c.getOrElse("exp_growth_rate", 200));
        tempTargetMargin = Math.clamp(c.getOrElse("temp_target_margin", 5), 0, 250);
        tempSafeMargin = Math.max(1, c.getOrElse("temp_safe_margin", 100));
        tempRangeBase = Math.max(0, c.getOrElse("temp_range_base", 40));
        explodeDelayBase = Math.max(1, c.getOrElse("explode_delay_base", 100));
        explodeDelayGain = Math.max(0, c.getOrElse("explode_delay_gain", 100));
        failedDelayBase = Math.max(1, c.getOrElse("failed_delay_base", 140));
        failedDelayGain = Math.max(0, c.getOrElse("failed_delay_gain", 60));
        hudScale = Math.clamp(c.getOrElse("hud_scale", 1.5), 1.0, 4.0);
    }
}

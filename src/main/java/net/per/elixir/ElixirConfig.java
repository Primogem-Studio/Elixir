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
        map.put("stability_loss_rate ", stabilityLossRate);
        map.put("refine_ticks ", refineTicks);
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
        extremeTemperatureRange = c.getOrElse("extreme_temperature_range", 100);
        badElixirCompensation = c.getOrElse("bad_elixir_compensation", 0.3);
        stabilityLossRate = c.getOrElse("stability_loss_rate", 10);
        refineTicks = c.getOrElse("refine_ticks", 20);
    }
}

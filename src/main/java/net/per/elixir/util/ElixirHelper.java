package net.per.elixir.util;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.per.elixir.registry.ElixirRegistries;
import net.per.elixir.registry.data.Material;

import java.util.HashMap;
import java.util.Map;

public class ElixirHelper {
    private static Map<Item, Holder<Material>> mains = Map.of();
    private static Map<Item, Holder<Material>> offs = Map.of();

    public static void flush(RegistryAccess registries) {
        var main = new HashMap<Item, Holder<Material>>();
        var off = new HashMap<Item, Holder<Material>>();
        for (var h : registries.registryOrThrow(ElixirRegistries.MATERIAL).holders().toList()) {
            var item = h.value().item().value();
            if (item == Items.AIR) continue;
            if (h.value().main()) main.putIfAbsent(item, h);
            else off.putIfAbsent(item, h);
        }
        mains = ImmutableMap.copyOf(main);
        offs = ImmutableMap.copyOf(off);
    }

    public static void execute(Material m, int pharm, int time, ItemStack stack, Level level, LivingEntity entity) {
        m.effect().ifLeft(e -> e.value().onAction(pharm, time, stack, level, entity));
    }

    public static int calc(Material m, int sum, double base) {
        return m.effect().right().map(c -> c.value().calc(sum, base)).orElse(1);
    }

    public static Holder<Material> findMain(Item item) {
        return mains.get(item);
    }

    public static Holder<Material> findOff(Item item) {
        return offs.get(item);
    }

    public static boolean hasMaterial(Item item) {
        return mains.containsKey(item) || offs.containsKey(item);
    }
}

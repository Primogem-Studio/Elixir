package net.per.elixir.util;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

import static net.per.elixir.Elixir.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class ModifierUtil {
    private static class Entry {
        final LivingEntity entity;
        final Holder<Attribute> attr;
        final ResourceLocation id;
        int time;

        public Entry(LivingEntity entity, Holder<Attribute> attr, ResourceLocation id, int time) {
            this.entity = entity;
            this.attr = attr;
            this.id = id;
            this.time = time;
        }
    }

    private static final List<Entry> entries = new ArrayList<>();

    public static void addTimeLimited(LivingEntity entity, int time, Holder<Attribute> attr, AttributeModifier modifier) {
        entries.add(new Entry(entity, attr, modifier.id(), time));
        var ins = entity.getAttribute(attr);
        if (ins != null) ins.addOrUpdateTransientModifier(modifier);
    }

    @SubscribeEvent
    private static void onTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide) return;
        for (var entry : ImmutableList.copyOf(entries)) {
            entry.time--;
            if (entry.time <= 0) {
                var ins = entry.entity.getAttribute(entry.attr);
                if (ins != null) ins.removeModifier(entry.id);
                entries.remove(entry);
            }
        }
    }
}
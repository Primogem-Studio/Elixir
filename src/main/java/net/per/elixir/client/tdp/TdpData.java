package net.per.elixir.client.tdp;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.per.elixir.registry.ElixirRegistries;
import net.per.elixir.registry.data.Material;

import java.util.ArrayList;
import java.util.List;

public final class TdpData {
    private static Registry<Material> cacheReg;
    private static List<Holder<Material>> cacheMains = List.of();
    private static List<Holder<Material>> cacheOffs = List.of();

    private TdpData() {
    }

    public static RegistryAccess access() {
        var mc = Minecraft.getInstance();
        return mc.level == null ? null : mc.level.registryAccess();
    }

    public static List<Holder<Material>> materials(boolean main) {
        var access = access();
        if (access == null) return List.of();
        var registry = access.registry(ElixirRegistries.MATERIAL).orElse(null);
        if (registry == null) return List.of();
        if (registry != cacheReg) {
            cacheReg = registry;
            var mains = new ArrayList<Holder<Material>>();
            var offs = new ArrayList<Holder<Material>>();
            for (var h : registry.holders().toList()) {
                var m = h.value();
                if (m.main()) mains.add(h);
                else offs.add(h);
            }
            cacheMains = List.copyOf(mains);
            cacheOffs = List.copyOf(offs);
        }
        return main ? cacheMains : cacheOffs;
    }

    public static Holder<Material> byId(String id) {
        var access = access();
        if (access == null) return null;
        try {
            return access.lookupOrThrow(ElixirRegistries.MATERIAL)
                    .get(ResourceKey.create(ElixirRegistries.MATERIAL, ResourceLocation.parse(id))).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static String id(Holder<Material> m) {
        return m.unwrapKey().map(k -> k.location().toString()).orElse("?");
    }

    public static ItemStack icon(Holder<Material> m) {
        return new ItemStack(m.value().item());
    }

    public static boolean isEmpty(Holder<Material> m) {
        return m.value().item().value() == Items.AIR;
    }

    public static void drawCell(net.minecraft.client.gui.GuiGraphics g, int x, int y, int size,
                                Holder<Material> m, boolean hover, boolean sel, int selColor) {
        if (hover) TdpUi.fill(g, x, y, size, size, 0x22FFFFFF);
        if (sel) TdpUi.frame(g, x, y, size, size, selColor);
        else if (hover) TdpUi.frame(g, x, y, size, size, 0xFFB9C0C8);
        if (isEmpty(m)) {
            int inner = size - 8;
            int off = (size - inner) / 2;
            TdpUi.fill(g, x + off, y + off, inner, inner, 0xAA2C2E33);
            TdpUi.frame(g, x + off, y + off, inner, inner, 0xFF55585D);
        } else {
            int off = (size - 16) / 2;
            g.renderItem(icon(m), x + off, y + off);
        }
    }

    public static Component name(Holder<Material> m) {
        var v = m.value();
        return v.main() ? Component.translatable(v.item().value().getDescriptionId())
                : Component.translatable(v.nameKey(m));
    }

    public static int tint(Holder<Material> m) {
        var colors = m.value().colors();
        if (colors == null || colors.length == 0) return 0xFFFFFF;
        int c = colors[0];
        return (c >> 16 & 0xFF) << 16 | (c >> 8 & 0xFF) << 8 | (c & 0xFF);
    }

    public static Component description(Holder<Material> m) {
        return m.value().hasDescription()
                ? Component.translatable(m.value().description())
                : Component.translatable("item.elixir.material.unknown");
    }

    public static String effectName(Holder<Material> m) {
        var key = m.value().effect().left()
                .flatMap(Holder::unwrapKey)
                .map(k -> "item.elixir.action." + k.location().toLanguageKey())
                .orElse(null);
        return key == null ? "" : Component.translatable(key).getString();
    }

    public static List<Component> statLines(Holder<Material> m) {
        var v = m.value();
        var list = new ArrayList<Component>();
        int label = 0xB4FF59;
        int value = 0xE09EFF;
        if (v.main()) {
            var effect = v.effect().left().orElseThrow().unwrapKey().orElseThrow().location().toLanguageKey();
            list.add(Component.translatable("item.elixir.material.main")
                    .append(Component.translatable("item.elixir.action." + effect)).withColor(label));
        } else {
            list.add(Component.translatable("item.elixir.material.off")
                    .append(Component.translatable(v.nameKey(m))).withColor(label));
        }
        list.add(Component.translatable("item.elixir.material.pharm").withColor(label)
                .append(Component.literal(String.valueOf(v.pharm())).withColor(value)));
        list.add(Component.translatable("item.elixir.material.stability").withColor(label)
                .append(Component.literal(String.valueOf(v.stability())).withColor(value)));
        if (!v.main()) {
            list.add(Component.translatable("item.elixir.material.base").withColor(label)
                    .append(Component.literal(String.valueOf(v.base())).withColor(value)));
        }
        return list;
    }

    public static void hoverMaterial(TdpScreen host, net.minecraft.client.gui.GuiGraphics g, Holder<Material> m,
                                     int mouseX, int mouseY) {
        if (isEmpty(m)) {
            host.queueHover(mouseX, mouseY, 120, 0xFFC9CFD6,
                    Component.translatable("gui.elixir.tdp.empty.off"), List.of(), null);
            return;
        }
        int tint = tint(m);
        if (tint == 0xFFFFFF) tint = 0xFFB8C6FF;
        else tint |= 0xFF000000;
        host.queueHover(mouseX, mouseY, 160, tint, name(m).copy().withColor(tint).append(Component.literal("  " + id(m))), statLines(m), description(m));
    }
}

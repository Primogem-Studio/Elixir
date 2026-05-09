package net.per.elixir.event;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.per.elixir.client.ConfigScreen;
import net.per.elixir.client.ElixirFurnaceScreen;
import net.per.elixir.data.ElixirFurnaceMenu;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.registry.ElixirDataComponents;
import net.per.elixir.registry.ElixirEntityTypes;
import net.per.elixir.registry.ElixirItems;
import net.per.elixir.registry.data.Material;
import net.per.elixir.render.tooltip.AlchemicalFormulaTooltip;
import net.per.elixir.util.ElixirHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static net.per.elixir.Elixir.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ClientEvent {
    @SubscribeEvent
    private static void onSetup(FMLClientSetupEvent event) {
        var container = ModList.get().getModContainerById(MOD_ID).orElseThrow();
        if (ModList.get().isLoaded("cloth_config"))
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigScreen::create);
    }

    @SubscribeEvent
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ElixirEntityTypes.elixir.get(), ThrownItemRenderer::new);
//        event.registerBlockEntityRenderer(ElixirFurnaceBlockEntity.Type, ElixirFurnaceRenderer::new);
    }

    @SubscribeEvent
    private static void onRegisterColorHandlers(RegisterColorHandlersEvent.Item event) {
        event.register(ClientEvent::getColor, ElixirItems.elixir);
    }

    @SubscribeEvent
    private static void onRegisterColorHandlers(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> 0xA3FF75, ElixirBlocks.alchemical_vessel.get());
    }

    @SubscribeEvent
    private static void onRegisterClientTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(AlchemicalFormulaTooltip.class, Function.identity());
    }

    @SubscribeEvent
    private static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        var item = event.getItemStack();
        if (!item.has(ElixirDataComponents.AlchemicalFormula)) return;
//        if (!Screen.hasShiftDown()) {
//            event.getTooltipElements().add(Either.left(Component.translatable("tooltip.alchemical_formula.shift").withColor(0xB4FF59)));
//            return;
//        }
        event.getTooltipElements().add(1, Either.right(new AlchemicalFormulaTooltip(item.get(ElixirDataComponents.AlchemicalFormula))));
    }

    @SubscribeEvent
    private static void onTooltip(ItemTooltipEvent event) {
        var it = event.getItemStack();
        var main = ElixirHelper.findMain(it.getItem());
        var off = ElixirHelper.findOff(it.getItem());
        var flag = it.has(ElixirDataComponents.MaterialPropertySwitching);
        boolean flag2 = it.getOrDefault(ElixirDataComponents.MaterialPropertySwitching, false);
        var tips = new ArrayList<Component>();
        if (!event.getFlags().hasShiftDown()) {
            if (main != null || off != null) {
                event.getToolTip().add(1, Component.translatable("tooltip.alchemical_formula.shift2").withColor(0xB4FF59));
                return;
            }
        }
        if (flag) {
            if (main != null && !flag2) addMainMaterialTips(tips, main);
            if (off != null && flag2) addOffMaterialTips(tips, off);
        } else {
            if (main != null) addMainMaterialTips(tips, main);
            if (off != null) addOffMaterialTips(tips, off);
        }
        event.getToolTip().addAll(1, tips);
    }

    private static void addMainMaterialTips(List<Component> tips, Holder<Material> m) {
        tips.add(Component.translatable("item.elixir.material.main").append(Component.translatable("item.elixir.action." + m.value().effect().left().orElseThrow().unwrapKey().orElseThrow().location().toLanguageKey())).withColor(0xB4FF59));
        tips.add(Component.translatable("item.elixir.material.pharm").withColor(0xB4FF59).append(Component.literal(String.valueOf(m.value().pharm())).withColor(0xE09EFF)));
        tips.add(Component.translatable("item.elixir.material.stability").withColor(0xB4FF59).append(Component.literal(String.valueOf(m.value().stability())).withColor(0xE09EFF)));
    }

    private static void addOffMaterialTips(List<Component> tips, Holder<Material> m) {
        tips.add(Component.translatable("item.elixir.material.off").append(Component.translatable("item.elixir.material.name." + m.unwrapKey().orElseThrow().location().toLanguageKey())).withColor(0xB4FF59));
        tips.add(Component.translatable("item.elixir.material.pharm").withColor(0xB4FF59).append(Component.literal(String.valueOf(m.value().pharm())).withColor(0xE09EFF)));
        tips.add(Component.translatable("item.elixir.material.stability").withColor(0xB4FF59).append(Component.literal(String.valueOf(m.value().stability())).withColor(0xE09EFF)));
        tips.add(Component.translatable("item.elixir.material.base").withColor(0xB4FF59).append(Component.literal(String.valueOf(m.value().base())).withColor(0xE09EFF)));
    }

    private static int getColor(ItemStack stack, int layer) {
        var com = stack.get(ElixirDataComponents.Elixir);
        if (com == null) return -1;
        int color = -1;
        for (var m : com.main()) {
            var colors = m.value().colors();
            if (colors == null || colors.length == 0) return -1;
            var c = colors.length >= layer + 1 ? colors[layer] : adjustColor(colors[0], layer);
            if (layer == 0 && colors.length == 1) c = adjustColor(c, layer);
            color = blend(color, c, 0.5f);
        }
        return color;
    }

    private static int adjustColor(int color, int layer) {
        return switch (layer) {
            case 1 -> adjustColor(color, 0, 0.7f, 0.15f);
            case 2 -> adjustColor(color, -3, 0.5f, -0.2f);
            case 3 -> adjustColor(color, 1, 1f, -0.6f);
            default -> adjustColor(color, 0, 1f, 0.5f);
        };
    }

    private static int adjustColor(int color, float dH, float dS, float dV) {
        double r = ((color >> 16) & 0xFF) / 255.0, g = ((color >> 8) & 0xFF) / 255.0, b = (color & 0xFF) / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), delta = max - min;
        double v = max, s = max == 0 ? 0 : delta / max, h = 0;
        if (delta != 0) {
            h = max == r ? (g - b) / delta * 60 : max == g ? (b - r) / delta * 60 + 120 : (r - g) / delta * 60 + 240;
            if (h < 0) h += 360;
        }
        h = (h + dH) % 360;
        if (h < 0) h += 360;
        s = Math.clamp(s + dS, 0, 1);
        v = Math.clamp(v + dV, 0, 1);
        double c = v * s, x = c * (1 - Math.abs((h / 60) % 2 - 1)), m = v - c;
        int sec = (int) (h / 60);
        double r1 = 0, g1 = 0, b1 = 0;
        switch (sec) {
            case 0 -> {
                r1 = c;
                g1 = x;
            }
            case 1 -> {
                r1 = x;
                g1 = c;
            }
            case 2 -> {
                g1 = c;
                b1 = x;
            }
            case 3 -> {
                g1 = x;
                b1 = c;
            }
            case 4 -> {
                r1 = x;
                b1 = c;
            }
            case 5 -> {
                r1 = c;
                b1 = x;
            }
        }
        return (color & 0xFF000000) | ((int) ((r1 + m) * 255) << 16) | ((int) ((g1 + m) * 255) << 8) | (int) ((b1 + m) * 255);
    }

    private static int blend(int color1, int color2, float ratio) {
        int a = (int) ((color1 >> 24 & 0xFF) * (1 - ratio) + (color2 >> 24 & 0xFF) * ratio);
        int r = (int) (((color1 >> 16) & 0xFF) * (1 - ratio) + ((color2 >> 16) & 0xFF) * ratio);
        int g = (int) (((color1 >> 8) & 0xFF) * (1 - ratio) + ((color2 >> 8) & 0xFF) * ratio);
        int b = (int) ((color1 & 0xFF) * (1 - ratio) + (color2 & 0xFF) * ratio);
        return a << 24 | r << 16 | g << 8 | b;
    }

    @SubscribeEvent
    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ElixirFurnaceMenu.Type, ElixirFurnaceScreen::new);
    }
}

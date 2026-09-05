package net.per.elixir.event;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.per.elixir.ElixirConfig;
import net.per.elixir.block.entity.LargeFurnaceBlockEntity;
import net.per.elixir.client.ConfigScreen;
import net.per.elixir.client.DanPouchScreen;
import net.per.elixir.client.DanWheelClient;
import net.per.elixir.client.ElixirFurnaceScreen;
import net.per.elixir.client.LargeFurnaceScreen;
import net.per.elixir.data.DanPouchMenu;
import net.per.elixir.data.ElixirFurnaceMenu;
import net.per.elixir.data.LargeFurnaceMenu;
import net.per.elixir.item.DanPouchItem;
import net.per.elixir.network.OpenPouchPayload;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.registry.ElixirDataComponents;
import net.per.elixir.registry.ElixirEntityTypes;
import net.per.elixir.registry.ElixirItems;
import net.per.elixir.registry.ElixirRegistries;
import net.per.elixir.registry.data.FurnaceVisual;
import net.per.elixir.registry.data.Material;
import net.per.elixir.render.entity.block.LargeFurnaceRenderer;
import net.per.elixir.render.tooltip.AlchemicalFormulaDetailTooltip;
import net.per.elixir.render.tooltip.AlchemicalFormulaTooltip;
import net.per.elixir.util.ElixirHelper;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static net.per.elixir.Elixir.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ClientEvent {
    public static final ResourceLocation ELIXIR_BLOCK_ATLAS = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/atlas/blocks.png");
    @SubscribeEvent
    private static void onSetup(FMLClientSetupEvent event) {
        var container = ModList.get().getModContainerById(MOD_ID).orElseThrow();
        if (ModList.get().isLoaded("cloth_config"))
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigScreen::create);
    }

    @SubscribeEvent
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ElixirConfig.restoreLocal();
    }

    @SubscribeEvent
    private static void onLevelLoad(LevelEvent.Load event) {
        var access = event.getLevel().registryAccess();
        if (access.registry(ElixirRegistries.MATERIAL).isPresent()) {
            ElixirHelper.flushClient(access);
        }
    }

    @SubscribeEvent
    private static void onTagsUpdated(TagsUpdatedEvent event) {
        var access = event.getRegistryAccess();
        if (access.registry(ElixirRegistries.MATERIAL).isPresent()) {
            ElixirHelper.flushClient(access);
        }
    }

    @SubscribeEvent
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ElixirEntityTypes.elixir.get(), ThrownItemRenderer::new);
        event.registerBlockEntityRenderer(LargeFurnaceBlockEntity.Type, LargeFurnaceRenderer::new);
    }

    @SubscribeEvent
    private static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(MOD_ID, "block/elixir_furnace_test"), "standalone"));
        event.register(new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(MOD_ID, "block/elixir_furnace_mask"), "standalone"));
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        level.registryAccess().registry(ElixirRegistries.FURNACE_VISUAL)
                .ifPresent(registry -> registry.forEach(visual -> collectModels(visual, event)));
    }

    @SubscribeEvent
    private static void onRegisterMaterialAtlases(RegisterMaterialAtlasesEvent event) {
        event.register(ELIXIR_BLOCK_ATLAS, ResourceLocation.fromNamespaceAndPath(MOD_ID, "blocks"));
    }

    private static void collectModels(FurnaceVisual visual, ModelEvent.RegisterAdditional event) {
        visual.model().ifPresent(rl -> event.register(new ModelResourceLocation(rl, "standalone")));
        visual.coverModel().ifPresent(rl -> event.register(new ModelResourceLocation(rl, "standalone")));
        visual.tiers().values().forEach(v -> collectModels(v, event));
        visual.options().forEach(v -> collectModels(v, event));
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
        event.register(AlchemicalFormulaDetailTooltip.class, Function.identity());
    }

    @SubscribeEvent
    private static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        var item = event.getItemStack();
        if (item.getItem() instanceof DanPouchItem) {
            var pouchComp = item.get(ElixirDataComponents.DanPouch);
            var pill = pouchComp != null && pouchComp.selected() >= 0 ? pouchComp.get(pouchComp.selected()) : ItemStack.EMPTY;
            var pillElixir = pill.get(ElixirDataComponents.Elixir);
            if (Screen.hasShiftDown() && !pill.isEmpty() && pillElixir != null) {
                var rows = AlchemicalFormulaDetailTooltip.rowsOf(pillElixir);
                AlchemicalFormulaDetailTooltip.begin(pillElixir, rows.size());
                event.getTooltipElements().add(1, Either.right(new AlchemicalFormulaDetailTooltip(rows)));
            } else {
                AlchemicalFormulaDetailTooltip.clear();
            }
            return;
        }
        var formula = item.get(ElixirDataComponents.AlchemicalFormula);
        var elixir = item.get(ElixirDataComponents.Elixir);
        var show = Screen.hasShiftDown() && (formula != null || elixir != null && !isRandomPill(item));
        if (!show) {
            AlchemicalFormulaDetailTooltip.clear();
            if (formula != null) {
                event.getTooltipElements().add(1, Either.right(new AlchemicalFormulaTooltip(formula)));
            }
            return;
        }
        if (formula != null) {
            var rows = AlchemicalFormulaDetailTooltip.rowsOf(formula);
            AlchemicalFormulaDetailTooltip.begin(formula, rows.size());
            event.getTooltipElements().add(1, Either.right(new AlchemicalFormulaDetailTooltip(rows)));
        } else {
            var rows = AlchemicalFormulaDetailTooltip.rowsOf(elixir);
            AlchemicalFormulaDetailTooltip.begin(elixir, rows.size());
            event.getTooltipElements().add(1, Either.right(new AlchemicalFormulaDetailTooltip(rows)));
        }
    }

    @SubscribeEvent
    private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (AlchemicalFormulaDetailTooltip.scroll(event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private static void onScreenMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (AlchemicalFormulaDetailTooltip.scroll(event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private static void onClientTick(ClientTickEvent.Post event) {
        AlchemicalFormulaDetailTooltip.onFrame();
        DanWheelClient.tick();
    }

    @SubscribeEvent
    private static void onInteractionKeyTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown()) return;
        if (mc.player.getMainHandItem().getItem() instanceof DanPouchItem) {
            event.setCanceled(true);
            PacketDistributor.sendToServer(new OpenPouchPayload());
        }
    }

    @SubscribeEvent
    private static void onTooltip(ItemTooltipEvent event) {
        var it = event.getItemStack();
        if (it.getItem() instanceof DanPouchItem) {
            if (event.getFlags().hasShiftDown()) {
                event.getToolTip().removeIf(line -> line.getContents() instanceof TranslatableContents t && t.getKey().startsWith("item.elixir.dan_pouch.usage."));
            } else {
                event.getToolTip().add(1, Component.translatable("tooltip.dan_pouch.shift").withColor(0xB4FF59));
            }
            return;
        }
        if (it.has(ElixirDataComponents.AlchemicalFormula) || it.has(ElixirDataComponents.Elixir) && !isRandomPill(it)) {
            if (!event.getFlags().hasShiftDown()) {
                var hint = Component.translatable("tooltip.alchemical_formula.shift").withColor(0xB4FF59);
                if (it.has(ElixirDataComponents.Elixir)) event.getToolTip().add(Math.min(2, event.getToolTip().size()), hint);
                else event.getToolTip().add(1, hint);
            }
            return;
        }
        var main = ElixirHelper.findMainClient(it.getItem());
        var off = ElixirHelper.findOffClient(it.getItem());
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
        tips.add(Component.translatable("item.elixir.material.off").append(Component.translatable(m.value().nameKey(m))).withColor(0xB4FF59));
        tips.add(Component.translatable("item.elixir.material.pharm").withColor(0xB4FF59).append(Component.literal(String.valueOf(m.value().pharm())).withColor(0xE09EFF)));
        tips.add(Component.translatable("item.elixir.material.stability").withColor(0xB4FF59).append(Component.literal(String.valueOf(m.value().stability())).withColor(0xE09EFF)));
        tips.add(Component.translatable("item.elixir.material.base").withColor(0xB4FF59).append(Component.literal(String.valueOf(m.value().base())).withColor(0xE09EFF)));
    }

    private static int getColor(ItemStack stack, int layer) {
        var com = stack.get(ElixirDataComponents.Elixir);
        if (com == null) return -1;
        int textureLayer = 3 - layer;
        Holder<Material> custom = null;
        var mixing = new ArrayList<Holder<Material>>();
        for (var m : com.main()) {
            var colors = m.value().colors();
            if (colors == null || colors.length == 0) return -1;
            if (colors.length == 4) {
                if (custom == null) custom = m;
            } else {
                mixing.add(m);
            }
        }
        if (!mixing.isEmpty()) {
            int color = -1;
            int i = 1;
            for (var m : mixing) {
                var colors = m.value().colors();
                var c = colors.length == 1 ? colors[0] : colors[Math.min(textureLayer, colors.length - 1)];
                color = color == -1 ? c : blend(color, c, 1.0f / i);
                i++;
            }
            return adjustColor(color, textureLayer);
        }
        if (custom == null) return -1;
        var colors = custom.value().colors();
        return colors[Math.min(textureLayer, colors.length - 1)];
    }

    private static int adjustColor(int color, int layer) {
        return switch (layer) {
            case 0 -> adjustColor(color, 30, 0.5f, 1.2f);
            case 1 -> adjustColor(color, 10, 0.85f, 1.08f);
            case 2 -> adjustColor(color, -35, 1f, 0.65f);
            default -> adjustColor(color, -2, 0.96f, 1.03f);
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
        s = Math.clamp(s * dS, 0, 1);
        v = Math.clamp(v * dV, 0.12, 1);
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

    private static boolean isRandomPill(ItemStack stack) {
        var name = stack.get(DataComponents.ITEM_NAME);
        return name != null && name.getContents() instanceof TranslatableContents t && t.getKey().equals("item.elixir.failed");
    }

    @SubscribeEvent
    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ElixirFurnaceMenu.Type, ElixirFurnaceScreen::new);
        event.register(LargeFurnaceMenu.Type, LargeFurnaceScreen::new);
        event.register(DanPouchMenu.Type, DanPouchScreen::new);
    }
}

package net.per.elixir.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.per.elixir.data.ElixirComponent;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.per.elixir.Elixir.MOD_ID;
import static net.per.elixir.registry.ElixirDataComponents.Elixir;
import static net.per.elixir.registry.ElixirItems.*;

public class ElixirTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final Supplier<CreativeModeTab> tab1 = TABS.register("tab1", () -> CreativeModeTab.builder()
            .icon(elixir::toStack)
            .title(Component.translatable("itemGroup.elixir.tab1"))
            .displayItems((flags, output) -> {
                var reg = flags.holders().lookupOrThrow(ElixirRegistries.MATERIAL);
                for (var m : reg.listElements().toList()) {
                    if (!m.value().main()) continue;
                    output.accept(withComp(elixir, s -> s.set(Elixir, new ElixirComponent(reg.getOrThrow(ResourceKey.create(ElixirRegistries.MATERIAL, ResourceLocation.fromNamespaceAndPath(MOD_ID, "off/empty"))), 999, List.of(m)))));
                }
            })
            .build());
    public static final Supplier<CreativeModeTab> tab2 = TABS.register("tab2", () -> CreativeModeTab.builder()
            .icon(elixir_furnace::toStack)
            .title(Component.translatable("itemGroup.elixir.tab2"))
            .displayItems((flags, output) -> {
                output.accept(elixir_furnace);
                output.accept(elixir_furnace_cover);
                output.accept(elixir_furnace_brick);
                output.accept(alchemical_vessel);
                output.accept(handheld_fan);
            })
            .build());

    private static ItemStack withComp(ItemLike item, Consumer<ItemStack> consumer) {
        var stack = new ItemStack(item);
        consumer.accept(stack);
        return stack;
    }
}

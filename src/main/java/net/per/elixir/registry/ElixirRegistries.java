package net.per.elixir.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.per.elixir.registry.data.Material;
import net.per.elixir.util.IElixirAction;
import net.per.elixir.util.IElixirCalc;

import static net.per.elixir.Elixir.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class ElixirRegistries {
    public static final ResourceKey<Registry<Material>> MATERIAL = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, "material"));
    public static final ResourceKey<Registry<IElixirAction>> ACTION = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, "action"));
    public static final ResourceKey<Registry<IElixirCalc>> CALCULATOR = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, "calculator"));
    public static final Registry<IElixirAction> ACTION_REGISTRY = new RegistryBuilder<>(ACTION).sync(true).create();
    public static final Registry<IElixirCalc> CALCULATOR_REGISTRY = new RegistryBuilder<>(CALCULATOR).sync(true).create();

    @SubscribeEvent
    private static void registerRegistries(NewRegistryEvent event) {
        event.register(ACTION_REGISTRY);
        event.register(CALCULATOR_REGISTRY);
    }

    @SubscribeEvent
    private static void registerDynamicRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(MATERIAL, Material.CODEC, Material.CODEC);
    }
}

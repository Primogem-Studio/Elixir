package net.per.elixir.network;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.per.elixir.data.AlchemicalFormulaComponent;
import net.per.elixir.registry.ElixirDataComponents;
import net.per.elixir.registry.ElixirRegistries;
import net.per.elixir.registry.data.Material;

import java.util.ArrayList;
import java.util.List;

import static net.per.elixir.Elixir.MOD_ID;

public record TdpCraftFormulaPayload(String name, int count, List<Content> main, List<Content> off) implements CustomPacketPayload {
    public record Content(String material, int count) {
    }

    public static final Type<TdpCraftFormulaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "tdp_craft_formula"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TdpCraftFormulaPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TdpCraftFormulaPayload decode(RegistryFriendlyByteBuf buf) {
            String name = null;
            if (buf.readBoolean()) name = buf.readUtf();
            int count = buf.readVarInt();
            var main = readContents(buf);
            var off = readContents(buf);
            return new TdpCraftFormulaPayload(name, count, main, off);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TdpCraftFormulaPayload value) {
            buf.writeBoolean(value.name != null);
            if (value.name != null) buf.writeUtf(value.name);
            buf.writeVarInt(value.count);
            writeContents(buf, value.main);
            writeContents(buf, value.off);
        }
    };

    private static List<Content> readContents(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        var list = new ArrayList<Content>(size);
        for (int i = 0; i < size; i++) {
            list.add(new Content(buf.readUtf(), buf.readVarInt()));
        }
        return List.copyOf(list);
    }

    private static void writeContents(RegistryFriendlyByteBuf buf, List<Content> list) {
        buf.writeVarInt(list.size());
        for (var c : list) {
            buf.writeUtf(c.material);
            buf.writeVarInt(c.count);
        }
    }

    public static void handle(TdpCraftFormulaPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer sp)) return;
            int n = Math.max(1, Math.min(64, payload.count));
            var registry = sp.serverLevel().registryAccess().lookupOrThrow(ElixirRegistries.MATERIAL);
            var main = resolve(registry, payload.main);
            var off = resolve(registry, payload.off);
            if (main == null || off == null || main.isEmpty()) return;
            var paper = new ItemStack(Items.PAPER);
            paper.set(ElixirDataComponents.AlchemicalFormula, new AlchemicalFormulaComponent(main, off));
            if (payload.name != null && !payload.name.isBlank()) {
                paper.set(DataComponents.CUSTOM_NAME, Component.literal(payload.name));
            }
            int given = TdpCraftPillPayload.give(sp, paper, n);
            sp.displayClientMessage(Component.translatable("message.elixir.tdp.given.formula", given), true);
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        });
    }

    private static List<AlchemicalFormulaComponent.Content> resolve(
            HolderLookup.RegistryLookup<Material> registry, List<Content> contents) {
        if (contents.isEmpty()) return List.of();
        var list = new ArrayList<AlchemicalFormulaComponent.Content>(contents.size());
        for (var c : contents) {
            var h = registry.get(ResourceKey.create(ElixirRegistries.MATERIAL, ResourceLocation.parse(c.material))).orElse(null);
            if (h == null) return null;
            list.add(new AlchemicalFormulaComponent.Content(h, Math.max(1, c.count)));
        }
        return list;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

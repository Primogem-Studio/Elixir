package net.per.elixir.network;

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
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.per.elixir.data.ElixirComponent;
import net.per.elixir.item.ElixirItem;
import net.per.elixir.registry.ElixirDataComponents;
import net.per.elixir.registry.ElixirItems;
import net.per.elixir.registry.ElixirRegistries;

import java.util.ArrayList;
import java.util.List;

import static net.per.elixir.Elixir.MOD_ID;

public record TdpCraftPillPayload(int count, int pharm, String off, List<String> main, String name) implements CustomPacketPayload {
    public static final Type<TdpCraftPillPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "tdp_craft_pill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TdpCraftPillPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TdpCraftPillPayload decode(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            int pharm = buf.readVarInt();
            String off = buf.readUtf();
            int size = buf.readVarInt();
            var main = new ArrayList<String>(size);
            for (int i = 0; i < size; i++) main.add(buf.readUtf());
            String name = null;
            if (buf.readBoolean()) name = buf.readUtf();
            return new TdpCraftPillPayload(count, pharm, off, List.copyOf(main), name);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TdpCraftPillPayload value) {
            buf.writeVarInt(value.count);
            buf.writeVarInt(value.pharm);
            buf.writeUtf(value.off);
            buf.writeVarInt(value.main.size());
            for (var id : value.main) buf.writeUtf(id);
            buf.writeBoolean(value.name != null);
            if (value.name != null) buf.writeUtf(value.name);
        }
    };

    public static void handle(TdpCraftPillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer sp)) return;
            int n = Math.max(1, Math.min(512, payload.count));
            var registry = sp.serverLevel().registryAccess().lookupOrThrow(ElixirRegistries.MATERIAL);
            var off = registry.get(ResourceKey.create(ElixirRegistries.MATERIAL, ResourceLocation.parse(payload.off))).orElse(null);
            if (off == null) return;
            var mains = new ArrayList<net.minecraft.core.Holder<net.per.elixir.registry.data.Material>>(payload.main.size());
            for (var id : payload.main) {
                var h = registry.get(ResourceKey.create(ElixirRegistries.MATERIAL, ResourceLocation.parse(id))).orElse(null);
                if (h == null) return;
                mains.add(h);
            }
            var elixir = new ItemStack(ElixirItems.elixir.get());
            elixir.set(ElixirDataComponents.Elixir, new ElixirComponent(off, payload.pharm, List.copyOf(mains)));
            if (payload.name != null && !payload.name.isBlank()) {
                elixir.set(DataComponents.ITEM_NAME, Component.literal(payload.name).withColor(ElixirItem.getColor(elixir.get(ElixirDataComponents.Elixir))));
            }
            int given = give(sp, elixir, n);
            sp.displayClientMessage(Component.translatable("message.elixir.tdp.given.pill", given), true);
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        });
    }

    public static int give(ServerPlayer sp, ItemStack stack, int total) {
        int given = 0;
        int max = Math.min(64, stack.getMaxStackSize());
        while (given < total) {
            var copy = stack.copy();
            int step = Math.min(max, total - given);
            copy.setCount(step);
            if (!sp.getInventory().add(copy)) {
                sp.drop(copy, false);
            }
            given += step;
        }
        return given;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

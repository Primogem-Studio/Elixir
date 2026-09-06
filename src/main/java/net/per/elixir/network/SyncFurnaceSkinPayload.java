package net.per.elixir.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.per.elixir.registry.data.FurnaceVisual;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.per.elixir.Elixir.MOD_ID;

public record SyncFurnaceSkinPayload(BlockPos core, FurnaceVisual visual) implements CustomPacketPayload {
    public static final Type<SyncFurnaceSkinPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sync_furnace_skin"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFurnaceSkinPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, SyncFurnaceSkinPayload>() {
        @Override
        public SyncFurnaceSkinPayload decode(RegistryFriendlyByteBuf buf) {
            var core = BlockPos.STREAM_CODEC.decode(buf);
            if (!buf.readBoolean()) return new SyncFurnaceSkinPayload(core, null);
            return new SyncFurnaceSkinPayload(core, new FurnaceVisual(
                    readRl(buf), readRl(buf), readRl(buf), readRl(buf), readRl(buf),
                    buf.readBoolean() ? Optional.of(buf.readInt()) : Optional.empty(),
                    Map.of(), false, 0, List.of()));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SyncFurnaceSkinPayload value) {
            BlockPos.STREAM_CODEC.encode(buf, value.core);
            var visual = value.visual;
            if (visual == null) {
                buf.writeBoolean(false);
                return;
            }
            buf.writeBoolean(true);
            writeRl(buf, visual.model());
            writeRl(buf, visual.texture());
            writeRl(buf, visual.coverModel());
            writeRl(buf, visual.coverTexture());
            writeRl(buf, visual.activeTexture());
            buf.writeBoolean(visual.activeColor().isPresent());
            visual.activeColor().ifPresent(buf::writeInt);
        }
    };

    private static void writeRl(RegistryFriendlyByteBuf buf, Optional<ResourceLocation> rl) {
        buf.writeBoolean(rl.isPresent());
        rl.ifPresent(l -> buf.writeUtf(l.toString()));
    }

    private static Optional<ResourceLocation> readRl(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(ResourceLocation.tryParse(buf.readUtf())) : Optional.empty();
    }

    public static void handle(SyncFurnaceSkinPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                ClientPayloadHooks.syncSkin.sync(payload.core(), payload.visual());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

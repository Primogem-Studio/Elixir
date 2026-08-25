package net.per.elixir.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.per.elixir.ElixirConfig;

import static net.per.elixir.Elixir.MOD_ID;

public record SyncFurnaceConfigPayload(
        int maxSize,
        int slotsBase,
        int slotsGain,
        int slotsCap,
        int lightLevel
) implements CustomPacketPayload {
    public static final Type<SyncFurnaceConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sync_furnace_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFurnaceConfigPayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    (p, buf) -> {
                        buf.writeVarInt(p.maxSize);
                        buf.writeVarInt(p.slotsBase);
                        buf.writeVarInt(p.slotsGain);
                        buf.writeVarInt(p.slotsCap);
                        buf.writeVarInt(p.lightLevel);
                    },
                    buf -> new SyncFurnaceConfigPayload(
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt()
                    )
            );

    public static void handle(SyncFurnaceConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ElixirConfig.maxFurnaceSize = Math.clamp(payload.maxSize, 3, 31);
            ElixirConfig.multifurnaceSlotsBase = Math.max(1, payload.slotsBase);
            ElixirConfig.multifurnaceSlotsGain = Math.max(1, payload.slotsGain);
            ElixirConfig.multifurnaceSlotsCap = Math.max(ElixirConfig.multifurnaceSlotsBase, payload.slotsCap);
            ElixirConfig.multifurnaceLightLevel = Math.clamp(payload.lightLevel, 0, 15);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

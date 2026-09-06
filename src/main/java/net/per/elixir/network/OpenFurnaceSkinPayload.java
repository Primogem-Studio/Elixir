package net.per.elixir.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.per.elixir.client.FurnaceSkinScreen;

import static net.per.elixir.Elixir.MOD_ID;

public record OpenFurnaceSkinPayload(int size, BlockPos core) implements CustomPacketPayload {
    public static final Type<OpenFurnaceSkinPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "open_furnace_skin"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFurnaceSkinPayload> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT,
            OpenFurnaceSkinPayload::size,
            BlockPos.STREAM_CODEC,
            OpenFurnaceSkinPayload::core,
            OpenFurnaceSkinPayload::new
    );

    public static void handle(OpenFurnaceSkinPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isClientbound()) return;
            var mc = Minecraft.getInstance();
            if (mc.player == null) return;
            mc.setScreen(new FurnaceSkinScreen(payload.size(), payload.core()));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

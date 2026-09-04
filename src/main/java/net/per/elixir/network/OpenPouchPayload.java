package net.per.elixir.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.per.elixir.data.DanPouchMenu;
import net.per.elixir.item.DanPouchItem;

import static net.per.elixir.Elixir.MOD_ID;

public record OpenPouchPayload() implements CustomPacketPayload {
    public static final Type<OpenPouchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "open_pouch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPouchPayload> STREAM_CODEC = StreamCodec.unit(new OpenPouchPayload());

    public static void handle(OpenPouchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer sp)) return;
            var stack = sp.getMainHandItem();
            if (stack.getItem() instanceof DanPouchItem) DanPouchMenu.open(sp, stack);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package net.per.elixir.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.per.elixir.data.DanPouchComponent;
import net.per.elixir.item.DanPouchItem;

import static net.per.elixir.Elixir.MOD_ID;

public record SelectPillPayload(int selected) implements CustomPacketPayload {
    public static final Type<SelectPillPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "select_pill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectPillPayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    (p, buf) -> buf.writeVarInt(p.selected),
                    buf -> new SelectPillPayload(buf.readVarInt())
            );

    public static void handle(SelectPillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer sp)) return;
            var stack = sp.getMainHandItem();
            if (stack.getItem() instanceof DanPouchItem) {
                DanPouchItem.setSelected(stack, Math.clamp(payload.selected, -1, DanPouchComponent.SIZE - 1));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

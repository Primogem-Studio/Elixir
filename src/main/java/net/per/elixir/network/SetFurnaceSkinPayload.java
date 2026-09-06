package net.per.elixir.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.per.elixir.block.entity.LargeFurnaceBlockEntity;
import net.per.elixir.registry.data.FurnaceVisual;
import net.per.elixir.util.MultiFurnaceStructure;

import static net.per.elixir.Elixir.MOD_ID;

public record SetFurnaceSkinPayload(BlockPos core, boolean pin, FurnaceVisual visual) implements CustomPacketPayload {
    public static final Type<SetFurnaceSkinPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "set_furnace_skin"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetFurnaceSkinPayload> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, SetFurnaceSkinPayload>() {
        @Override
        public SetFurnaceSkinPayload decode(RegistryFriendlyByteBuf buf) {
            return read(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SetFurnaceSkinPayload value) {
            write(buf, value);
        }
    };

    private static void write(RegistryFriendlyByteBuf buf, SetFurnaceSkinPayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.core);
        buf.writeBoolean(payload.pin);
        CompoundTag tag = null;
        if (payload.pin && payload.visual != null) {
            tag = (CompoundTag) FurnaceVisual.CODEC.encodeStart(NbtOps.INSTANCE, payload.visual).result().orElse(null);
        }
        buf.writeNbt(tag);
    }

    private static SetFurnaceSkinPayload read(RegistryFriendlyByteBuf buf) {
        var core = BlockPos.STREAM_CODEC.decode(buf);
        boolean pin = buf.readBoolean();
        FurnaceVisual visual = null;
        var tag = buf.readNbt();
        if (tag != null) {
            visual = FurnaceVisual.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
        }
        return new SetFurnaceSkinPayload(core, pin, visual);
    }

    public static void handle(SetFurnaceSkinPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer sp)) return;
            var level = sp.serverLevel();
            var core = payload.core;
            var center = MultiFurnaceStructure.findCore(level, core);
            if (center == null || !center.equals(core)) return;
            if (!(level.getBlockEntity(core) instanceof LargeFurnaceBlockEntity be)) return;
            if (!sp.canInteractWithBlock(core, 4.0D + be.size())) return;
            if (payload.pin) {
                if (payload.visual == null) return;
                be.setPinnedVisual(payload.visual.flattened());
                level.playSound(null, core, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.6f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                        core.getX() + 0.5, core.getY() + 0.5, core.getZ() + 0.5,
                        40, 1.6, 1.6, 1.6, 0.02);
                sp.displayClientMessage(Component.translatable("message.elixir.seal.pinned"), true);
            } else {
                be.clearPinnedVisual();
                level.playSound(null, core, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                sp.displayClientMessage(Component.translatable("message.elixir.seal.unpinned"), true);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package net.per.elixir.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.per.elixir.ElixirConfig;

import static net.per.elixir.Elixir.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class ElixirNetwork {
    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(SyncFurnaceConfigPayload.TYPE, SyncFurnaceConfigPayload.STREAM_CODEC, SyncFurnaceConfigPayload::handle);
    }

    public static void syncFurnaceConfigTo(ServerPlayer player) {
        if (!player.server.isDedicatedServer()) return;
        PacketDistributor.sendToPlayer(player, new SyncFurnaceConfigPayload(
                ElixirConfig.maxFurnaceSize,
                ElixirConfig.multifurnaceSlotsBase,
                ElixirConfig.multifurnaceSlotsGain,
                ElixirConfig.multifurnaceSlotsCap,
                ElixirConfig.multifurnaceLightLevel
        ));
    }
}

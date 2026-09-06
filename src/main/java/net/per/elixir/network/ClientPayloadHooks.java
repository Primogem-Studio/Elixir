package net.per.elixir.network;

import net.minecraft.core.BlockPos;
import net.per.elixir.registry.data.FurnaceVisual;

public final class ClientPayloadHooks {
    private ClientPayloadHooks() {
    }

    @FunctionalInterface
    public interface SkinScreenOpener {
        void open(int size, BlockPos core);
    }

    @FunctionalInterface
    public interface SkinSyncer {
        void sync(BlockPos core, FurnaceVisual visual);
    }

    public static SkinScreenOpener openSkinScreen = (size, core) -> {
    };
    public static SkinSyncer syncSkin = (core, visual) -> {
    };
    public static Runnable openTdpScreen = () -> {
    };
}

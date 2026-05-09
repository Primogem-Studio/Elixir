package net.per.elixir.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityHelper {
    public interface Ticker<T extends BlockEntity> {
        void tick(T be, Level level, BlockPos pos, BlockState state);
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> createTicker(Ticker<T> ticker) {
        return (level, pos, state, blockEntity) -> ticker.tick(blockEntity, level, pos, state);
    }
}

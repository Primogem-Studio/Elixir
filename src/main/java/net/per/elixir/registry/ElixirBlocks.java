package net.per.elixir.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.per.elixir.block.AlchemicalVesselBlock;
import net.per.elixir.block.ElixirFurnaceBlock;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredBlock<ElixirFurnaceBlock> elixir_furnace = BLOCKS.register("elixir_furnace", ElixirFurnaceBlock::new);
    public static final DeferredBlock<AlchemicalVesselBlock> alchemical_vessel = BLOCKS.register("alchemical_vessel", AlchemicalVesselBlock::new);
    public static final DeferredBlock<Block> elixir_furnace_brick = BLOCKS.register("elixir_furnace_brick", () -> new Block(BlockBehaviour.Properties.of().strength(2f, 2.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> elixir_furnace_cover = BLOCKS.register("elixir_furnace_cover", () -> new Block(BlockBehaviour.Properties.of()) {
        private static final VoxelShape SHAPE = Shapes.box(0.2, -0.2, 0.2, 0.8, 0.5, 0.8);

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    });
}

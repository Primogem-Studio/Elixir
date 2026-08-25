package net.per.elixir.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.per.elixir.ElixirConfig;
import net.per.elixir.block.AlchemicalVesselBlock;
import net.per.elixir.block.ElixirFurnaceBlock;
import net.per.elixir.block.ElixirFurnaceBrickBlock;
import net.per.elixir.block.ElixirFurnaceCoreBlock;

import java.util.List;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredBlock<ElixirFurnaceBlock> elixir_furnace = BLOCKS.register("elixir_furnace", ElixirFurnaceBlock::new);
    public static final DeferredBlock<AlchemicalVesselBlock> alchemical_vessel = BLOCKS.register("alchemical_vessel", AlchemicalVesselBlock::new);
    public static final DeferredBlock<ElixirFurnaceBrickBlock> elixir_furnace_brick = BLOCKS.register("elixir_furnace_brick", () -> new ElixirFurnaceBrickBlock(BlockBehaviour.Properties.of().strength(2f, 2.0f).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion().lightLevel(state -> state.getValue(ElixirFurnaceBrickBlock.LIT) ? ElixirConfig.multifurnaceLightLevel : 0)));
    public static final DeferredBlock<ElixirFurnaceCoreBlock> elixir_furnace_core = BLOCKS.register("elixir_furnace_core", () -> new ElixirFurnaceCoreBlock());
    public static final DeferredBlock<Block> elixir_furnace_cover = BLOCKS.register("elixir_furnace_cover", () -> new Block(BlockBehaviour.Properties.of()) {
        private static final VoxelShape SHAPE = Shapes.box(0.2, -0.2, 0.2, 0.8, 0.5, 0.8);

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
            tooltipComponents.add(Component.translatable("item.elixir.elixir_furnace_cover.usage.1"));
            tooltipComponents.add(Component.translatable("item.elixir.elixir_furnace_cover.usage.2"));
        }
    });
}

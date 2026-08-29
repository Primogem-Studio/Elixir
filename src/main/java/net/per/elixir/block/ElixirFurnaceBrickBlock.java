package net.per.elixir.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.util.MultiFurnaceStructure;

import java.util.List;

public class ElixirFurnaceBrickBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public ElixirFurnaceBrickBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        registerDefaultState(defaultBlockState().setValue(FORMED, false).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
        builder.add(LIT);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return MultiFurnaceStructure.handleUseItemOn(level, pos, player, stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return MultiFurnaceStructure.handleUseWithoutItem(level, pos, player, state.getValue(FORMED));
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        if (!state.getValue(FORMED)) return null;
        var core = MultiFurnaceStructure.findCore(level, pos);
        if (core == null) return null;
        return level.getBlockEntity(core) instanceof MenuProvider mp ? mp : null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && state.getValue(FORMED) && !state.is(newState.getBlock())) {
            var core = MultiFurnaceStructure.findCoreNear(level, pos);
            if (core != null) MultiFurnaceStructure.dissolve(level, core, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        if (state.getValue(FORMED)) {
            level.levelEvent(player, 2001, pos, Block.getId(ElixirBlocks.elixir_furnace_brick.get().defaultBlockState()));
            return;
        }
        super.spawnDestroyParticles(level, player, pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.elixir.elixir_furnace_brick.usage.1"));
        tooltipComponents.add(Component.translatable("item.elixir.elixir_furnace_brick.usage.2"));
        tooltipComponents.add(Component.translatable("item.elixir.elixir_furnace_brick.usage.3"));
    }
}

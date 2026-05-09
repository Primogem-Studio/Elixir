package net.per.elixir.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.per.elixir.block.entity.ElixirFurnaceBlockEntity;
import net.per.elixir.registry.ElixirItems;
import net.per.elixir.util.BlockEntityHelper;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
import static net.per.elixir.registry.ElixirDataAttachments.ELIXIR_EXP;

public class ElixirFurnaceBlock extends BaseEntityBlock {
    private static final MapCodec<ElixirFurnaceBlock> CODEC = simpleCodec(ElixirFurnaceBlock::new);
    private static final VoxelShape SHAPE = Shapes.box(0.1, 0, 0.1, 0.9, 0.9, 0.9);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private ElixirFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    public ElixirFurnaceBlock() {
        this(Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5f, 6.0f).lightLevel(state -> state.getValue(ACTIVE) ? 15 : 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
        builder.add(ACTIVE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof ElixirFurnaceBlockEntity be) {
                if (stack.is(Items.FLINT_AND_STEEL)) {
                    if (be.start(level, player)) {
                        level.setBlockAndUpdate(pos, state.setValue(ACTIVE, true));
                        player.setData(ELIXIR_EXP, player.getData(ELIXIR_EXP) + Math.min(level.random.nextFloat(), 0.05f));
                        return ItemInteractionResult.SUCCESS;
                    }
                    player.sendSystemMessage(Component.translatable("message.elixir_furnace.failed").withStyle(ChatFormatting.DARK_RED));
                    return ItemInteractionResult.SUCCESS;
                } else if (stack.is(ElixirItems.handheld_fan)) {
                    if (be.started()) be.temperature += level.random.nextInt(1, 50);
                    else {
                        be.temperature -= 3;
                        level.sendBlockUpdated(pos, state, state, UPDATE_CLIENTS);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ElixirFurnaceBlockEntity.Type, BlockEntityHelper.createTicker(ElixirFurnaceBlockEntity::tick));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElixirFurnaceBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}

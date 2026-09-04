package net.per.elixir.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.per.elixir.data.ElixirFurnaceMenu;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.util.ElixirHelper;

import static net.per.elixir.block.ElixirFurnaceBlock.ACTIVE;
import static net.per.elixir.ElixirConfig.pharmaLimited;

public class ElixirFurnaceBlockEntity extends AbstractAlchemyFurnaceBlockEntity {
    @SuppressWarnings("DataFlowIssue")
    public static final BlockEntityType<ElixirFurnaceBlockEntity> Type = BlockEntityType.Builder.of(ElixirFurnaceBlockEntity::new, ElixirBlocks.elixir_furnace.get()).build(null);

    public ElixirFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Type, pos, state);
        this.items = NonNullList.withSize(6, ItemStack.EMPTY);
    }

    @Override
    protected int containerSize() {
        return 6;
    }

    @Override
    protected int materialSlotCount() {
        return 4;
    }

    @Override
    protected int outputSlot() {
        return 4;
    }

    @Override
    protected int formulaSlot() {
        return 5;
    }

    @Override
    protected double coolRate() {
        return 1.5;
    }

    @Override
    protected double extraStability(Level level, BlockPos pos) {
        return calcStability(level, pos);
    }

    @Override
    public boolean isCovered(Level level) {
        return level.getBlockState(worldPosition.above()).is(ElixirBlocks.elixir_furnace_cover);
    }

    @Override
    protected double explodeY(BlockPos pos) {
        return pos.getY() + 1;
    }

    @Override
    protected float explodeRadius() {
        return 2;
    }

    @Override
    protected void applySizeAdjustments() {
        pharmaLimit = pharmaLimited;
    }

    @Override
    protected void setActiveVisual(Level level, BlockPos pos, BlockState state, boolean active) {
        if (state.getValue(ACTIVE) != active) {
            level.setBlockAndUpdate(pos, state.setValue(ACTIVE, active));
        }
    }

    public static double calcStability(Level level, BlockPos pos) {
        var point = new BlockPos(pos.getX() - 2, pos.getY() + 2, pos.getZ() - 2);
        var result = 0.0;
        for (int i = 0; i < 4; i++) {
            var p1 = point;
            for (int j = 0; j < 4; j++) {
                var p2 = p1;
                for (int k = 0; k < 4; k++) {
                    var state = level.getBlockState(p2);
                    if (state.is(ElixirBlocks.elixir_furnace_brick)) result += 5;
                    p2 = p2.south();
                }
                p1 = p1.east();
            }
            point = point.below();
        }
        return result;
    }

    @Override
    protected Component getDefaultName() {
        return Component.empty();
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ElixirFurnaceMenu(containerId, inventory, this, ContainerLevelAccess.create(level, getBlockPos()));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (started) return false;
        if (slot < materialSlotCount()) return ElixirHelper.hasMaterial(stack.getItem());
        return slot == formulaSlot();
    }
}

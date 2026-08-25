package net.per.elixir.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.per.elixir.block.ElixirFurnaceBrickBlock;
import net.per.elixir.data.LargeFurnaceMenu;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.registry.ElixirDataComponents;
import net.per.elixir.util.MultiFurnaceStructure;

import static net.per.elixir.ElixirConfig.*;

public class LargeFurnaceBlockEntity extends AbstractAlchemyFurnaceBlockEntity {
    @SuppressWarnings("DataFlowIssue")
    public static final BlockEntityType<LargeFurnaceBlockEntity> Type = BlockEntityType.Builder.of(LargeFurnaceBlockEntity::new, ElixirBlocks.elixir_furnace_core.get()).build(null);

    private int size = 3;
    private Direction facing = Direction.NORTH;
    public boolean disposed;

    public LargeFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Type, pos, state);
        this.items = NonNullList.withSize(containerSizeFor(3), ItemStack.EMPTY);
    }

    public void init(int size, Direction facing) {
        this.size = size;
        this.facing = facing;
        this.items = NonNullList.withSize(containerSizeFor(size), ItemStack.EMPTY);
        setChanged();
    }

    public int size() {
        return size;
    }

    public Direction facing() {
        return facing;
    }

    public int materialSlots() {
        return materialSlotsFor(size);
    }

    public static int materialSlotsFor(int size) {
        int d = size - 3;
        int slots = multifurnaceSlotsBase + multifurnaceSlotsGain * d;
        return Math.min(slots, Math.max(multifurnaceSlotsCap, multifurnaceSlotsBase));
    }

    public static int containerSizeFor(int size) {
        return materialSlotsFor(size) + 2;
    }

    @Override
    protected int containerSize() {
        return containerSizeFor(size);
    }

    @Override
    protected int materialSlotCount() {
        return materialSlotsFor(size);
    }

    @Override
    protected int outputSlot() {
        return materialSlotsFor(size);
    }

    @Override
    protected int formulaSlot() {
        return materialSlotsFor(size) + 1;
    }

    @Override
    protected double coolRate() {
        double rate = multifurnaceCoolBase * Math.pow(multifurnaceCoolFactor, size - 3);
        if (size <= 3) rate *= 2;
        return rate;
    }

    @Override
    protected double extraStability(Level level, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isCovered(Level level) {
        return true;
    }

    @Override
    protected double explodeY(BlockPos pos) {
        return pos.getY() + 0.5;
    }

    @Override
    protected float explodeRadius() {
        return 2f + size / 2f;
    }

    @Override
    protected void applySizeAdjustments() {
        pharmaLimit = pharmaLimited + (size - 3) * multifurnacePharmaGain;
        stability -= (size - 3) * multifurnaceStabilityPenalty;
    }

    @Override
    protected void setActiveVisual(Level level, BlockPos pos, BlockState state, boolean active) {
        if (level.isClientSide) return;
        int n = size;
        int half = (n - 1) / 2;
        var min = pos.offset(-half, -half, -half);
        var brick = ElixirBlocks.elixir_furnace_brick.get();
        for (var p : new BlockPos[]{
                min.offset(n - 1, half, half),
                min.offset(0, half, half),
                min.offset(half, half, n - 1),
                min.offset(half, half, 0)}) {
            var s = level.getBlockState(p);
            if (s.is(brick) && s.getValue(ElixirFurnaceBrickBlock.FORMED)
                    && s.getValue(ElixirFurnaceBrickBlock.LIT) != active) {
                level.setBlockAndUpdate(p, s.setValue(ElixirFurnaceBrickBlock.LIT, active));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("size", size);
        tag.putInt("facing", facing.get2DDataValue());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        size = tag.getInt("size");
        if (size < 3 || size > maxFurnaceSize) size = 3;
        facing = Direction.from2DDataValue(tag.getInt("facing"));
        super.loadAdditional(tag, provider);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.putInt("size", size);
        tag.putInt("facing", facing.get2DDataValue());
        return tag;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.elixir.large_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new LargeFurnaceMenu(containerId, inventory, this, ContainerLevelAccess.create(level, getBlockPos()), size);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < materialSlots()) return true;
        return slot == materialSlots() + 1 && stack.has(ElixirDataComponents.AlchemicalFormula);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!disposed && level != null && !level.isClientSide
                && level.getServer() != null && level.getServer().isRunning()) {
            MultiFurnaceStructure.dissolve(level, this, worldPosition);
        }
    }
}

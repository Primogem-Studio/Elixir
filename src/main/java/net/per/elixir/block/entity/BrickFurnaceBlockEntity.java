package net.per.elixir.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.util.MultiFurnaceStructure;
import org.jetbrains.annotations.Nullable;

public class BrickFurnaceBlockEntity extends BlockEntity implements Container, MenuProvider {
    @SuppressWarnings("DataFlowIssue")
    public static final BlockEntityType<BrickFurnaceBlockEntity> Type = BlockEntityType.Builder
            .of(BrickFurnaceBlockEntity::new, ElixirBlocks.elixir_furnace_brick.get()).build(null);

    public BrickFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Type, pos, state);
    }

    @Nullable
    private LargeFurnaceBlockEntity core() {
        if (level == null) return null;
        var core = MultiFurnaceStructure.findCore(level, worldPosition);
        return core != null && level.getBlockEntity(core) instanceof LargeFurnaceBlockEntity lbe ? lbe : null;
    }

    @Override
    public int getContainerSize() {
        var c = core();
        return c != null ? c.getContainerSize() : 0;
    }

    @Override
    public boolean isEmpty() {
        var c = core();
        return c == null || c.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        var c = core();
        return c != null ? c.getItem(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        var c = core();
        return c != null ? c.removeItem(index, count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        var c = core();
        return c != null ? c.removeItemNoUpdate(index) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        var c = core();
        if (c != null) c.setItem(index, stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        var c = core();
        if (c != null) c.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        var c = core();
        return c != null && c.stillValid(player);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        var c = core();
        return c != null && c.canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItem(Container target, int index, ItemStack stack) {
        var c = core();
        return c != null && c.canTakeItem(target, index, stack);
    }

    @Override
    public void clearContent() {
        var c = core();
        if (c != null) c.clearContent();
    }

    @Override
    public Component getDisplayName() {
        var c = core();
        return c != null ? c.getDisplayName() : Component.translatable("container.elixir.large_furnace");
    }

    @Nullable
    public IItemHandler itemHandler() {
        var c = core();
        return c != null ? c.itemHandler() : null;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        var c = core();
        return c != null ? c.createMenu(containerId, inventory, player) : null;
    }
}

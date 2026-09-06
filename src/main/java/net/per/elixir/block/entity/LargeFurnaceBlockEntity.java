package net.per.elixir.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.per.elixir.block.ElixirFurnaceBrickBlock;
import net.per.elixir.data.LargeFurnaceMenu;
import net.per.elixir.network.SyncFurnaceSkinPayload;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.registry.data.FurnaceVisual;
import net.per.elixir.util.ElixirHelper;
import net.per.elixir.util.MultiFurnaceStructure;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.per.elixir.ElixirConfig.*;

public class LargeFurnaceBlockEntity extends AbstractAlchemyFurnaceBlockEntity {
    @SuppressWarnings("DataFlowIssue")
    public static final BlockEntityType<LargeFurnaceBlockEntity> Type = BlockEntityType.Builder.of(LargeFurnaceBlockEntity::new, ElixirBlocks.elixir_furnace_core.get()).build(null);

    private int size = 3;
    private Direction facing = Direction.NORTH;
    private FurnaceVisual pinnedVisual;
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

    public FurnaceVisual pinnedVisual() {
        return pinnedVisual;
    }

    public boolean isVisualPinned() {
        return pinnedVisual != null;
    }

    public FurnaceVisual currentVisual(Level level) {
        if (pinnedVisual != null) return pinnedVisual;
        var visual = FurnaceVisual.getDefault(level);
        return visual == null ? null : visual.select(size, worldPosition.asLong());
    }

    public void setPinnedVisual(FurnaceVisual visual) {
        pinnedVisual = visual == null ? null : visual.flattened();
        syncVisualToClients();
    }

    public void clearPinnedVisual() {
        setPinnedVisual(null);
    }

    public void acceptPinnedVisualClient(FurnaceVisual visual) {
        pinnedVisual = visual == null ? null : visual.flattened();
    }

    private void syncVisualToClients() {
        setChanged();
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(worldPosition),
                    new SyncFurnaceSkinPayload(worldPosition, pinnedVisual));
        }
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
        writePinnedVisual(tag, pinnedVisual);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        size = tag.getInt("size");
        if (size < 3 || size > maxFurnaceSize) size = 3;
        facing = Direction.from2DDataValue(tag.getInt("facing"));
        pinnedVisual = readPinnedVisual(tag);
        super.loadAdditional(tag, provider);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.putInt("size", size);
        tag.putInt("facing", facing.get2DDataValue());
        writePinnedVisual(tag, pinnedVisual);
        return tag;
    }

    private static void writePinnedVisual(CompoundTag tag, FurnaceVisual visual) {
        if (visual == null) return;
        var pin = new CompoundTag();
        visual.model().ifPresent(rl -> pin.putString("model", rl.toString()));
        visual.texture().ifPresent(rl -> pin.putString("texture", rl.toString()));
        visual.coverModel().ifPresent(rl -> pin.putString("cover_model", rl.toString()));
        visual.coverTexture().ifPresent(rl -> pin.putString("cover_texture", rl.toString()));
        visual.activeTexture().ifPresent(rl -> pin.putString("active_texture", rl.toString()));
        visual.activeColor().ifPresent(c -> pin.putInt("active_color", c));
        tag.put("pinned_visual", pin);
    }

    private static FurnaceVisual readPinnedVisual(CompoundTag tag) {
        if (!tag.contains("pinned_visual")) return null;
        var pin = tag.getCompound("pinned_visual");
        return new FurnaceVisual(
                optRl(pin, "model"),
                optRl(pin, "texture"),
                optRl(pin, "cover_model"),
                optRl(pin, "cover_texture"),
                optRl(pin, "active_texture"),
                pin.contains("active_color") ? Optional.of(pin.getInt("active_color")) : Optional.empty(),
                Map.of(), false, 0, List.of());
    }

    private static Optional<ResourceLocation> optRl(CompoundTag tag, String key) {
        return tag.contains(key) ? Optional.ofNullable(ResourceLocation.tryParse(tag.getString(key))) : Optional.empty();
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
        if (started) return false;
        if (slot < materialSlots()) return ElixirHelper.hasMaterial(stack.getItem());
        return slot == materialSlots() + 1;
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

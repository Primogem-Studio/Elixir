package net.per.elixir.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.per.elixir.ElixirConfig;
import net.per.elixir.block.ElixirFurnaceBrickBlock;
import net.per.elixir.block.entity.BrickFurnaceBlockEntity;
import net.per.elixir.block.entity.LargeFurnaceBlockEntity;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.registry.ElixirItems;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class MultiFurnaceStructure {
    public static final int MIN_SIZE = 3;

    private record Bounds(BlockPos min, int size) {
    }

    private MultiFurnaceStructure() {
    }

    public static int maxSize() {
        return ElixirConfig.maxFurnaceSize;
    }

    private static Bounds scan(Level level, BlockPos pos, Predicate<BlockPos> part) {
        var min = pos;
        while (part.test(min.west())) min = min.west();
        while (part.test(min.below())) min = min.below();
        while (part.test(min.north())) min = min.north();
        int sx = 0, sy = 0, sz = 0;
        for (var p = min; part.test(p); p = p.east()) sx++;
        for (var p = min; part.test(p); p = p.above()) sy++;
        for (var p = min; part.test(p); p = p.south()) sz++;
        if (sx != sy || sy != sz) return null;
        return new Bounds(min, sx);
    }

    public static boolean tryForm(Level level, BlockPos pos, Direction facing) {
        if (level.isClientSide) return false;
        if (!isUnformedBrick(level, pos)) return false;
        var b = scan(level, pos, p -> isUnformedBrick(level, p));
        if (b == null) return false;
        int n = b.size();
        if (n < MIN_SIZE || n > maxSize()) return false;
        var min = b.min();
        for (int x = 0; x < n; x++)
            for (int y = 0; y < n; y++)
                for (int z = 0; z < n; z++)
                    if (!isUnformedBrick(level, min.offset(x, y, z))) return false;

        BlockPos center = centerOf(min, n);
        level.setBlockAndUpdate(center, ElixirBlocks.elixir_furnace_core.get().defaultBlockState());
        if (!(level.getBlockEntity(center) instanceof LargeFurnaceBlockEntity lbe)) {
            level.setBlockAndUpdate(center, ElixirBlocks.elixir_furnace_brick.get().defaultBlockState());
            return false;
        }
        lbe.init(n, facing);
        var formed = ElixirBlocks.elixir_furnace_brick.get().defaultBlockState()
                .setValue(ElixirFurnaceBrickBlock.FORMED, true);
        for (int x = 0; x < n; x++)
            for (int y = 0; y < n; y++)
                for (int z = 0; z < n; z++) {
                    var p = min.offset(x, y, z);
                    if (p.equals(center)) continue;
                    level.setBlockAndUpdate(p, formed);
                    if (x == 0 || x == n - 1 || y == 0 || y == n - 1 || z == 0 || z == n - 1) {
                        var be = new BrickFurnaceBlockEntity(p, formed);
                        level.setBlockEntity(be);
                        be.setChanged();
                    }
                }
        level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    public static BlockPos centerOf(BlockPos min, int n) {
        int half = (n - 1) / 2;
        return min.offset(half, half, half);
    }

    public static BlockPos findCore(Level level, BlockPos pos) {
        if (!isFormedPart(level, pos)) return null;
        var b = scan(level, pos, p -> isFormedPart(level, p));
        if (b == null) return null;
        int n = b.size();
        if (n < MIN_SIZE || n > maxSize()) return null;
        var center = centerOf(b.min(), n);
        if (level.getBlockState(center).is(ElixirBlocks.elixir_furnace_core.get())) return center;
        return null;
    }

    public static BlockPos findCoreNear(Level level, BlockPos pos) {
        for (var dir : Direction.values()) {
            var core = findCore(level, pos.relative(dir));
            if (core != null) return core;
        }
        return null;
    }

    public static void dissolve(Level level, BlockPos corePos, BlockPos dropPos) {
        if (level.isClientSide) return;
        if (level.getBlockEntity(corePos) instanceof LargeFurnaceBlockEntity lbe) {
            dissolveCore(level, lbe, dropPos);
            return;
        }
        onPartRemoved(level, corePos, dropPos);
    }

    public static void dissolve(Level level, LargeFurnaceBlockEntity lbe, BlockPos dropPos) {
        if (level.isClientSide) return;
        dissolveCore(level, lbe, dropPos);
    }

    public static void onPartRemoved(Level level, BlockPos brokenPos, BlockPos dropPos) {
        if (level.isClientSide) return;
        var corePos = findCoreNear(level, brokenPos);
        if (corePos != null && level.getBlockEntity(corePos) instanceof LargeFurnaceBlockEntity lbe) {
            dissolveCore(level, lbe, dropPos);
            return;
        }
        var region = floodFormed(level, brokenPos);
        if (region.isEmpty()) return;
        for (var p : region) {
            if (level.getBlockState(p).is(ElixirBlocks.elixir_furnace_core.get())
                    && level.getBlockEntity(p) instanceof LargeFurnaceBlockEntity lbe) {
                dissolveCore(level, lbe, dropPos);
                return;
            }
        }
        dissolveRegion(level, region);
    }

    private static void dissolveCore(Level level, LargeFurnaceBlockEntity lbe, BlockPos dropPos) {
        if (lbe.disposed) return;
        lbe.disposed = true;
        Containers.dropContents(level, dropPos, lbe);
        int n = lbe.size();
        int half = (n - 1) / 2;
        var min = lbe.getBlockPos().offset(-half, -half, -half);
        convertRegion(level, min, n, n, n);
        level.playSound(null, min, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private static void dissolveRegion(Level level, Set<BlockPos> region) {
        BlockPos soundPos = null;
        for (var p : region) {
            if (!isActivePart(level.getBlockState(p))) continue;
            level.setBlock(p, ElixirBlocks.elixir_furnace_brick.get().defaultBlockState(), 2);
            level.removeBlockEntity(p);
            if (soundPos == null) soundPos = p;
        }
        if (soundPos != null)
            level.playSound(null, soundPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private static void convertRegion(Level level, BlockPos min, int sx, int sy, int sz) {
        for (int x = 0; x < sx; x++)
            for (int y = 0; y < sy; y++)
                for (int z = 0; z < sz; z++) {
                    var p = min.offset(x, y, z);
                    if (!isActivePart(level.getBlockState(p))) continue;
                    level.setBlock(p, ElixirBlocks.elixir_furnace_brick.get().defaultBlockState(), 2);
                    level.removeBlockEntity(p);
                }
    }

    private static boolean isActivePart(BlockState state) {
        if (state.is(ElixirBlocks.elixir_furnace_core.get())) return true;
        return state.is(ElixirBlocks.elixir_furnace_brick.get())
                && state.getValue(ElixirFurnaceBrickBlock.FORMED);
    }

    private static Set<BlockPos> floodFormed(Level level, BlockPos brokenPos) {
        var region = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        for (var dir : Direction.values()) {
            var p = brokenPos.relative(dir);
            if (isFormedPart(level, p) && region.add(p)) queue.add(p);
        }
        int cap = maxSize() * maxSize() * maxSize();
        while (!queue.isEmpty() && region.size() < cap) {
            var p = queue.poll();
            for (var dir : Direction.values()) {
                var q = p.relative(dir);
                if (region.size() >= cap) return region;
                if (isFormedPart(level, q) && region.add(q)) queue.add(q);
            }
        }
        return region;
    }

    public static ItemInteractionResult handleUseItemOn(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (stack.is(Items.FLINT_AND_STEEL)) {
            var core = findCore(level, pos);
            if (core != null && level.getBlockEntity(core) instanceof LargeFurnaceBlockEntity be) {
                if (be.start(level, player)) {
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return ItemInteractionResult.SUCCESS;
                }
                player.sendSystemMessage(Component.translatable("message.elixir_furnace.failed"));
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ElixirItems.handheld_fan)) {
            if (!isFormedPart(level, pos)) {
                if (tryForm(level, pos, player.getDirection())) {
                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return ItemInteractionResult.SUCCESS;
                }
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
                player.sendSystemMessage(Component.translatable("message.elixir_multifurnace.invalid"));
                return ItemInteractionResult.SUCCESS;
            }
            var core = findCore(level, pos);
            if (core != null && level.getBlockEntity(core) instanceof LargeFurnaceBlockEntity be) {
                int delta = be.started() ? level.random.nextInt(1, 50) : -3;
                be.temperature += delta;
                if (delta <= 24)
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.0f);
                else
                    level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
                level.sendBlockUpdated(core, level.getBlockState(core), level.getBlockState(core), Block.UPDATE_CLIENTS);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static InteractionResult handleUseWithoutItem(Level level, BlockPos pos, Player player, boolean formed) {
        if (!formed) return InteractionResult.PASS;
        if (player.isSpectator()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof LargeFurnaceBlockEntity be) {
            player.openMenu(be, buf -> buf.writeInt(be.size()));
            level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static boolean isUnformedBrick(Level level, BlockPos pos) {
        var s = level.getBlockState(pos);
        return s.is(ElixirBlocks.elixir_furnace_brick.get()) && !s.getValue(ElixirFurnaceBrickBlock.FORMED);
    }

    public static boolean isFormedPart(Level level, BlockPos pos) {
        var s = level.getBlockState(pos);
        if (s.is(ElixirBlocks.elixir_furnace_core.get())) return true;
        return s.is(ElixirBlocks.elixir_furnace_brick.get()) && s.getValue(ElixirFurnaceBrickBlock.FORMED);
    }
}

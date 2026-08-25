package net.per.elixir.compat.tlm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.per.elixir.block.entity.AbstractAlchemyFurnaceBlockEntity;
import net.per.elixir.block.entity.LargeFurnaceBlockEntity;
import net.per.elixir.data.ElixirFurnaceMenu;
import net.per.elixir.data.LargeFurnaceMenu;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.registry.ElixirDataComponents;
import net.per.elixir.registry.ElixirItems;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.per.elixir.ElixirConfig.maidNegligenceChance;

public class MaidAlchemyTask extends Behavior<EntityMaid> {
    private static final int SEARCH_INTERVAL = 100;
    private static final int VERIFY_INTERVAL = 40;
    private static final int ACTION_INTERVAL = 10;
    private static final double REACH_DISTANCE = 2.0;
    private static final double OCCUPY_DIST = 3.0;
    private static final double MAID_SCAN_INFLATE = 4.0;
    private static final int MIN_SEARCH_RADIUS = 4;
    private static final int MAX_STAND_DIST = 1;
    private static final int STAND_RETRY_INTERVAL = 20;
    private static final int WALK_TIMEOUT = 400;
    private static final int IGNORE_INTERVAL = 300;
    private static final int BUBBLE_INTERVAL = 600;
    private static final float FAN_WEAK_MAX = 24f;
    private static final int SHARE_BONUS = 2;

    private static final String BUBBLE_START = "chat_bubble.elixir.alchemy.start";
    private static final String BUBBLE_COLLECT = "chat_bubble.elixir.alchemy.collect";
    private static final String BUBBLE_BACKPACK_FULL = "chat_bubble.elixir.alchemy.backpack_full";
    private static final String BUBBLE_WAIT_PLAYER = "chat_bubble.elixir.alchemy.wait_player";
    private static final String BUBBLE_FORMULA_MISMATCH = "chat_bubble.elixir.alchemy.formula_mismatch";
    private static final String BUBBLE_EMPTY = "chat_bubble.elixir.alchemy.empty";

    @Nullable
    private BlockPos furnacePos;
    @Nullable
    private BlockPos standPos;
    @Nullable
    private BlockPos pendingPos;
    @Nullable
    private BlockPos ignoredPos;
    private int searchCooldown;
    private int verifyCooldown;
    private int relocateCooldown;
    private int actionCooldown;
    private int standRetryCooldown;
    private int walkTicks;
    private int ignoreCooldown;
    private int bubbleCooldown;
    private boolean waitingForPlayer;
    private long bubbleKey = -1;

    public MaidAlchemyTask() {
        super(ImmutableMap.of());
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        if (this.bubbleKey >= 0) {
            maid.getChatBubbleManager().removeChatBubble(this.bubbleKey);
        }
        this.furnacePos = null;
        this.standPos = null;
        this.pendingPos = null;
        this.ignoredPos = null;
        this.searchCooldown = 0;
        this.verifyCooldown = 0;
        this.relocateCooldown = 0;
        this.actionCooldown = 0;
        this.standRetryCooldown = 0;
        this.walkTicks = 0;
        this.ignoreCooldown = 0;
        this.bubbleCooldown = 0;
        this.waitingForPlayer = false;
        this.bubbleKey = -1;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return maid.getMainHandItem().is(ElixirItems.handheld_fan.get());
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        if (this.bubbleCooldown > 0) {
            this.bubbleCooldown--;
        }
        if (this.furnacePos == null) {
            if (this.ignoreCooldown > 0 && --this.ignoreCooldown <= 0) {
                this.ignoredPos = null;
            }
            if (--this.searchCooldown > 0) {
                return;
            }
            this.searchCooldown = SEARCH_INTERVAL;
            BlockPos found = findFurnace(level, maid);
            if (found != null) {
                this.furnacePos = found;
                this.standPos = findStandPos(level, found, maid);
                this.verifyCooldown = 0;
            }
            return;
        }

        if (--this.verifyCooldown <= 0) {
            this.verifyCooldown = VERIFY_INTERVAL;
            if (!(level.getBlockEntity(this.furnacePos) instanceof AbstractAlchemyFurnaceBlockEntity)
                    || (maid.hasRestriction() && !maid.isWithinRestriction(this.furnacePos))) {
                clearTarget(maid);
                return;
            }
            if (isOccupiedByNearbyMaid(level, maid, this.furnacePos)) {
                forgetFurnace(maid);
                return;
            }
        }

        if (--this.relocateCooldown <= 0) {
            this.relocateCooldown = SEARCH_INTERVAL;
            AbstractAlchemyFurnaceBlockEntity curBe = level.getBlockEntity(this.furnacePos) instanceof AbstractAlchemyFurnaceBlockEntity b ? b : null;
            int curScore = curBe != null ? scoreFurnace(curBe, isLarge(level, this.furnacePos)) : 0;
            boolean curBusy = curBe != null && (curBe.started() || !curBe.getItem(outputSlotOf(curBe)).isEmpty());
            BlockPos better = findFurnace(level, maid);
            if (better != null && !better.equals(this.furnacePos)
                    && level.getBlockEntity(better) instanceof AbstractAlchemyFurnaceBlockEntity betterBe
                    && scoreFurnace(betterBe, isLarge(level, better)) > curScore) {
                if (curBusy) {
                    this.pendingPos = better;
                } else {
                    switchTo(level, maid, better);
                }
            }
        }
        if (this.pendingPos != null) {
            boolean curBusy = level.getBlockEntity(this.furnacePos) instanceof AbstractAlchemyFurnaceBlockEntity cBe
                    && (cBe.started() || !cBe.getItem(outputSlotOf(cBe)).isEmpty());
            if (!curBusy) {
                BlockPos target = this.pendingPos;
                this.pendingPos = null;
                if (level.getBlockEntity(target) instanceof AbstractAlchemyFurnaceBlockEntity pBe && scoreFurnace(pBe, isLarge(level, target)) > 0) {
                    switchTo(level, maid, target);
                }
            }
        }

        if (this.standPos == null) {
            if (--this.standRetryCooldown > 0) {
                return;
            }
            this.standRetryCooldown = STAND_RETRY_INTERVAL;
            this.standPos = findStandPos(level, this.furnacePos, maid);
            if (this.standPos == null) {
                return;
            }
        }

        // 已在互动范围内 → 直接互动，不再依赖精确站位（大炉子站位远、层数多，容易卡在走路分支）
        if (isInInteractRange(level, maid)) {
            this.walkTicks = 0;
            maid.getLookControl().setLookAt(Vec3.atCenterOf(this.furnacePos));
            if (--this.actionCooldown > 0) {
                return;
            }
            this.actionCooldown = ACTION_INTERVAL;
            interact(level, maid);
            return;
        }

        if (this.standPos == null) {
            if (--this.standRetryCooldown > 0) {
                return;
            }
            this.standRetryCooldown = STAND_RETRY_INTERVAL;
            this.standPos = findStandPos(level, this.furnacePos, maid);
            if (this.standPos == null) {
                return;
            }
        }

        WalkTarget currentTarget = maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElse(null);
        boolean targetIsStandPos = currentTarget != null && currentTarget.getTarget().currentBlockPosition().equals(this.standPos);
        if (!targetIsStandPos) {
            BehaviorUtils.setWalkAndLookTargetMemories(maid, this.standPos, 0.7f, 2);
        }
        if (++this.walkTicks > WALK_TIMEOUT) {
            clearTarget(maid);
        }
    }

    /**
     * 女仆是否已在可互动范围内：大型丹炉以炉体方块盒最近点计（站在炉体旁任意一侧即可），
     * 普通丹炉以核心方块中心计。
     */
    private boolean isInInteractRange(ServerLevel level, EntityMaid maid) {
        if (level.getBlockEntity(this.furnacePos) instanceof LargeFurnaceBlockEntity l) {
            int n = l.size();
            int half = (n - 1) / 2;
            double px = Math.clamp(maid.getX(), this.furnacePos.getX() - half, this.furnacePos.getX() + half + 1);
            double py = Math.clamp(maid.getY(), this.furnacePos.getY() - half, this.furnacePos.getY() + half + 1);
            double pz = Math.clamp(maid.getZ(), this.furnacePos.getZ() - half, this.furnacePos.getZ() + half + 1);
            double dx = maid.getX() - px;
            double dy = maid.getY() - py;
            double dz = maid.getZ() - pz;
            return dx * dx + dy * dy + dz * dz <= 9;
        }
        return maid.distanceToSqr(Vec3.atCenterOf(this.furnacePos)) <= 16;
    }

    private void interact(ServerLevel level, EntityMaid maid) {
        if (level.random.nextFloat() < maidNegligenceChance) {
            return;
        }
        if (this.furnacePos == null) {
            return;
        }
        if (!isInInteractRange(level, maid)) {
            return;
        }
        if (!(level.getBlockEntity(this.furnacePos) instanceof AbstractAlchemyFurnaceBlockEntity be)) {
            clearTarget(maid);
            return;
        }
        int outputSlot = outputSlotOf(be);
        if (be.started()) {
            if (be.temperature < be.targetTemp - 5f) {
                float gap = be.targetTemp - be.temperature;
                float gain = Math.min(gap + level.random.nextInt(-5, 16), 55f);
                be.temperature += Math.max(gain, 10f);
                if (gain <= FAN_WEAK_MAX) {
                    level.playSound(null, this.furnacePos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.0f);
                } else {
                    level.playSound(null, this.furnacePos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
                }
                maid.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }
        ItemStack output = be.getItem(outputSlot);
        if (!output.isEmpty()) {
            ItemStack rest = ItemHandlerHelper.insertItemStacked(maid.getAvailableBackpackInv(), output, false);
            be.setItem(outputSlot, rest);
            if (rest.isEmpty()) {
                this.waitingForPlayer = false;
                sayBubble(maid, BUBBLE_COLLECT);
            } else {
                this.waitingForPlayer = false;
                sayBubble(maid, BUBBLE_BACKPACK_FULL);
                forgetFurnace(maid);
            }
            return;
        }
        if (isMenuOpenByOwner(level, maid, be)) {
            return;
        }
        if (!hasMaterial(be)) {
            this.waitingForPlayer = false;
            sayBubble(maid, BUBBLE_EMPTY);
            forgetFurnace(maid);
            return;
        }
        if (hasFormula(be)) {
            if (!be.isFormulaSatisfied()) {
                this.waitingForPlayer = false;
                sayBubble(maid, BUBBLE_FORMULA_MISMATCH);
                forgetFurnace(maid);
                return;
            }
            if (be.start(level, maid)) {
                level.playSound(null, this.furnacePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                this.waitingForPlayer = false;
                sayBubble(maid, BUBBLE_START);
            }
            return;
        }
        if (!this.waitingForPlayer) {
            sayBubble(maid, BUBBLE_WAIT_PLAYER);
            this.waitingForPlayer = true;
        }
    }

    private void sayBubble(EntityMaid maid, String textKey) {
        if (this.bubbleCooldown > 0) {
            return;
        }
        var manager = maid.getChatBubbleManager();
        if (this.bubbleKey >= 0) {
            manager.removeChatBubble(this.bubbleKey);
        }
        this.bubbleKey = manager.addTextChatBubble(textKey);
        this.bubbleCooldown = BUBBLE_INTERVAL;
    }

    private void switchTo(ServerLevel level, EntityMaid maid, BlockPos target) {
        this.furnacePos = target;
        this.standPos = findStandPos(level, target, maid);
        this.verifyCooldown = 0;
        this.walkTicks = 0;
        this.waitingForPlayer = false;
        this.pendingPos = null;
    }

    private void forgetFurnace(EntityMaid maid) {
        this.ignoredPos = this.furnacePos;
        this.ignoreCooldown = IGNORE_INTERVAL;
        clearTarget(maid);
    }

    private boolean isMenuOpenByOwner(ServerLevel level, EntityMaid maid, AbstractAlchemyFurnaceBlockEntity be) {
        if (!(maid.getOwner() instanceof Player owner)) {
            return false;
        }
        if (owner.containerMenu instanceof ElixirFurnaceMenu menu && menu.getSlot(0).container == be) {
            return true;
        }
        return owner.containerMenu instanceof LargeFurnaceMenu menu && menu.getSlot(0).container == be;
    }

    private int scoreFurnace(AbstractAlchemyFurnaceBlockEntity be, boolean large) {
        if (be.started()) {
            return large ? 6 : (be.isPlayerTriggered() ? 4 : 3);
        }
        if (hasFormula(be)) {
            return hasMaterial(be) && be.isFormulaSatisfied() ? 2 : 0;
        }
        return hasMaterial(be) ? 1 : 0;
    }

    private boolean hasMaterial(AbstractAlchemyFurnaceBlockEntity be) {
        for (int i = 0; i < materialCountOf(be); i++) {
            if (!be.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFormula(AbstractAlchemyFurnaceBlockEntity be) {
        ItemStack formula = be.getItem(formulaSlotOf(be));
        return !formula.isEmpty() && formula.has(ElixirDataComponents.AlchemicalFormula);
    }

    private static int materialCountOf(AbstractAlchemyFurnaceBlockEntity be) {
        return be instanceof LargeFurnaceBlockEntity l ? l.materialSlots() : 4;
    }

    private static int outputSlotOf(AbstractAlchemyFurnaceBlockEntity be) {
        return materialCountOf(be);
    }

    private static int formulaSlotOf(AbstractAlchemyFurnaceBlockEntity be) {
        return materialCountOf(be) + 1;
    }

    private static boolean isLarge(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ElixirBlocks.elixir_furnace_core);
    }

    private void clearTarget(EntityMaid maid) {
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.furnacePos = null;
        this.standPos = null;
        this.pendingPos = null;
        this.waitingForPlayer = false;
        this.walkTicks = 0;
    }

    @Nullable
    private BlockPos findFurnace(ServerLevel level, EntityMaid maid) {
        BlockPos center = maid.blockPosition();
        int radius = Math.max((int) (maid.getRestrictRadius() / 2), MIN_SEARCH_RADIUS);
        List<EntityMaid> otherMaids = level.getEntitiesOfClass(EntityMaid.class,
                new AABB(center).inflate(radius + MAID_SCAN_INFLATE),
                m -> m != maid && m.getMainHandItem().is(ElixirItems.handheld_fan.get()));
        List<BlockPos> otherTargets = new ArrayList<>();
        for (EntityMaid other : otherMaids) {
            other.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                    .ifPresent(wt -> otherTargets.add(wt.getTarget().currentBlockPosition()));
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        int bestScore = 0;
        double bestDistSqr = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // 大炉子核心在炉体中心，比地面高 (size-1)/2 格，纵向多扫几层
                for (int y = -4; y <= 4; y++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (pos.equals(this.ignoredPos)) {
                        continue;
                    }
                    if (maid.hasRestriction() && !maid.isWithinRestriction(pos)) {
                        continue;
                    }
                    var st = level.getBlockState(pos);
                    if (!(level.getBlockEntity(pos) instanceof AbstractAlchemyFurnaceBlockEntity be)) {
                        continue;
                    }
                    boolean large;
                    if (st.is(ElixirBlocks.elixir_furnace)) {
                        large = false;
                    } else if (st.is(ElixirBlocks.elixir_furnace_core)) {
                        large = true;
                    } else {
                        continue;
                    }
                    int score = scoreFurnace(be, large);
                    if (score <= 0) {
                        continue;
                    }
                    boolean targeted = isTargetedByOther(pos, otherTargets);
                    if (!large && targeted) {
                        continue;
                    }
                    if (large && targeted) {
                        score += SHARE_BONUS;
                    }
                    double d = pos.distToCenterSqr(maid.getX(), maid.getY(), maid.getZ());
                    if (score > bestScore || (score == bestScore && d < bestDistSqr)) {
                        bestScore = score;
                        bestDistSqr = d;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    private boolean isTargetedByOther(BlockPos furnacePos, List<BlockPos> otherTargets) {
        for (BlockPos target : otherTargets) {
            if (target.closerThan(furnacePos, OCCUPY_DIST)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOccupiedByNearbyMaid(ServerLevel level, EntityMaid maid, BlockPos furnacePos) {
        if (isLarge(level, furnacePos)) {
            return false;
        }
        List<EntityMaid> near = level.getEntitiesOfClass(EntityMaid.class,
                new AABB(furnacePos).inflate(OCCUPY_DIST),
                m -> m != maid && m.getMainHandItem().is(ElixirItems.handheld_fan.get()));
        for (EntityMaid other : near) {
            if (other.blockPosition().closerThan(furnacePos, OCCUPY_DIST)
                    && other.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                    .map(wt -> wt.getTarget().currentBlockPosition().closerThan(furnacePos, OCCUPY_DIST))
                    .orElse(false)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private BlockPos findStandPos(ServerLevel level, BlockPos furnacePos, EntityMaid maid) {
        int half = MAX_STAND_DIST;
        int yMin = furnacePos.getY();
        int yMax = furnacePos.getY();
        if (level.getBlockEntity(furnacePos) instanceof LargeFurnaceBlockEntity l) {
            int n = l.size();
            half = (n - 1) / 2 + 1;
            yMin = furnacePos.getY() - (n - 1) / 2 - 1;
            yMax = furnacePos.getY() + (n - 1) / 2 + 1;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDistSqr = Double.MAX_VALUE;
        for (int y = yMin; y <= yMax; y++) {
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    if (Math.abs(dx) != half && Math.abs(dz) != half) {
                        continue;
                    }
                    pos.set(furnacePos.getX() + dx, y, furnacePos.getZ() + dz);
                    if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                        double d = pos.distToCenterSqr(maid.getX(), maid.getY(), maid.getZ());
                        if (d < bestDistSqr) {
                            bestDistSqr = d;
                            best = pos.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }
}

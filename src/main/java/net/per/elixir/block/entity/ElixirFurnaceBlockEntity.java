package net.per.elixir.block.entity;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.per.elixir.Elixir;
import net.per.elixir.data.AlchemicalFormulaComponent;
import net.per.elixir.data.ElixirComponent;
import net.per.elixir.data.ElixirFurnaceMenu;
import net.per.elixir.item.ElixirItem;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.registry.ElixirDataComponents;
import net.per.elixir.registry.ElixirItems;
import net.per.elixir.registry.ElixirRegistries;
import net.per.elixir.registry.data.Material;
import net.per.elixir.util.ElixirHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static net.per.elixir.Elixir.MOD_ID;
import static net.per.elixir.ElixirConfig.*;
import static net.per.elixir.block.ElixirFurnaceBlock.ACTIVE;
import static net.per.elixir.registry.ElixirDataAttachments.ELIXIR_EXP;

public class ElixirFurnaceBlockEntity extends BaseContainerBlockEntity {
    public static final BlockEntityType<ElixirFurnaceBlockEntity> Type = BlockEntityType.Builder.of(ElixirFurnaceBlockEntity::new, ElixirBlocks.elixir_furnace.get()).build(null);
    private final NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    private boolean started;
    private int progress;
    public float temperature;
    private double stability, tempStability;
    private int explodeProgress, failedProgress;
    private Set<Holder<Material>> main, off;
    private int pharma;
    private boolean empty;
    private Object2IntMap<Holder<Material>> counter;
    private Player trigger;

    public ElixirFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putFloat("temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        temperature = tag.getFloat("temperature");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putFloat("temperature", temperature);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel sl && started) {
            progress++;
            temperature -= 0.5f;
            temperature = Math.clamp(temperature, 0, 500);
            var t = (int) ((pharma / (float) pharmaLimited + 1) / 2f * 500);
            if (temperature < t) {
                if (temperature < t - Math.max(0, extremeTemperatureRange)) {
                    failedProgress++;
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 1, 0, 0, 0, 0.1);
                    if (failedProgress == 140) {
                        started = false;
                        level.setBlockAndUpdate(pos, state.setValue(ACTIVE, false));
                        if (empty) return;
                        failed(level);
                        return;
                    }
                }
                tempStability -= 2.5;
            } else {
                if (temperature > t + extremeTemperatureRange) {
                    explodeProgress++;
                    sl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, Math.min(explodeProgress, 100), 0, 0, 0, 0.1);
                    if (explodeProgress >= 100 + trigger.getData(ELIXIR_EXP))
                        level.explode(null, pos.getX(), pos.getY() + 1, pos.getZ(), 2, Level.ExplosionInteraction.TNT);
                }
                tempStability += 2.5;
            }
            if (progress >= Math.clamp((long) pharma * refineTicks, 200, 1200)) {
                started = false;
                level.setBlockAndUpdate(pos, state.setValue(ACTIVE, false));
                if (empty) return;
                process(level, pos);
            }
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void failed(Level level) {
        var pharma = (int) (this.pharma * (level.random.nextFloat() - 0.5f) * 2 * badElixirCompensation);
        pharma = (int) Math.min(pharma, pharmaLimited * badElixirCompensation);
        items.clear();
        var reg = level.registryAccess().registryOrThrow(ElixirRegistries.MATERIAL);
        var main = new HashSet<Holder<Material>>();
        var off = new HashSet<Holder<Material>>();
        while (off.isEmpty() || main.isEmpty()) {
            reg.getRandom(level.random).ifPresent(m -> {
                if (m.value().main()) main.add(m);
                else off.add(m);
            });
        }
        var elixir = new ItemStack(ElixirItems.elixir.get());
        elixir.set(ElixirDataComponents.Elixir, new ElixirComponent(off.iterator().next(), pharma, List.copyOf(main)));
        elixir.set(DataComponents.ITEM_NAME, Component.translatable("item.elixir.failed").withColor(ElixirItem.getColor(elixir.get(ElixirDataComponents.Elixir))));
        items.set(4, elixir);
        trigger.setData(ELIXIR_EXP, trigger.getData(ELIXIR_EXP) + 1);
    }

    private void process(Level level, BlockPos pos) {
        if (off.isEmpty())
            off.add(level.registryAccess().holderOrThrow(ResourceKey.create(ElixirRegistries.MATERIAL, ResourceLocation.fromNamespaceAndPath(MOD_ID, "off/empty"))));
        var elixir = new ItemStack(ElixirItems.elixir.get());
        var s = (calcStability(level, pos) + stability) * (1 + tempStability / (Math.abs(tempStability) + 50));
        Elixir.LOGGER.debug("[E]稳定性 {} 药理 {} 经验{} ", s, pharma, trigger.getData(ELIXIR_EXP));
        if (!level.getBlockState(pos.above()).is(ElixirBlocks.elixir_furnace_cover)) s /= 2;
        if (s > -pharmaLimited) {
            elixir.set(ElixirDataComponents.Elixir, new ElixirComponent(List.copyOf(off).get(level.random.nextInt(off.size())), (int) (pharma + ((trigger.getData(ELIXIR_EXP) * 0.01) * pharma) + (pharma * (Math.min(s * 0.01, 2)))), List.copyOf(main)));
            items.set(4, elixir);
            outputRecipe();
            trigger.setData(ELIXIR_EXP, trigger.getData(ELIXIR_EXP) + 5);
            return;
        }
        failed(level);
    }

    private void outputRecipe() {
        var it = items.get(5);
        if (it.isEmpty() || it.has(ElixirDataComponents.AlchemicalFormula)) return;
        var main = ImmutableList.<AlchemicalFormulaComponent.Content>builder();
        var off = ImmutableList.<AlchemicalFormulaComponent.Content>builder();
        for (var e : counter.object2IntEntrySet()) {
            var m = e.getKey();
            if (m.value().main()) main.add(new AlchemicalFormulaComponent.Content(m, e.getIntValue()));
            else off.add(new AlchemicalFormulaComponent.Content(m, e.getIntValue()));
        }
        it.set(DataComponents.ITEM_NAME, Component.translatable("item.elixir.alchemical_formula"));
        it.set(ElixirDataComponents.AlchemicalFormula, new AlchemicalFormulaComponent(main.build(), off.build()));
    }

    private void startWithoutRecipe(Level level) {
        main = new HashSet<>();
        off = new HashSet<>();
        counter = new Object2IntOpenHashMap<>();
        pharma = 0;
        empty = true;
        for (var i = 0; i < 4; i++) {
            var it = items.get(i);
            if (it.isEmpty()) continue;
            empty = false;
            var m = ElixirHelper.findMain(it.getItem());
            var o = ElixirHelper.findOff(it.getItem());
            if (m != null && o != null) {
                if (it.has(ElixirDataComponents.MaterialPropertySwitching)) {
                    if (it.getOrDefault(ElixirDataComponents.MaterialPropertySwitching, false)) m = null;
                    else o = null;
                } else {
                    if (level.random.nextDouble() > 0.5) m = null;
                    else o = null;
                }
            }
            calc(it, m, main);
            calc(it, o, off);
        }
    }

    private void startWithRecipe(Level level) {
        main = new HashSet<>();
        off = new HashSet<>();
        counter = new Object2IntOpenHashMap<>();
        pharma = 0;
        var ms = new Object2IntOpenHashMap<Item>();
        var os = new Object2IntOpenHashMap<Item>();
        for (var i = 0; i < 4; i++) {
            var it = items.get(i);
            if (it.isEmpty()) continue;
            var m = ElixirHelper.findMain(it.getItem());
            var o = ElixirHelper.findOff(it.getItem());
            if (m != null && o != null) {
                if (it.has(ElixirDataComponents.MaterialPropertySwitching)) {
                    if (it.getOrDefault(ElixirDataComponents.MaterialPropertySwitching, false)) m = null;
                    else o = null;
                } else {
                    if (level.random.nextDouble() > 0.5) {
                        m = null;
                        it.set(ElixirDataComponents.MaterialPropertySwitching, true);
                    } else {
                        o = null;
                        it.set(ElixirDataComponents.MaterialPropertySwitching, false);
                    }
                }
            }
            if (m != null) {
                ms.computeIntIfPresent(it.getItem(), (k, v) -> v + it.getCount());
                ms.putIfAbsent(it.getItem(), it.getCount());
            }
            if (o != null) {
                os.computeIntIfPresent(it.getItem(), (k, v) -> v + it.getCount());
                os.putIfAbsent(it.getItem(), it.getCount());
            }
        }
        var recipe = items.get(5).get(ElixirDataComponents.AlchemicalFormula);
        assert recipe != null;
        empty = true;
        if (ms.size() < recipe.main().size() || os.size() < recipe.off().size()) return;
        if (check(recipe.main(), ms) || check(recipe.off(), os)) return;
        empty = false;
        consume(recipe.main(), it -> !it.getOrDefault(ElixirDataComponents.MaterialPropertySwitching, false), main);
        consume(recipe.off(), it -> it.getOrDefault(ElixirDataComponents.MaterialPropertySwitching, false), off);
    }

    private void consume(List<AlchemicalFormulaComponent.Content> cs, Predicate<ItemStack> addition, Set<Holder<Material>> ms) {
        for (var rm : cs) {
            ms.add(rm.material());
            pharma += rm.material().value().pharm() * rm.count();
            stability += rm.material().value().stability() * rm.count() - rm.count() * (rm.count() * stabilityLossRate);
            ContainerHelper.clearOrCountMatchingItems(this, it -> it.is(rm.material().value().item()) && addition.test(it), rm.count(), false);
        }
    }

    private boolean check(List<AlchemicalFormulaComponent.Content> cs, Object2IntMap<Item> ms) {
        for (var rm : cs) {
            var flag = true;
            for (var mm : ms.object2IntEntrySet()) {
                if (rm.material().value().item().value() == mm.getKey()) {
                    if (mm.getIntValue() < rm.count()) return true;
                    ms.removeInt(mm.getKey());
                    flag = false;
                    break;
                }
            }
            if (flag) return true;
        }
        return false;
    }

    public boolean start(Level level, Player player) {
        if (started) return true;
        if (!items.get(4).isEmpty()) return false;
        stability = tempStability = 0;
        progress = failedProgress = explodeProgress = 0;
        trigger = player;
        if (!items.get(5).has(ElixirDataComponents.AlchemicalFormula)) startWithoutRecipe(level);
        else startWithRecipe(level);
        return started = true;
    }

    private void calc(ItemStack it, Holder<Material> m, Set<Holder<Material>> set) {
        if (m != null) {
            set.add(m);
            pharma += m.value().pharm() * it.getCount();
            stability += m.value().stability() * it.getCount() - it.getCount() * (it.getCount() * stabilityLossRate);
            counter.computeIntIfPresent(m, (k, v) -> v + it.getCount());
            counter.putIfAbsent(m, it.getCount());
            it.setCount(0);
        }
    }

    public boolean started() {
        return started;
    }

    private static double calcStability(Level level, BlockPos pos) {
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
        return Component.translatable("container.elixir.elixir_furnace");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ElixirFurnaceMenu(containerId, inventory, this, ContainerLevelAccess.create(level, getBlockPos()));
    }

    @Override
    public boolean canOpen(Player player) {
        return !started;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return 6;
    }
}

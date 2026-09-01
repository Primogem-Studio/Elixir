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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.per.elixir.Elixir;
import net.per.elixir.data.AlchemicalFormulaComponent;
import net.per.elixir.data.ElixirComponent;
import net.per.elixir.data.IFurnaceView;
import net.per.elixir.item.ElixirItem;
import net.per.elixir.registry.ElixirDataComponents;
import net.per.elixir.registry.ElixirItems;
import net.per.elixir.registry.ElixirRegistries;
import net.per.elixir.registry.data.Material;
import net.per.elixir.util.ElixirHelper;
import net.per.elixir.util.ElixirMath;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static net.per.elixir.Elixir.MOD_ID;
import static net.per.elixir.ElixirConfig.*;
import static net.per.elixir.registry.ElixirDataAttachments.ELIXIR_EXP;

public abstract class AbstractAlchemyFurnaceBlockEntity extends BaseContainerBlockEntity implements IFurnaceView {
    protected NonNullList<ItemStack> items;
    protected boolean started;
    protected int progress;
    public float temperature;
    public int tempRange;
    public float targetTemp = 250f;
    public int totalTicks = 200;
    public int pharmaLimit = 1000;
    public float exp;
    protected float expFactor;
    protected double stability, tempStability;
    protected int explodeProgress, failedProgress;
    protected Set<Holder<Material>> main, off;
    protected int pharma;
    protected boolean empty;
    protected Object2IntMap<Holder<Material>> counter;
    protected LivingEntity trigger;
    protected UUID triggerUUID;
    private final IItemHandler itemHandler = new InvWrapper(this) {
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canTakeItem(AbstractAlchemyFurnaceBlockEntity.this, slot, getStackInSlot(slot))) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    };

    protected AbstractAlchemyFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract int containerSize();

    protected abstract int materialSlotCount();

    protected abstract int outputSlot();

    protected abstract int formulaSlot();

    protected abstract double coolRate();

    protected abstract double extraStability(Level level, BlockPos pos);

    public abstract boolean isCovered(Level level);

    protected abstract double explodeY(BlockPos pos);

    protected abstract float explodeRadius();

    protected abstract void applySizeAdjustments();

    protected abstract void setActiveVisual(Level level, BlockPos pos, BlockState state, boolean active);

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, getItems(), provider);
        tag.putFloat("temperature", temperature);
        tag.putFloat("exp", exp);
        tag.putInt("pharma", pharma);
        tag.putInt("tempRange", tempRange);
        tag.putFloat("targetTemp", targetTemp);
        tag.putInt("totalTicks", totalTicks);
        tag.putDouble("stability", stability);
        tag.putDouble("tempStability", tempStability);
        tag.putInt("progress", progress);
        tag.putBoolean("started", started);
        tag.putInt("explodeProgress", explodeProgress);
        tag.putInt("failedProgress", failedProgress);
        tag.putBoolean("empty", empty);
        tag.put("mains", saveMaterials(main));
        tag.put("offs", saveMaterials(off));
        if (counter != null && !counter.isEmpty()) {
            var counters = new ListTag();
            for (var e : counter.object2IntEntrySet()) {
                var ct = new CompoundTag();
                e.getKey().unwrapKey().ifPresent(k -> ct.putString("mat", k.location().toString()));
                ct.putInt("count", e.getIntValue());
                counters.add(ct);
            }
            tag.put("counter", counters);
        }
        if (trigger != null) tag.putUUID("trigger", trigger.getUUID());
    }

    private static ListTag saveMaterials(Set<Holder<Material>> materials) {
        var tag = new ListTag();
        if (materials != null) {
            for (var h : materials) h.unwrapKey().ifPresent(k -> tag.add(StringTag.valueOf(k.location().toString())));
        }
        return tag;
    }

    private static Set<Holder<Material>> loadMaterials(CompoundTag tag, String key, HolderLookup.Provider provider) {
        var set = new HashSet<Holder<Material>>();
        if (tag.contains(key, Tag.TAG_LIST)) {
            var reg = provider.lookupOrThrow(ElixirRegistries.MATERIAL);
            for (var t : tag.getList(key, Tag.TAG_STRING)) {
                reg.get(ResourceKey.create(ElixirRegistries.MATERIAL, ResourceLocation.parse(t.getAsString()))).ifPresent(set::add);
            }
        }
        return set;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(containerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        temperature = tag.getFloat("temperature");
        exp = tag.getFloat("exp");
        expFactor = exp / (exp + expGrowthRate);
        pharma = tag.getInt("pharma");
        tempRange = tag.getInt("tempRange");
        progress = tag.getInt("progress");
        started = tag.getBoolean("started");
        stability = tag.getDouble("stability");
        tempStability = tag.getDouble("tempStability");
        pharmaLimit = tag.getInt("pharmaLimit");
        if (pharmaLimit <= 0) pharmaLimit = pharmaLimited;
        targetTemp = tag.getFloat("targetTemp");
        if (targetTemp <= 0) targetTemp = calcTargetTemp(pharma);
        totalTicks = tag.getInt("totalTicks");
        if (totalTicks <= 0) totalTicks = Math.clamp((long) pharma * refineTicks, 200, 1200);
        if (tag.contains("trigger")) triggerUUID = tag.getUUID("trigger");
        empty = tag.getBoolean("empty");
        main = loadMaterials(tag, "mains", provider);
        off = loadMaterials(tag, "offs", provider);
        if (tag.contains("counter", Tag.TAG_LIST)) {
            var c = new Object2IntOpenHashMap<Holder<Material>>();
            var reg = provider.lookupOrThrow(ElixirRegistries.MATERIAL);
            for (var t : tag.getList("counter", Tag.TAG_COMPOUND)) {
                var ct = (CompoundTag) t;
                if (!ct.contains("mat")) continue;
                reg.get(ResourceKey.create(ElixirRegistries.MATERIAL, ResourceLocation.parse(ct.getString("mat")))).ifPresent(h -> c.put(h, ct.getInt("count")));
            }
            counter = c;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putFloat("temperature", temperature);
        tag.putInt("pharma", pharma);
        tag.putInt("tempRange", tempRange);
        tag.putFloat("targetTemp", targetTemp);
        tag.putInt("totalTicks", totalTicks);
        tag.putInt("pharmaLimit", pharmaLimit);
        tag.putFloat("exp", exp);
        tag.putInt("progress", progress);
        tag.putBoolean("started", started);
        tag.putDouble("stability", stability);
        tag.putDouble("tempStability", tempStability);
        var offs = new ListTag();
        if (off != null)
            for (var h : off) h.unwrapKey().ifPresent(k -> offs.add(StringTag.valueOf(k.location().toString())));
        if (!offs.isEmpty()) tag.put("offs", offs);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel sl && started) {
            if (trigger == null && triggerUUID != null
                    && sl.getEntity(triggerUUID) instanceof LivingEntity le && le.isAlive()) {
                trigger = le;
            }
            if (trigger != null) {
                this.exp = Math.max(0f, trigger.getData(ELIXIR_EXP));
            }
            setActiveVisual(level, pos, state, true);
            progress++;
            temperature -= (float) coolRate();
            temperature = Math.clamp(temperature, 0, targetTemp + tempRange + tempSafeMargin);
            float cx = pos.getX() + 0.5f;
            float cy = pos.getY() + 0.5f;
            float cz = pos.getZ() + 0.5f;
            if (temperature < targetTemp) {
                if (temperature < targetTemp - tempRange) {
                    failedProgress++;
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, cx, cy, cz, 1, 0, 0, 0, 0.1);
                    if (failedProgress >= failedDelayBase + failedDelayGain * expFactor) {
                        started = false;
                        setActiveVisual(level, pos, state, false);
                        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                        if (empty) return;
                        failed(level);
                        return;
                    }
                }
                tempStability -= 2.5;
            } else {
                if (temperature > targetTemp + tempRange) {
                    explodeProgress++;
                    if (explodeProgress % 2 == 0)
                        sl.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, Math.min(explodeProgress / 2, 50), 0, 0, 0, 0.1);
                    if (explodeProgress >= explodeDelayBase + explodeDelayGain * expFactor)
                        level.explode(null, cx, explodeY(pos), cz, explodeRadius(), Level.ExplosionInteraction.TNT);
                }
                tempStability += 2.5;
            }
            if (progress >= totalTicks) {
                started = false;
                setActiveVisual(level, pos, state, false);
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                if (empty) return;
                process(level, pos);
                return;
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
        for (int attempts = 0; (off.isEmpty() || main.isEmpty()) && attempts < 100; attempts++) {
            reg.getRandom(level.random).ifPresent(m -> {
                if (m.value().main()) main.add(m);
                else off.add(m);
            });
        }
        var elixir = new ItemStack(ElixirItems.elixir.get());
        elixir.set(ElixirDataComponents.Elixir, new ElixirComponent(off.iterator().next(), pharma, List.copyOf(main)));
        elixir.set(DataComponents.ITEM_NAME, Component.translatable("item.elixir.failed").withColor(ElixirItem.getColor(elixir.get(ElixirDataComponents.Elixir))));
        items.set(outputSlot(), elixir);
        if (trigger instanceof Player) trigger.setData(ELIXIR_EXP, trigger.getData(ELIXIR_EXP) + expFailureGain);
        else if (trigger != null) trigger.setData(ELIXIR_EXP, trigger.getData(ELIXIR_EXP) + (float) maidExpFailureGain);
        level.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private void process(Level level, BlockPos pos) {
        if (main == null) main = new HashSet<>();
        if (off == null || off.isEmpty()) {
            if (off == null) off = new HashSet<>();
            off.add(level.registryAccess().holderOrThrow(ResourceKey.create(ElixirRegistries.MATERIAL, ResourceLocation.fromNamespaceAndPath(MOD_ID, "off/empty"))));
        }
        var elixir = new ItemStack(ElixirItems.elixir.get());
        var s = (extraStability(level, pos) + stability) * (1 + tempStability / (Math.abs(tempStability) + 50));
        if (!isCovered(level)) s /= 2;
        var exp = trigger != null ? trigger.getData(ELIXIR_EXP) : this.exp;
        Elixir.LOGGER.debug("[E]稳定性 {} 药理 {} 经验{} ", s, pharma, exp);
        if (s > -pharmaLimited) {
            elixir.set(ElixirDataComponents.Elixir, new ElixirComponent(List.copyOf(off).get(level.random.nextInt(off.size())), ElixirMath.rawPharm(pharma, exp, s), List.copyOf(main)));
            applyFormulaName(elixir);
            items.set(outputSlot(), elixir);
            outputRecipe();
            if (trigger instanceof Player) trigger.setData(ELIXIR_EXP, exp + expSuccessGain);
            else if (trigger != null) trigger.setData(ELIXIR_EXP, exp + (float) maidExpSuccessGain);
            level.playSound(null, worldPosition, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 1.0f);
            return;
        }
        failed(level);
    }

    private void applyFormulaName(ItemStack elixir) {
        var formula = items.get(formulaSlot());
        if (formula.isEmpty()) return;
        MutableComponent name = null;
        if (formula.has(DataComponents.CUSTOM_NAME)) {
            var s = formula.get(DataComponents.CUSTOM_NAME).getString();
            var idx = s.indexOf(" - ");
            if (idx < 0) return;
            var pillName = s.substring(idx + 3).trim();
            if (!pillName.isEmpty()) name = Component.literal(pillName);
        } else if (formula.has(DataComponents.ITEM_NAME)
                && formula.get(DataComponents.ITEM_NAME).getContents() instanceof TranslatableContents tc) {
            var key = tc.getKey();
            var prefix = "item.elixir.dan_fang.";
            if (key.startsWith(prefix)) name = Component.translatable("item.elixir.pill." + key.substring(prefix.length()));
        }
        if (name != null) {
            elixir.set(DataComponents.ITEM_NAME, name.withColor(ElixirItem.getColor(elixir.get(ElixirDataComponents.Elixir))));
        }
    }

    private void outputRecipe() {
        var it = items.get(formulaSlot());
        if (it.isEmpty() || it.has(ElixirDataComponents.AlchemicalFormula) || counter == null) return;
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
        for (var i = 0; i < materialSlotCount(); i++) {
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
        for (var i = 0; i < materialSlotCount(); i++) {
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
            } else if (o != null) {
                it.set(ElixirDataComponents.MaterialPropertySwitching, true);
            }
            if (m != null) {
                ms.put(it.getItem(), ms.getInt(it.getItem()) + it.getCount());
            }
            if (o != null) {
                os.put(it.getItem(), os.getInt(it.getItem()) + it.getCount());
            }
        }
        var recipe = items.get(formulaSlot()).get(ElixirDataComponents.AlchemicalFormula);
        assert recipe != null;
        empty = true;
        if (ms.size() < recipe.main().size() || os.size() < recipe.off().size()) return;
        if (check(recipe.main(), ms) || check(recipe.off(), os)) return;
        empty = false;
        consume(recipe.main(), it -> !it.getOrDefault(ElixirDataComponents.MaterialPropertySwitching, false), main);
        consume(recipe.off(), it -> it.getOrDefault(ElixirDataComponents.MaterialPropertySwitching, false), off);
    }

    public boolean isFormulaSatisfied() {
        var formula = items.get(formulaSlot());
        if (formula.isEmpty() || !formula.has(ElixirDataComponents.AlchemicalFormula)) {
            return true;
        }
        var ms = new Object2IntOpenHashMap<Item>();
        var os = new Object2IntOpenHashMap<Item>();
        for (var i = 0; i < materialSlotCount(); i++) {
            var it = items.get(i);
            if (it.isEmpty()) continue;
            var m = ElixirHelper.findMain(it.getItem());
            var o = ElixirHelper.findOff(it.getItem());
            if (m != null && o != null) {
                if (it.has(ElixirDataComponents.MaterialPropertySwitching)) {
                    if (it.getOrDefault(ElixirDataComponents.MaterialPropertySwitching, false)) m = null;
                    else o = null;
                } else {
                    continue;
                }
            }
            if (m != null) {
                ms.put(it.getItem(), ms.getInt(it.getItem()) + it.getCount());
            }
            if (o != null) {
                os.put(it.getItem(), os.getInt(it.getItem()) + it.getCount());
            }
        }
        var recipe = formula.get(ElixirDataComponents.AlchemicalFormula);
        assert recipe != null;
        if (ms.size() < recipe.main().size() || os.size() < recipe.off().size()) {
            return false;
        }
        return !check(recipe.main(), ms) && !check(recipe.off(), os);
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

    public boolean start(Level level, LivingEntity entity) {
        if (started) return true;
        if (!items.get(outputSlot()).isEmpty()) return false;
        stability = tempStability = 0;
        progress = failedProgress = explodeProgress = 0;
        trigger = entity;
        triggerUUID = entity.getUUID();
        exp = trigger.getData(ELIXIR_EXP);
        if (!items.get(formulaSlot()).has(ElixirDataComponents.AlchemicalFormula)) startWithoutRecipe(level);
        else startWithRecipe(level);
        expFactor = exp / (exp + expGrowthRate);
        tempRange = (int) Math.clamp(tempRangeBase + (tempRangeMax - tempRangeBase) * expFactor, 6, tempRangeMax);
        targetTemp = calcTargetTemp(pharma);
        totalTicks = Math.clamp((long) pharma * refineTicks, 200, 1200);
        applySizeAdjustments();
        started = true;
        setActiveVisual(level, worldPosition, level.getBlockState(worldPosition), true);
        return true;
    }

    private int calcTargetTemp(int pharma) {
        var margin = Math.clamp(tempTargetMargin, 0, 250);
        return (int) Math.clamp((pharma / (float) Math.max(1, pharmaLimit) + 1) / 2f * 500, margin, 500 - margin);
    }

    private void calc(ItemStack it, Holder<Material> m, Set<Holder<Material>> set) {
        if (m != null) {
            set.add(m);
            pharma += m.value().pharm() * it.getCount();
            stability += m.value().stability() * it.getCount() - it.getCount() * (it.getCount() * stabilityLossRate);
            counter.put(m, counter.getInt(m) + it.getCount());
            it.setCount(0);
        }
    }

    public boolean isPlayerTriggered() {
        return trigger instanceof Player;
    }

    @Override
    public float temperature() {
        return temperature;
    }

    @Override
    public float targetTemp() {
        return targetTemp;
    }

    @Override
    public int tempRange() {
        return tempRange;
    }

    @Override
    public boolean started() {
        return started;
    }

    @Override
    public int progress() {
        return progress;
    }

    @Override
    public int totalTicks() {
        return totalTicks;
    }

    @Override
    public int pharmaLimit() {
        return pharmaLimit;
    }

    @Override
    public double stability() {
        return stability;
    }

    @Override
    public double tempStability() {
        return tempStability;
    }

    @Override
    public Set<Holder<Material>> offs() {
        return off;
    }

    @Override
    public int pharma() {
        return pharma;
    }

    @Override
    public float exp() {
        return exp;
    }

    @Override
    public double stabilityBonus(Level level) {
        return extraStability(level, worldPosition);
    }

    @Override
    public BlockPos blockPos() {
        return worldPosition;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return !started && slot == outputSlot();
    }

    public IItemHandler itemHandler() {
        return itemHandler;
    }

    @Override
    public boolean canOpen(Player player) {
        return !started;
    }
}

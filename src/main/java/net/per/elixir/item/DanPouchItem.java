package net.per.elixir.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.per.elixir.data.DanPouchComponent;
import net.per.elixir.registry.ElixirDataComponents;

import java.util.List;

public class DanPouchItem extends Item {
    public DanPouchItem() {
        super(new Properties().stacksTo(1).food(new FoodProperties.Builder().alwaysEdible().nutrition(4).saturationModifier(0.3F).build()));
    }

    public static int selectedSlot(ItemStack pouch) {
        var comp = pouch.get(ElixirDataComponents.DanPouch);
        return comp == null ? -1 : comp.selected();
    }

    public static ItemStack selectedPill(ItemStack pouch) {
        var comp = pouch.get(ElixirDataComponents.DanPouch);
        if (comp == null || comp.selected() < 0) return ItemStack.EMPTY;
        return comp.get(comp.selected());
    }

    public static void setSelected(ItemStack pouch, int slot) {
        var comp = pouch.get(ElixirDataComponents.DanPouch);
        if (comp == null) {
            if (slot < 0) return;
            pouch.set(ElixirDataComponents.DanPouch, new DanPouchComponent(slot, List.of()));
        } else {
            pouch.set(ElixirDataComponents.DanPouch, comp.withSelected(slot));
        }
    }

    public static void throwPill(Player player, ItemStack pouch) {
        if (player.level().isClientSide) return;
        var pill = takePill(pouch);
        if (!pill.isEmpty()) ElixirItem.launch(player, pill);
    }

    public static void eatPill(ItemStack pouch, Level level, Player player) {
        var pill = takePill(pouch);
        if (!pill.isEmpty()) pill.getItem().finishUsingItem(pill, level, player);
    }

    private static ItemStack takePill(ItemStack pouch) {
        var comp = pouch.get(ElixirDataComponents.DanPouch);
        if (comp == null || comp.selected() < 0) return ItemStack.EMPTY;
        var slot = comp.selected();
        var current = comp.get(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        var taken = current.copy();
        taken.setCount(1);
        var rest = current.copy();
        rest.shrink(1);
        var updated = comp.setItem(slot, rest);
        if (rest.isEmpty()) updated = updated.withSelected(-1);
        pouch.set(ElixirDataComponents.DanPouch, updated);
        return taken;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                if (!selectedPill(stack).isEmpty()) {
                    throwPill(player, stack);
                    return InteractionResultHolder.consume(stack);
                }
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (selectedPill(stack).isEmpty()) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        var pill = selectedPill(stack);
        return pill.isEmpty() ? super.getUseDuration(stack, entity) : ElixirItem.eatDuration(pill);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide && livingEntity instanceof Player player) {
            eatPill(stack, level, player);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        list.add(Component.translatable("item.elixir.dan_pouch.usage.1"));
        list.add(Component.translatable("item.elixir.dan_pouch.usage.2", Component.keybind("key.elixir.dan_wheel")));
        var comp = stack.get(ElixirDataComponents.DanPouch);
        if (comp != null && comp.selected() >= 0) {
            var pill = comp.get(comp.selected());
            if (!pill.isEmpty()) {
                list.add(Component.translatable("item.elixir.dan_pouch.selected").append(pill.getHoverName()));
                var pillComp = pill.get(ElixirDataComponents.Elixir);
                if (pillComp != null) list.add(ElixirItem.pharmComponent(pillComp));
                return;
            }
        }
        list.add(Component.translatable("item.elixir.dan_pouch.selected").append(Component.translatable("item.elixir.dan_pouch.none").withColor(0x8A8A8A)));
    }
}

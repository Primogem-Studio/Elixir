package net.per.elixir.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.per.elixir.Elixir;
import net.per.elixir.data.ElixirComponent;
import net.per.elixir.entity.ElixirProjectile;
import net.per.elixir.registry.ElixirDataComponents;
import net.per.elixir.util.ElixirHelper;

import java.util.List;

import static net.per.elixir.ElixirConfig.*;

public class ElixirItem extends Item {
    public ElixirItem() {
        super(new Properties().food(new FoodProperties.Builder().alwaysEdible().nutrition(4).saturationModifier(0.3F).build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        var com = stack.get(ElixirDataComponents.Elixir);
        if (com != null) {
            var pharma = getPharma(com) / pharmaConversionRate;
            var time = pharma * timeConversionRate;
            time = time <= 0 ? -time : time;
            Elixir.LOGGER.debug("[E]最终 {} 药理", pharma);
            for (var m : com.main())
                ElixirHelper.execute(m.value(), pharma, time, stack, level, livingEntity);
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                var pe = new ElixirProjectile(player);
                pe.setOwner(player);
                pe.setItem(player.getItemInHand(hand));
                pe.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.5f, 1.0f);
                level.addFreshEntity(pe);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 0.5f, 0.5f);
                if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), true);
        }
        return super.use(level, player, hand);
    }

    public static int getColor(ElixirComponent com) {
        return switch (getLevel(com)) {
            case 1 -> 0xFF666666;
            case 2 -> 0xFF777777;
            case 3 -> 0xFF888888;
            case 4 -> 0xFF999999;
            case 5 -> 0xFFAAAAAA;
            case 6 -> 0xFFBBBBBB;
            case 7 -> 0xFFCCCCCC;
            case 8 -> 0xFFD8D8D8;
            case 9 -> 0xFFE4E4E4;
            case 10 -> 0xFFF0F0F0;
            case 11 -> 0xFFE0FFEE;
            case 12 -> 0xFFBBFFDD;
            case 13 -> 0xFF95FFCC;
            case 14 -> 0xFF99EEFF;
            case 15 -> 0xFF77DDFF;
            case 16 -> 0xFFAACCFF;
            case 17 -> 0xFFCCAAFF;
            case 18 -> 0xFFEE99FF;
            case 19 -> 0xFFFFAAEE;
            case 20 -> 0xFFFF9922;
            case 21 -> 0xFFFF3F5F;
            default -> -1;
        };
    }

    private static int getPharma(ElixirComponent com) {
        return Mth.clamp(ElixirHelper.calc(com.off().value(), com.pharm(), com.off().value().base()), -pharmaLimited, pharmaLimited);
    }

    private static int getLevel(ElixirComponent com) {
        return (int) ((Mth.clamp(getPharma(com), -highestPharmaLimited, highestPharmaLimited) + highestPharmaLimited) / (highestPharmaLimited * 2f) * 20) + 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        var com = stack.get(ElixirDataComponents.Elixir);
        if (com != null) {
            list.add(Component.translatable("item.elixir.pharma").append(Component.translatable("item.elixir.pharma.level." + getLevel(com)).withColor(getColor(com))));
//            list.add(Component.nullToEmpty(String.valueOf(getPharma(com))));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        var com = stack.get(ElixirDataComponents.Elixir);
        if (com != null) {
            var c = Component.empty().withColor(getColor(com));
            c.append(Component.translatable("item.elixir.material.name." + com.off().unwrapKey().orElseThrow().location().toLanguageKey()));
            for (var m : com.main()) {
                c.append(Component.translatable("item.elixir.action." + m.value().effect().left().orElseThrow().unwrapKey().orElseThrow().location().toLanguageKey()));
                c.append(Component.translatable("item.elixir.data.splitter"));
            }
            c.getSiblings().removeLast();
            c.append(super.getName(stack));
            return c;
        }
        return super.getName(stack);
    }
}

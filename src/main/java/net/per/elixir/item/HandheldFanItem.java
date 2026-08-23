package net.per.elixir.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class HandheldFanItem extends Item {
    public HandheldFanItem() {
        super(new Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        list.add(Component.translatable("item.elixir.handheld_fan.usage.1"));
        list.add(Component.translatable("item.elixir.handheld_fan.usage.2"));
    }
}

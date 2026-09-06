package net.per.elixir.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DanFormSealItem extends Item {
    public DanFormSealItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        list.add(Component.translatable("item.elixir.dan_form_seal.tag"));
        list.add(Component.translatable("item.elixir.dan_form_seal.usage.1"));
        list.add(Component.translatable("item.elixir.dan_form_seal.usage.2"));
    }
}

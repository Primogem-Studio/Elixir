package net.per.elixir.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IElixirAction {
    void onAction(int pharm, int time, ItemStack stack, Level level, LivingEntity entity);
}

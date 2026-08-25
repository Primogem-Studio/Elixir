package net.per.elixir.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.per.elixir.registry.data.Material;

import java.util.Set;

public interface IFurnaceView {
    float temperature();

    float targetTemp();

    int tempRange();

    boolean started();

    int progress();

    int totalTicks();

    int pharmaLimit();

    double stability();

    double tempStability();

    Set<Holder<Material>> offs();

    int pharma();

    float exp();

    boolean isCovered(Level level);

    double stabilityBonus(Level level);

    BlockPos blockPos();
}

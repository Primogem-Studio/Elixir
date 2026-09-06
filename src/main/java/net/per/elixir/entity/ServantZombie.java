package net.per.elixir.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public final class ServantZombie extends Zombie {
    public ServantZombie(Level level) {
        super(EntityType.ZOMBIE, level);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }
}

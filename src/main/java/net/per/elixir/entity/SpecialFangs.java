package net.per.elixir.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public final class SpecialFangs extends EvokerFangs {
    public float damage = 6;

    public SpecialFangs(Level level, double x, double y, double z, float yRot, int warmupDelay, LivingEntity owner) {
        super(level, x, y, z, yRot, warmupDelay, owner);
    }

    public SpecialFangs setDamage(float damage) {
        this.damage = damage;
        return this;
    }

    @Override
    protected void dealDamageTo(LivingEntity target) {
        var owner = getOwner();
        if (!target.isAlive() || target.isInvulnerable() || target == owner) return;
        if (owner == null) {
            target.hurt(damageSources().magic(), damage);
            return;
        }
        if (level() instanceof ServerLevel level) {
            var ds = damageSources().indirectMagic(this, owner);
            if (target.hurt(ds, damage)) EnchantmentHelper.doPostAttackEffects(level, target, ds);
        }
    }
}

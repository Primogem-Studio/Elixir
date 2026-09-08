package net.per.elixir.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.per.elixir.compat.ModSet;
import net.per.elixir.compat.genshincraft.DamageHelper;

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
            target.hurt(wrap(damageSources().magic()), damage);
            return;
        }
        if (level() instanceof ServerLevel level) {
            var ds = wrap(damageSources().indirectMagic(this, owner));
            if (target.hurt(ds, damage)) EnchantmentHelper.doPostAttackEffects(level, target, ds);
        }
    }

    private DamageSource wrap(DamageSource source) {
        if (ModSet.GenshinCraft) return DamageHelper.type(source, 0);
        return source;
    }
}

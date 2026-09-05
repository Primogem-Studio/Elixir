package net.per.elixir.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class ElixirSummon {
    private static final String SERVANT = "elixir_servant";
    private static final String HOSTILE = "elixir_hostile";
    private static final String OWNER = "elixir_owner";
    private static final String BATCH = "elixir_batch";
    private static final String FOE = "elixir_foe";

    private ElixirSummon() {
    }

    public static boolean isServant(LivingEntity living) {
        return living instanceof Zombie zombie && zombie.getPersistentData().getBoolean(SERVANT);
    }

    public static boolean isHostile(Zombie zombie) {
        return zombie.getPersistentData().getBoolean(HOSTILE);
    }

    public static String ownerId(Zombie zombie) {
        return zombie.getPersistentData().getString(OWNER);
    }

    public static String foeId(Zombie zombie) {
        return zombie.getPersistentData().getString(FOE);
    }

    public static void tickServant(Zombie zombie) {
        if (zombie.level().isClientSide || zombie.tickCount % 10 != 0 || !isServant(zombie) || isHostile(zombie)) return;
        var current = zombie.getTarget();
        if (current != null && current.isAlive()) return;
        if (!(zombie.level() instanceof ServerLevel level)) return;
        var owner = level.getEntity(UUID.fromString(ownerId(zombie)));
        if (!(owner instanceof LivingEntity lo) || !lo.isAlive()) return;
        LivingEntity foe = null;
        double best = Double.MAX_VALUE;
        for (var m : level.getEntitiesOfClass(Mob.class, lo.getBoundingBox().inflate(24))) {
            if (!m.isAlive() || isServant(m) || m == lo || m == zombie) continue;
            if (m.getTarget() != lo && !(lo instanceof Mob om && om.getTarget() == m)) continue;
            double d = zombie.distanceToSqr(m);
            if (d < best) {
                best = d;
                foe = m;
            }
        }
        if (foe == null) {
            for (var m : level.getEntitiesOfClass(Mob.class, zombie.getBoundingBox().inflate(16))) {
                if (!m.isAlive() || isServant(m) || !(m instanceof Enemy)) continue;
                double d = zombie.distanceToSqr(m);
                if (d < best) {
                    best = d;
                    foe = m;
                }
            }
        }
        if (foe != null) {
            zombie.getPersistentData().putString(FOE, foe.getUUID().toString());
            zombie.setTarget(foe);
        } else {
            zombie.getPersistentData().putString(FOE, "");
        }
    }

    public static int summonZombies(ServerLevel level, LivingEntity summoner, LivingEntity foe, boolean hostile, int amount) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(summoner.getType());
        String batch = summoner instanceof Player p ? "p_" + p.getStringUUID() : "t_" + key;
        var olds = new java.util.ArrayList<Zombie>();
        for (var e : level.getAllEntities()) {
            if (e instanceof Zombie z && isServant(z) && batch.equals(z.getPersistentData().getString(BATCH))) olds.add(z);
        }
        for (var z : olds) z.discard();
        int spawned = 0;
        var r = level.random;
        for (int i = 0; i < amount; i++) {
            var z = new Zombie(EntityType.ZOMBIE, level);
            var tag = z.getPersistentData();
            tag.putBoolean(SERVANT, true);
            tag.putBoolean(HOSTILE, hostile);
            tag.putString(OWNER, summoner.getUUID().toString());
            tag.putString(BATCH, batch);
            z.setPersistenceRequired();
            z.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0));
            z.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0));
            z.moveTo(summoner.getX() + (r.nextDouble() - 0.5) * 4, summoner.getY(), summoner.getZ() + (r.nextDouble() - 0.5) * 4);
            for (int k = 0; k < 6 && !level.noCollision(z.getBoundingBox()); k++) z.moveTo(z.getX(), z.getY() + 1, z.getZ());
            if (foe != null) {
                tag.putString(FOE, foe.getUUID().toString());
                z.setTarget(foe);
            }
            level.addFreshEntity(z);
            if (foe != null) z.hurt(level.damageSources().mobAttack(foe), 0.01f);
            spawned++;
        }
        return spawned;
    }
}

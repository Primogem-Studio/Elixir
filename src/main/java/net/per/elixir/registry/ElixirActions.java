package net.per.elixir.registry;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.per.elixir.util.IElixirAction;
import net.per.elixir.util.ModifierUtil;

import static net.per.elixir.Elixir.MOD_ID;
import static net.per.elixir.ElixirConfig.attributeModifierDilute;
import static net.per.elixir.ElixirConfig.effectDilute;

public class ElixirActions {
    public static final DeferredRegister<IElixirAction> ACTIONS = DeferredRegister.create(ElixirRegistries.ACTION, MOD_ID);

    static {
        ACTIONS.register("speed", () -> effect(MobEffects.MOVEMENT_SPEED));
        ACTIONS.register("slowdown", () -> effect(MobEffects.MOVEMENT_SLOWDOWN));
        ACTIONS.register("dig_speed", () -> effect(MobEffects.DIG_SPEED));
        ACTIONS.register("dig_slowdown", () -> effect(MobEffects.DIG_SLOWDOWN));
        ACTIONS.register("damage_boost", () -> effect(MobEffects.DAMAGE_BOOST));
        ACTIONS.register("instant_health", () -> (pharm, time, stack, level, entity) -> entity.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, (int) (pharm / effectDilute))));
        ACTIONS.register("instant_damage", () -> (pharm, time, stack, level, entity) -> entity.addEffect(new MobEffectInstance(MobEffects.HARM, 1, (int) Math.min(Math.ceil(Math.log(entity.getMaxHealth() / 6) / Math.log(2)) + 1, (pharm / effectDilute)))));
        ACTIONS.register("jump_boost", () -> effect(MobEffects.JUMP));
        ACTIONS.register("confusion", () -> effect(MobEffects.CONFUSION));
        ACTIONS.register("regeneration", () -> effect(MobEffects.REGENERATION));
        ACTIONS.register("damage_resistance", () -> effect(MobEffects.DAMAGE_RESISTANCE));
        ACTIONS.register("fire_resistance", () -> effect(MobEffects.FIRE_RESISTANCE));
        ACTIONS.register("water_breathing", () -> effect(MobEffects.WATER_BREATHING));
        ACTIONS.register("invisibility", () -> effect(MobEffects.INVISIBILITY));
        ACTIONS.register("blindness", () -> effect(MobEffects.BLINDNESS));
        ACTIONS.register("night_vision", () -> effect(MobEffects.NIGHT_VISION));
        ACTIONS.register("hunger", () -> effect(MobEffects.HUNGER));
        ACTIONS.register("weakness", () -> effect(MobEffects.WEAKNESS));
        ACTIONS.register("poison", () -> effect(MobEffects.POISON));
        ACTIONS.register("wither", () -> effect(MobEffects.WITHER));
        ACTIONS.register("health_boost", () -> effect(MobEffects.HEALTH_BOOST));
        ACTIONS.register("absorption", () -> effect(MobEffects.ABSORPTION));
        ACTIONS.register("saturation", () -> effect(MobEffects.SATURATION));
        ACTIONS.register("glowing", () -> effect(MobEffects.GLOWING));
        ACTIONS.register("levitation", () -> effect(MobEffects.LEVITATION));
        ACTIONS.register("luck", () -> effect(MobEffects.LUCK));
        ACTIONS.register("unluck", () -> effect(MobEffects.UNLUCK));
        ACTIONS.register("slow_falling", () -> effect(MobEffects.SLOW_FALLING));
        ACTIONS.register("conduit_power", () -> effect(MobEffects.CONDUIT_POWER));
        ACTIONS.register("dolphins_grace", () -> effect(MobEffects.DOLPHINS_GRACE));
        ACTIONS.register("bad_omen", () -> effect(MobEffects.BAD_OMEN));
        ACTIONS.register("hero_of_the_village", () -> effect(MobEffects.HERO_OF_THE_VILLAGE));
        ACTIONS.register("darkness", () -> effect(MobEffects.DARKNESS));
        ACTIONS.register("trial_omen", () -> effect(MobEffects.TRIAL_OMEN));
        ACTIONS.register("raid_omen", () -> effect(MobEffects.RAID_OMEN));
        ACTIONS.register("wind_charged", () -> effect(MobEffects.WIND_CHARGED));
        ACTIONS.register("weaving", () -> effect(MobEffects.WEAVING));
        ACTIONS.register("oozing", () -> effect(MobEffects.OOZING));
        ACTIONS.register("infested", () -> effect(MobEffects.INFESTED));

        ACTIONS.register("speed_slowdown", () -> effect(MobEffects.MOVEMENT_SPEED, MobEffects.MOVEMENT_SLOWDOWN));
        ACTIONS.register("dig_speed_slowdown", () -> effect(MobEffects.DIG_SPEED, MobEffects.DIG_SLOWDOWN));
        ACTIONS.register("strength", () -> effect(MobEffects.DAMAGE_BOOST, MobEffects.WEAKNESS));
        ACTIONS.register("poison_wither", () -> effect(MobEffects.POISON, MobEffects.WITHER));
        ACTIONS.register("regeneration_poison", () -> effect(MobEffects.REGENERATION, MobEffects.POISON));
        ACTIONS.register("invisibility_glowing", () -> effect(MobEffects.INVISIBILITY, MobEffects.GLOWING));
        ACTIONS.register("instant_health_damage", () -> (pharm, time, stack, level, entity) -> {
            var d = pharm > 0 ? (pharm / effectDilute) : Math.min(Math.ceil(Math.log(entity.getMaxHealth() / 6) / Math.log(2)) + 1, (-pharm / effectDilute));
            entity.addEffect(new MobEffectInstance(pharm > 0 ? MobEffects.HEAL : MobEffects.HARM, 1, (int) d));
        });
        ACTIONS.register("night_vision_blindness", () -> effect(MobEffects.NIGHT_VISION, MobEffects.BLINDNESS));

        ACTIONS.register("max_health", () -> modifier("e1", Attributes.MAX_HEALTH));
        ACTIONS.register("follow_range", () -> modifier("e2", Attributes.FOLLOW_RANGE));
        ACTIONS.register("knockback_resistance", () -> modifier("e3", Attributes.KNOCKBACK_RESISTANCE));
        ACTIONS.register("movement_speed", () -> modifier("e4", Attributes.MOVEMENT_SPEED));
        ACTIONS.register("attack_damage", () -> modifier("e5", Attributes.ATTACK_DAMAGE));
        ACTIONS.register("armor", () -> modifier("e6", Attributes.ARMOR));
        ACTIONS.register("armor_toughness", () -> modifier("e7", Attributes.ARMOR_TOUGHNESS));
        ACTIONS.register("attack_speed", () -> modifier("e8", Attributes.ATTACK_SPEED));
        ACTIONS.register("lucks", () -> modifier("e9", Attributes.LUCK));
        ACTIONS.register("entity_interaction_range", () -> modifier("e10", Attributes.ENTITY_INTERACTION_RANGE));
        ACTIONS.register("gravity", () -> modifier("e11", Attributes.GRAVITY));
        ACTIONS.register("true_lightning", () -> (pharm, time, stack, level, entity) -> {
            if (!level.isClientSide) {
                int c = Math.max(Math.abs(pharm) / 10, 1);
                level.addFreshEntity(new LightningBolt(EntityType.LIGHTNING_BOLT, level) {{
                    setPos(entity.getX(), entity.getY(), entity.getZ());
                }});
                var list = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(10), e -> e != entity && e.isAlive());
                if (list.isEmpty()) return;
                var r = level.getRandom();
                for (int i = 0; i < c; i++) {
                    var t = list.get(r.nextInt(list.size()));
                    level.addFreshEntity(new LightningBolt(EntityType.LIGHTNING_BOLT, level) {{
                        setPos(t.getX(), t.getY(), t.getZ());
                    }});
                }
            }

        });

        ACTIONS.register("explode", () -> (pharm, time, stack, level, entity) -> {
            if (!level.isClientSide)
                level.explode(null, entity.getX(), entity.getY(), entity.getZ(), Math.clamp(pharm, 1, 100), Level.ExplosionInteraction.TNT);
        });
        ACTIONS.register("lightning", () -> (pharm, time, stack, level, entity) -> {
            if (!level.isClientSide) {
                entity.hurt(level.damageSources().lightningBolt(), pharm > 0 ? pharm : -pharm * 2);
                level.addFreshEntity(new LightningBolt(EntityType.LIGHTNING_BOLT, level) {{
                    setPos(entity.getX(), entity.getY(), entity.getZ());
                    setVisualOnly(true);
                }});
            }
        });
        ACTIONS.register("stereo_explode", () -> (pharm, time, stack, level, entity) -> {
            if (level.isClientSide) return;
            var r = level.random;
            double radius = 5 + pharm / 2f;
            for (int i = 0; i < pharm / 2; i++)
                level.explode(entity, entity.getX() + (r.nextDouble() - 0.5) * 2 * radius, entity.getY() + (r.nextDouble() - 0.5) * radius, entity.getZ() + (r.nextDouble() - 0.5) * 2 * radius, Math.clamp(pharm / 10, 1, 10), Level.ExplosionInteraction.TNT);
        });
    }

    private static IElixirAction effect(Holder<MobEffect> effect) {
        return (pharm, time, stack, level, entity) -> {
            if (pharm < 0) {
                var et = entity.getEffect(effect);
                entity.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 0));
                if (et != null && time * 1.5 >= et.getDuration()) entity.removeEffect(effect);
                return;
            }
            entity.addEffect(new MobEffectInstance(effect, Math.max(time, 200), (int) (pharm / effectDilute)));
        };
    }

    private static IElixirAction effect(Holder<MobEffect> ef, Holder<MobEffect> ef2) {
        return (pharm, time, stack, level, entity) -> {
            boolean p = pharm > 0;
            entity.addEffect(new MobEffectInstance(p ? ef : ef2, Math.max(time, 200), (int) ((p ? pharm : -pharm) / effectDilute)));
        };
    }

    private static IElixirAction modifier(String name, Holder<Attribute> attr) {
        return (pharm, time, stack, level, entity) -> ModifierUtil.addTimeLimited(entity, time, attr, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), pharm / attributeModifierDilute, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
}

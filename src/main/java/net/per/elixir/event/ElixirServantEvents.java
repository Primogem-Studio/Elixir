package net.per.elixir.event;

import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.per.elixir.util.ElixirSummon;

import static net.per.elixir.Elixir.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class ElixirServantEvents {
    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie) || !ElixirSummon.isServant(zombie)) return;
        var target = event.getNewAboutToBeSetTarget();
        if (target == null) return;
        if (target.getUUID().toString().equals(ElixirSummon.ownerId(zombie))) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }
        if (target instanceof Zombie zombieTarget && ElixirSummon.isServant(zombieTarget)) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }
        if (ElixirSummon.isHostile(zombie)) return;
        if (target instanceof Enemy || target == zombie.getLastHurtByMob()) return;
        String foe = ElixirSummon.foeId(zombie);
        if (!foe.isEmpty() && foe.equals(target.getUUID().toString())) return;
        event.setNewAboutToBeSetTarget(null);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Zombie zombie) ElixirSummon.tickServant(zombie);
    }
}

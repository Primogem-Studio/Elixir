package net.per.elixir.entity;

import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.per.elixir.registry.ElixirEntityTypes;
import net.per.elixir.registry.ElixirItems;

public class ElixirProjectile extends ThrowableItemProjectile {

    public ElixirProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public ElixirProjectile(LivingEntity shooter) {
        super(ElixirEntityTypes.elixir.get(), shooter, shooter.level());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        Containers.dropItemStack(level(), getX(), getY(), getZ(), getItem().copy());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide && result.getEntity() instanceof LivingEntity e)
            getItem().finishUsingItem(level(), e);
    }

    @Override
    protected Item getDefaultItem() {
        return ElixirItems.elixir.get();
    }
}

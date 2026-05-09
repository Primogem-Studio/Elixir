package net.per.elixir.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.per.elixir.entity.ElixirProjectile;

import java.util.function.Supplier;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final Supplier<EntityType<ElixirProjectile>> elixir = ENTITY_TYPES.register("elixir", () -> EntityType.Builder.<ElixirProjectile>of(ElixirProjectile::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).noSave().build("elixir"));
}

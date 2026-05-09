package net.per.elixir.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.per.elixir.block.entity.ElixirFurnaceBlockEntity;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    static {
        BLOCK_ENTITY_TYPES.register("elixir_furnace", () -> ElixirFurnaceBlockEntity.Type);
    }
}

package net.per.elixir.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.per.elixir.data.AlchemicalFormulaComponent;
import net.per.elixir.data.ElixirComponent;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DataComponentType<ElixirComponent> Elixir = DataComponentType.<ElixirComponent>builder().persistent(ElixirComponent.codec).networkSynchronized(ElixirComponent.streamCodec).build();
    public static final DataComponentType<AlchemicalFormulaComponent> AlchemicalFormula = DataComponentType.<AlchemicalFormulaComponent>builder().persistent(AlchemicalFormulaComponent.codec).networkSynchronized(AlchemicalFormulaComponent.streamCodec).build();
    public static final DataComponentType<Boolean> MaterialPropertySwitching = DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build();

    static {
        COMPONENTS.register("elixir", () -> Elixir);
        COMPONENTS.register("alchemical_formula", () -> AlchemicalFormula);
        COMPONENTS.register("material_property_switching", () -> MaterialPropertySwitching);
    }
}

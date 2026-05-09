package net.per.elixir.registry;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.per.elixir.util.IElixirCalc;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirCalculators {
    public static final DeferredRegister<IElixirCalc> CALCULATORS = DeferredRegister.create(ElixirRegistries.CALCULATOR, MOD_ID);

    static {
        CALCULATORS.register("empty", () -> (sum, base) -> sum);
        CALCULATORS.register("mul", () -> (sum, base) -> (int) (sum * base));
        CALCULATORS.register("plus", () -> (sum, base) -> (int) (sum + base));
    }
}

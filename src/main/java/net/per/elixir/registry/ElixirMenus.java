package net.per.elixir.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.per.elixir.data.ElixirFurnaceMenu;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

    static {
        MENUS.register("elixir_furnace", () -> ElixirFurnaceMenu.Type);
    }
}

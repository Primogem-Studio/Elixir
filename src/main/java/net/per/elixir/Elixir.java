package net.per.elixir;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.per.elixir.registry.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Elixir.MOD_ID)
public class Elixir {
    public static final String MOD_ID = "elixir";
    public static final Logger LOGGER = LogManager.getLogger();

    public Elixir(IEventBus bus) {
        ElixirItems.ITEMS.register(bus);
        ElixirBlocks.BLOCKS.register(bus);
        ElixirEntityTypes.ENTITY_TYPES.register(bus);
        ElixirDataComponents.COMPONENTS.register(bus);
        ElixirTabs.TABS.register(bus);
        ElixirMenus.MENUS.register(bus);
        ElixirBlockEntityTypes.BLOCK_ENTITY_TYPES.register(bus);
        ElixirActions.ACTIONS.register(bus);
        ElixirCalculators.CALCULATORS.register(bus);
        ElixirDataAttachments.ATTACHMENT_TYPES.register(bus);
        ElixirConfig.load();
    }
}

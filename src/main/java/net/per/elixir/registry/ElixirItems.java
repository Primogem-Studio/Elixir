package net.per.elixir.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.per.elixir.item.ElixirItem;
import net.per.elixir.item.HandheldFanItem;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<Item> elixir = ITEMS.register("elixir", ElixirItem::new);
    public static final DeferredItem<Item> handheld_fan = ITEMS.register("handheld_fan", HandheldFanItem::new);

    public static final DeferredItem<BlockItem> elixir_furnace = ITEMS.registerSimpleBlockItem(ElixirBlocks.elixir_furnace);
    public static final DeferredItem<BlockItem> elixir_furnace_cover = ITEMS.registerSimpleBlockItem(ElixirBlocks.elixir_furnace_cover);
    public static final DeferredItem<BlockItem> alchemical_vessel = ITEMS.registerSimpleBlockItem(ElixirBlocks.alchemical_vessel);
    public static final DeferredItem<BlockItem> elixir_furnace_brick = ITEMS.registerSimpleBlockItem(ElixirBlocks.elixir_furnace_brick);
}

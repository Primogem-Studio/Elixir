package net.per.elixir.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.registry.ElixirItems;

import static net.per.elixir.Elixir.MOD_ID;

@GameTestHolder(MOD_ID)
public class FurnaceCapabilityTests {
    public static void register(RegisterGameTestsEvent event) {
        event.register(FurnaceCapabilityTests.class);
    }

    @GameTest(template = "furnace_test")
    public static void itemHandlerTransfer(GameTestHelper helper) {
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ElixirBlocks.elixir_furnace.get());
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        if (handler == null) helper.fail("ItemHandler capability missing");
        if (!handler.insertItem(0, new ItemStack(Items.APPLE), false).isEmpty()) helper.fail("material insert rejected");
        if (handler.getStackInSlot(0).isEmpty()) helper.fail("material insert lost");
        if (!handler.extractItem(0, 1, true).isEmpty()) helper.fail("material slot extraction allowed");
        ((IItemHandlerModifiable) handler).setStackInSlot(4, new ItemStack(ElixirItems.elixir.get()));
        if (handler.extractItem(4, 1, true).isEmpty()) helper.fail("output extraction rejected");
        helper.succeed();
    }
}

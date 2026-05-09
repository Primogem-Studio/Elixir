package net.per.elixir.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.per.elixir.block.entity.ElixirFurnaceBlockEntity;

public class ElixirFurnaceMenu extends AbstractContainerMenu {
    public static final MenuType<ElixirFurnaceMenu> Type = IMenuTypeExtension.create(ElixirFurnaceMenu::new);
    private final ContainerLevelAccess access;
    private final Container inventory;

    public ElixirFurnaceMenu(int id, Inventory inv, RegistryFriendlyByteBuf ignored) {
        this(id, inv, new SimpleContainer(6), ContainerLevelAccess.NULL);
    }

    public ElixirFurnaceMenu(int containerId, Inventory inv, Container container, ContainerLevelAccess access) {
        super(Type, containerId);
        inventory = container;
        this.access = access;
        addSlot(new Slot(inventory, 0, 31, 25));
        addSlot(new Slot(inventory, 1, 53, 25));
        addSlot(new Slot(inventory, 2, 31, 47));
        addSlot(new Slot(inventory, 3, 53, 47));
        addSlot(new Slot(inventory, 4, 132, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(inventory, 5, 132, 63));

        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                addSlot(new Slot(inv, j + (i + 1) * 9, 10 + j * 18, 97 + i * 18));
        for (int i = 0; i < 9; ++i)
            addSlot(new Slot(inv, i, 10 + i * 18, 155));
    }


    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var itemStack = ItemStack.EMPTY;
        var slot = slots.get(index);
        if (slot.hasItem()) {
            var stack = slot.getItem();
            itemStack = stack.copy();
            if (index < 6) {
                if (!moveItemStackTo(stack, 6, slots.size(), false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, 6, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof ElixirFurnaceBlockEntity be && !be.started() && player.canInteractWithBlock(pos, 4), true);
    }
}

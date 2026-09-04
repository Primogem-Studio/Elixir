package net.per.elixir.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.per.elixir.item.DanPouchItem;
import net.per.elixir.item.ElixirItem;
import net.per.elixir.registry.ElixirDataComponents;

import java.util.ArrayList;
import java.util.List;

public class DanPouchMenu extends AbstractContainerMenu {
    public static final MenuType<DanPouchMenu> Type = IMenuTypeExtension.create(DanPouchMenu::new);
    public static final int IMAGE_W = 176;
    public static final int IMAGE_H = 166;
    public static final int POUCH_X0 = 47;
    public static final int POUCH_Y0 = 26;
    public static final int POUCH_PITCH = 22;
    public static final int INV_X0 = 8;
    public static final int INV_Y0 = 84;
    public static final int HOTBAR_Y = 142;

    private final Player player;
    private final ItemStack pouch;
    private final PouchContainer pouchContainer;
    private final int pouchSlot;
    private final int pouchInvIndex;

    public DanPouchMenu(int id, Inventory inv, ItemStack pouch) {
        super(Type, id);
        this.player = inv.player;
        this.pouch = pouch;
        this.pouchContainer = new PouchContainer(pouch);
        this.pouchInvIndex = inv.selected;
        this.pouchSlot = DanPouchComponent.SIZE + 27 + inv.selected;
        for (int i = 0; i < DanPouchComponent.SIZE; i++) {
            int col = i % 4;
            int row = i / 4;
            addSlot(new Slot(pouchContainer, i, POUCH_X0 + col * POUCH_PITCH, POUCH_Y0 + row * POUCH_PITCH) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ElixirItem;
                }
            });
        }
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                addSlot(new Slot(inv, j + (i + 1) * 9, INV_X0 + j * 18, INV_Y0 + i * 18));
        for (int i = 0; i < 9; ++i) {
            if (i == inv.selected) addSlot(new LockedSlot(inv, i, INV_X0 + i * 18, HOTBAR_Y));
            else addSlot(new Slot(inv, i, INV_X0 + i * 18, HOTBAR_Y));
        }
    }

    public DanPouchMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, ItemStack.EMPTY);
    }

    public static void open(ServerPlayer player, ItemStack pouch) {
        player.openMenu(new SimpleMenuProvider((id, inv, ply) -> new DanPouchMenu(id, inv, pouch), Component.empty()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == pouchSlot) return ItemStack.EMPTY;
        var itemstack = ItemStack.EMPTY;
        var slot = slots.get(index);
        if (slot.hasItem()) {
            var stack = slot.getItem();
            itemstack = stack.copy();
            if (index < DanPouchComponent.SIZE) {
                if (!moveItemStackTo(stack, DanPouchComponent.SIZE, slots.size(), false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, DanPouchComponent.SIZE, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isLockedAction(slotId, button, clickType)) return;
        super.clicked(slotId, button, clickType, player);
    }

    private boolean isLockedAction(int slotId, int button, ClickType clickType) {
        if (clickType == ClickType.SWAP) {
            if (button == pouchInvIndex) return true;
            if (slotId == pouchSlot && (button >= 0 && button < 9 || button == 40)) return true;
            return false;
        }
        if (slotId != pouchSlot) return false;
        return clickType == ClickType.PICKUP
                || clickType == ClickType.QUICK_MOVE
                || clickType == ClickType.THROW
                || clickType == ClickType.SWAP
                || clickType == ClickType.PICKUP_ALL
                || clickType == ClickType.CLONE;
    }

    @Override
    public boolean stillValid(Player player) {
        if (pouch.isEmpty()) return true;
        return this.player == player
                && player.getMainHandItem() == pouch
                && player.getMainHandItem().getItem() instanceof DanPouchItem;
    }

    private static class LockedSlot extends Slot {
        LockedSlot(Container inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static class PouchContainer implements Container {
        private final ItemStack pouch;
        private final List<ItemStack> items = new ArrayList<>(DanPouchComponent.SIZE);

        PouchContainer(ItemStack pouch) {
            this.pouch = pouch;
            var comp = pouch.get(ElixirDataComponents.DanPouch);
            for (int i = 0; i < DanPouchComponent.SIZE; i++) {
                items.add(comp != null ? comp.get(i).copy() : ItemStack.EMPTY);
            }
        }

        @Override
        public int getContainerSize() {
            return DanPouchComponent.SIZE;
        }

        @Override
        public boolean isEmpty() {
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < items.size() ? items.get(index) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            if (index < 0 || index >= items.size() || items.get(index).isEmpty() || count <= 0) return ItemStack.EMPTY;
            var stack = items.get(index).split(count);
            setChanged();
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            if (index < 0 || index >= items.size() || items.get(index).isEmpty()) return ItemStack.EMPTY;
            var stack = items.get(index);
            items.set(index, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (index >= 0 && index < items.size()) {
                items.set(index, stack);
                setChanged();
            }
        }

        @Override
        public void setChanged() {
            if (pouch.isEmpty()) return;
            var comp = pouch.get(ElixirDataComponents.DanPouch);
            int selected = comp == null ? -1 : comp.selected();
            pouch.set(ElixirDataComponents.DanPouch, new DanPouchComponent(selected, items));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
            setChanged();
        }
    }
}

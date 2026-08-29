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
import net.per.elixir.block.entity.LargeFurnaceBlockEntity;

public class LargeFurnaceMenu extends AbstractContainerMenu {
    public static final MenuType<LargeFurnaceMenu> Type = IMenuTypeExtension.create(LargeFurnaceMenu::new);

    public static final int SLOT = 18;
    public static final int VIEW_X = 9, VIEW_Y = 9, VIEW_COLS = 4, VIEW_ROWS = 4;
    public static final int TRACK_X = 84, TRACK_Y = 8, TRACK_W = 12, TRACK_H = 72;
    public static final int OUT_X = 124, OUT_Y = 19, FORMULA_Y = 53;
    private static final int INV_X0 = 9, INV_Y0 = 91, INV_HOTBAR_Y = 149;

    private final ContainerLevelAccess access;
    private final Container inventory;
    private final int size;

    public LargeFurnaceMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf != null ? buf.readInt() : 3);
    }

    public LargeFurnaceMenu(int id, Inventory inv, int size) {
        this(id, inv, new SimpleContainer(LargeFurnaceBlockEntity.containerSizeFor(size)), ContainerLevelAccess.NULL, size);
    }

    public LargeFurnaceMenu(int id, Inventory inv, Container container, ContainerLevelAccess access, int size) {
        super(Type, id);
        this.access = access;
        this.inventory = container;
        this.size = size;
        int matSlots = LargeFurnaceBlockEntity.materialSlotsFor(size);
        for (int i = 0; i < matSlots; i++) {
            int col = i % VIEW_COLS;
            int row = i / VIEW_COLS;
            addSlot(new MaterialSlot(inventory, i, VIEW_X + col * SLOT, VIEW_Y + row * SLOT));
        }
        addSlot(new Slot(inventory, matSlots, OUT_X, OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(inventory, matSlots + 1, OUT_X, FORMULA_Y));
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                addSlot(new Slot(inv, j + (i + 1) * 9, INV_X0 + j * SLOT, INV_Y0 + i * SLOT));
        for (int i = 0; i < 9; ++i)
            addSlot(new Slot(inv, i, INV_X0 + i * SLOT, INV_HOTBAR_Y));
    }

    public static class MaterialSlot extends Slot {
        public MaterialSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }

    public int size() {
        return size;
    }

    public int materialSlotCount() {
        return LargeFurnaceBlockEntity.materialSlotsFor(size);
    }

    public int materialRows() {
        return (materialSlotCount() + VIEW_COLS - 1) / VIEW_COLS;
    }

    public int furnaceSlotCount() {
        return LargeFurnaceBlockEntity.containerSizeFor(size);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var itemStack = ItemStack.EMPTY;
        var slot = slots.get(index);
        if (slot.hasItem()) {
            var stack = slot.getItem();
            itemStack = stack.copy();
            int furnaceSlots = furnaceSlotCount();
            if (index < furnaceSlots) {
                if (!moveItemStackTo(stack, furnaceSlots, slots.size(), false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, furnaceSlots, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof LargeFurnaceBlockEntity be
                && !be.started()
                && canInteractWithFurnace(player, be), true);
    }

    private static boolean canInteractWithFurnace(Player player, LargeFurnaceBlockEntity be) {
        int n = be.size();
        int half = (n - 1) / 2;
        var c = be.getBlockPos();
        double px = Math.clamp(player.getX(), c.getX() - half, c.getX() + half + 1.0);
        double py = Math.clamp(player.getY(), c.getY() - half, c.getY() + half + 1.0);
        double pz = Math.clamp(player.getZ(), c.getZ() - half, c.getZ() + half + 1.0);
        double dx = player.getX() - px, dy = player.getY() - py, dz = player.getZ() - pz;
        return dx * dx + dy * dy + dz * dz <= 36.0;
    }
}

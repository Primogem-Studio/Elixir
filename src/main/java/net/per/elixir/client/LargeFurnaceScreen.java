package net.per.elixir.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.per.elixir.data.LargeFurnaceMenu;

import static net.per.elixir.Elixir.MOD_ID;
import static net.per.elixir.data.LargeFurnaceMenu.*;

public class LargeFurnaceScreen extends AbstractContainerScreen<LargeFurnaceMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/large_furnace.png");
    private static final ResourceLocation BRICK = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/block/elixir_furnace_brick.png");
    private static final ResourceLocation SCROLLBAR = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/large_furnace_scrollbar.png");
    private static final int TEX = 256;
    private static final int PANEL_W = 176, PANEL_H = 174;
    private static final int HANDLE_H = 12;
    private static final int SCROLLBAR_W = TRACK_W - 2, SCROLLBAR_H = HANDLE_H;

    private final int matSlots;
    private final int maxScroll;
    private int scrollOffset;
    private boolean dragging;
    private int dragType;
    private Slot clickSlot;
    private int clickButton;
    private boolean quickCraftHasMat;
    private boolean skipNextRelease;

    public LargeFurnaceScreen(LargeFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_W;
        imageHeight = PANEL_H;
        matSlots = menu.materialSlotCount();
        maxScroll = Math.max(0, menu.materialRows() - VIEW_ROWS);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(BG, x, y, 0, 0, PANEL_W, PANEL_H, TEX, TEX);
        for (int r = 0; r < VIEW_ROWS; r++) {
            int rowIndex = scrollOffset + r;
            int used = matSlots - rowIndex * VIEW_COLS;
            if (used >= VIEW_COLS) continue;
            for (int c = Math.max(0, used); c < VIEW_COLS; c++) {
                g.blit(BRICK, x + VIEW_X - 1 + c * SLOT, y + VIEW_Y - 1 + r * SLOT, SLOT, SLOT, 0, 0, 8, 8, 16, 16);
            }
        }
        int handleY = handleY();
        g.blit(SCROLLBAR, x + TRACK_X + 1, y + handleY, SCROLLBAR_W, SCROLLBAR_H, 0, 0, SCROLLBAR_W, SCROLLBAR_H, SCROLLBAR_W, SCROLLBAR_H);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        updateHoveredSlot(mouseX, mouseY);
        renderMaterialSlots(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderMaterialSlots(GuiGraphics graphics) {
        for (int i = 0; i < matSlots; i++) {
            Slot slot = menu.slots.get(i);
            int y = slot.y - scrollOffset * SLOT;
            if (y + SLOT <= VIEW_Y || y >= VIEW_Y + VIEW_ROWS * SLOT) continue;
            graphics.enableScissor(leftPos + VIEW_X, topPos + VIEW_Y,
                    leftPos + VIEW_X + VIEW_COLS * SLOT, topPos + VIEW_Y + VIEW_ROWS * SLOT);
            if (slot.hasItem()) {
                var stack = slot.getItem();
                graphics.renderItem(stack, leftPos + slot.x, topPos + y);
                graphics.renderItemDecorations(font, stack, leftPos + slot.x, topPos + y);
            }
            if (isQuickCrafting && quickCraftSlots.contains(slot)) {
                graphics.fill(leftPos + slot.x, topPos + y, leftPos + slot.x + 16, topPos + y + 16, 0x40FFAA00);
            } else if (hoveredSlot == slot) {
                graphics.fill(leftPos + slot.x, topPos + y, leftPos + slot.x + 16, topPos + y + 16, 0x80FFFFFF);
            }
            graphics.disableScissor();
        }
    }

    private void updateHoveredSlot(double mouseX, double mouseY) {
        Slot hit = findMatSlot(mouseX, mouseY);
        if (hit != null) {
            hoveredSlot = hit;
        } else if (hoveredSlot != null && hoveredSlot.index < matSlots) {
            hoveredSlot = null;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0 && (isOverView(mouseX, mouseY) || isOverTrack(mouseX, mouseY))) {
            int before = scrollOffset;
            scrollOffset = Math.clamp(scrollOffset - (int) Math.signum(verticalAmount), 0, maxScroll);
            return scrollOffset != before;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll > 0 && isOverTrack(mouseX, mouseY)) {
            dragging = true;
            updateScrollFromMouse(mouseY);
            return true;
        }
        Slot matSlot = findMatSlot(mouseX, mouseY);
        if (matSlot != null) {
            if (isQuickCrafting) return true;
            skipNextRelease = false;
            clickSlot = matSlot;
            clickButton = button;
            if (menu.getCarried().isEmpty()) {
                skipNextRelease = true;
                ClickType type = hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
                slotClicked(matSlot, matSlot.index, button, type);
            } else {
                isQuickCrafting = true;
                quickCraftHasMat = true;
                dragType = button == 0 ? 0 : button == 1 ? 1 : 2;
                quickCraftSlots.clear();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        if (isQuickCrafting) {
            Slot matSlot = findMatSlot(mouseX, mouseY);
            if (matSlot != null && !menu.getCarried().isEmpty()
                    && (dragType == 2 || menu.getCarried().getCount() > quickCraftSlots.size())
                    && AbstractContainerMenu.canItemQuickReplace(matSlot, menu.getCarried(), true)
                    && matSlot.mayPlace(menu.getCarried())
                    && menu.canDragTo(matSlot)) {
                quickCraftSlots.add(matSlot);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        if (isQuickCrafting && quickCraftHasMat && button != clickButton) {
            isQuickCrafting = false;
            quickCraftHasMat = false;
            quickCraftSlots.clear();
            skipNextRelease = true;
            return true;
        }
        if (skipNextRelease) {
            skipNextRelease = false;
            return true;
        }
        if (isQuickCrafting && quickCraftHasMat) {
            isQuickCrafting = false;
            quickCraftHasMat = false;
            if (!quickCraftSlots.isEmpty()) {
                slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(0, dragType), ClickType.QUICK_CRAFT);
                for (Slot slot : quickCraftSlots) {
                    slotClicked(slot, slot.index, AbstractContainerMenu.getQuickcraftMask(1, dragType), ClickType.QUICK_CRAFT);
                }
                slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(2, dragType), ClickType.QUICK_CRAFT);
                quickCraftSlots.clear();
                return true;
            }
            quickCraftSlots.clear();
            Slot target = clickSlot != null ? clickSlot : findMatSlot(mouseX, mouseY);
            if (target != null && !menu.getCarried().isEmpty()) {
                ClickType type = hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
                slotClicked(target, target.index, clickButton, type);
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private Slot findMatSlot(double mouseX, double mouseY) {
        for (int i = 0; i < matSlots; i++) {
            Slot slot = menu.slots.get(i);
            int y = slot.y - scrollOffset * SLOT;
            if (y + SLOT <= VIEW_Y || y >= VIEW_Y + VIEW_ROWS * SLOT) continue;
            if (isHovering(slot.x, y, 16, 16, mouseX, mouseY)) return slot;
        }
        return null;
    }

    private int handleY() {
        if (maxScroll <= 0) return TRACK_Y;
        return TRACK_Y + (int) (scrollOffset * (float) (TRACK_H - HANDLE_H) / maxScroll);
    }

    private void updateScrollFromMouse(double mouseY) {
        float ratio = (float) (mouseY - topPos - TRACK_Y - HANDLE_H / 2f) / (TRACK_H - HANDLE_H);
        scrollOffset = Math.clamp(Math.round(ratio * maxScroll), 0, maxScroll);
    }

    private boolean isOverView(double mx, double my) {
        return mx >= leftPos + VIEW_X && mx < leftPos + VIEW_X + VIEW_COLS * SLOT
                && my >= topPos + VIEW_Y && my < topPos + VIEW_Y + VIEW_ROWS * SLOT;
    }

    private boolean isOverTrack(double mx, double my) {
        return mx >= leftPos + TRACK_X && mx < leftPos + TRACK_X + TRACK_W
                && my >= topPos + TRACK_Y && my < topPos + TRACK_Y + TRACK_H;
    }
}

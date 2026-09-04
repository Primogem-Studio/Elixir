package net.per.elixir.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.per.elixir.data.DanPouchComponent;
import net.per.elixir.registry.ElixirDataComponents;

import java.util.ArrayList;
import java.util.List;

public class DanWheelScreen extends Screen {
    private static final int RING_R = 68;
    private static final int RING_W = 20;
    private static final int HIT = 14;
    private static final int SLOT = 16;
    private static final int MARK = 22;
    private static final int CENTER_H = 24;
    private static final int RING_COLOR = 0x2E808080;
    private static final int CENTER_BG = 0xC033363C;

    private final List<ItemStack> pills;
    private final int fixed;
    private int hover = -1;
    private boolean closed;

    public DanWheelScreen() {
        super(Component.translatable("key.elixir.dan_wheel"));
        var mc = Minecraft.getInstance();
        var pouch = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        var comp = pouch.get(ElixirDataComponents.DanPouch);
        fixed = comp == null ? -1 : comp.selected();
        pills = new ArrayList<>(DanPouchComponent.SIZE);
        for (int i = 0; i < DanPouchComponent.SIZE; i++) {
            pills.add(comp != null && i < comp.items().size() ? comp.get(i) : ItemStack.EMPTY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        updateHover(mouseX, mouseY);
        int cx = width / 2;
        int cy = height / 2;
        drawRing(g, cx, cy, RING_R + RING_W, RING_R - RING_W, RING_COLOR);
        for (int i = 0; i < DanPouchComponent.SIZE; i++) {
            if (pills.get(i).isEmpty()) continue;
            double angle = Math.toRadians(i * 45.0 - 90.0);
            int px = cx + (int) (Math.cos(angle) * RING_R);
            int py = cy + (int) (Math.sin(angle) * RING_R);
            boolean over = hover == i;
            if (over || fixed == i) drawMark(g, px, py, over);
            g.renderItem(pills.get(i), px - SLOT / 2, py - SLOT / 2);
        }
        ItemStack show = hover >= 0 ? pills.get(hover) : fixed >= 0 ? pills.get(fixed) : ItemStack.EMPTY;
        renderCenter(g, cx, cy, show);
    }

    private void drawMark(GuiGraphics g, int cx, int cy, boolean hovered) {
        int fill = hovered ? 0x3F8A8A8A : 0x248A8A8A;
        int edge = hovered ? 0xCFA0A0A0 : 0x6E8A8A8A;
        int x0 = cx - MARK / 2;
        int y0 = cy - MARK / 2;
        g.fill(x0, y0, x0 + MARK, y0 + MARK, fill);
        g.fill(x0, y0, x0 + MARK, y0 + 1, edge);
        g.fill(x0, y0 + MARK - 1, x0 + MARK, y0 + MARK, edge);
        g.fill(x0, y0, x0 + 1, y0 + MARK, edge);
        g.fill(x0 + MARK - 1, y0, x0 + MARK, y0 + MARK, edge);
    }

    private void renderCenter(GuiGraphics g, int cx, int cy, ItemStack show) {
        var desc = show.isEmpty()
                ? Component.translatable("dan_wheel.elixir.none")
                : show.getHoverName();
        int iconW = show.isEmpty() ? 0 : SLOT + 4;
        int textW = font.width(desc);
        int pad = 10;
        int pw = pad * 2 + iconW + textW;
        int px = cx - pw / 2;
        int py = cy - CENTER_H / 2;
        g.fill(px, py, px + pw, py + CENTER_H, CENTER_BG);
        if (!show.isEmpty()) {
            g.renderItem(show, px + pad, py + (CENTER_H - SLOT) / 2);
        }
        g.drawString(font, desc, px + pad + iconW, py + (CENTER_H - font.lineHeight) / 2, 0xFFFFFFFF);
    }

    private void drawRing(GuiGraphics g, int cx, int cy, int rOuter, int rInner, int color) {
        for (int y = -rOuter; y <= rOuter; y++) {
            int o = (int) Math.floor(Math.sqrt(rOuter * rOuter - y * y));
            if (Math.abs(y) >= rInner) {
                g.fill(cx - o, cy + y, cx + o + 1, cy + y + 1, color);
                continue;
            }
            int i = (int) Math.floor(Math.sqrt(rInner * rInner - y * y));
            g.fill(cx - o, cy + y, cx - i, cy + y + 1, color);
            g.fill(cx + i + 1, cy + y, cx + o + 1, cy + y + 1, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            if (hover >= 0) DanWheelClient.sendSelected(hover);
            closeScreen();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            closeScreen();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void commitNow() {
        if (closed) return;
        if (hover >= 0) DanWheelClient.sendSelected(hover);
        closeScreen();
    }

    private void closeScreen() {
        if (closed) return;
        closed = true;
        minecraft.setScreen(null);
    }

    private void updateHover(double mx, double my) {
        hover = indexAt(mx, my);
    }

    private int indexAt(double mx, double my) {
        int cx = width / 2;
        int cy = height / 2;
        double dx = mx - cx;
        double dy = my - cy;
        double dist = Math.hypot(dx, dy);
        if (dist < RING_R - HIT || dist > RING_R + HIT) return -1;
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        if (angle < 0) angle += 360.0;
        int idx = (int) ((angle + 22.5) / 45.0) % 8;
        if (idx < 0) idx += 8;
        return pills.get(idx).isEmpty() ? -1 : idx;
    }
}

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
    private static final int SLOT = 16;
    private static final int CENTER_H = 24;
    private static final int CENTER_DEAD = 30;
    private static final int RING_COLOR = 0x2E808080;
    private static final int CENTER_BG = 0xC033363C;
    private static final int FILL = 0x248A8A8A;
    private static final int FILL_ON = 0x3F8A8A8A;
    private static final int EDGE = 0x6E8A8A8A;
    private static final int EDGE_ON = 0xCFA0A0A0;
    private static final double COS_HALF = Math.cos(Math.PI / 8.0);
    private static final double SIDE_EPS = 0.014;

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
            if (hover == i) drawSector(g, cx, cy, i, true);
            else if (fixed == i) drawSector(g, cx, cy, i, false);
            double angle = Math.toRadians(i * 45.0 - 90.0);
            int px = cx + (int) (Math.cos(angle) * RING_R);
            int py = cy + (int) (Math.sin(angle) * RING_R);
            g.renderItem(pills.get(i), px - SLOT / 2, py - SLOT / 2);
        }
        ItemStack show = hover >= 0 ? pills.get(hover) : fixed >= 0 ? pills.get(fixed) : ItemStack.EMPTY;
        renderCenter(g, cx, cy, show);
    }

    private void drawSector(GuiGraphics g, int cx, int cy, int idx, boolean hovered) {
        int rIn = RING_R - RING_W;
        int rOut = RING_R + RING_W;
        int inner2 = rIn * rIn;
        int outer2 = rOut * rOut;
        int fill = hovered ? FILL_ON : FILL;
        int edge = hovered ? EDGE_ON : EDGE;
        double mid = Math.toRadians(idx * 45.0 - 90.0);
        double cm = Math.cos(mid);
        double sm = Math.sin(mid);
        for (int y = -rOut; y <= rOut; y++) {
            int xo = (int) Math.floor(Math.sqrt(outer2 - y * y));
            int start = -1;
            int color = 0;
            for (int x = -xo; x <= xo; x++) {
                int k = sectorPixel(x, y, cm, sm, inner2, outer2, rIn, rOut);
                if (k == 0) {
                    if (start >= 0) {
                        fillRow(g, cx, cy, y, start, x, color);
                        start = -1;
                        color = 0;
                    }
                    continue;
                }
                int c = k == 2 ? edge : fill;
                if (color == 0) {
                    start = x;
                    color = c;
                } else if (c != color) {
                    fillRow(g, cx, cy, y, start, x, color);
                    start = x;
                    color = c;
                }
            }
            if (start >= 0) fillRow(g, cx, cy, y, start, xo + 1, color);
        }
    }

    private int sectorPixel(int x, int y, double cm, double sm, int inner2, int outer2, int rIn, int rOut) {
        int d2 = x * x + y * y;
        if (d2 < inner2 || d2 > outer2) return 0;
        double c = (x * cm + y * sm) / Math.sqrt(d2);
        if (c < COS_HALF - 0.001) return 0;
        if (c <= COS_HALF + SIDE_EPS) return 2;
        if (d2 >= outer2 - 2 * rOut) return 2;
        if (d2 <= inner2 + 2 * rIn) return 2;
        return 1;
    }

    private void fillRow(GuiGraphics g, int cx, int cy, int y, int x1, int x2, int color) {
        g.fill(cx + x1, cy + y, cx + x2, cy + y + 1, color);
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
        double dx = mx - width / 2.0;
        double dy = my - height / 2.0;
        if (dx * dx + dy * dy < CENTER_DEAD * CENTER_DEAD) return -1;
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        if (angle < 0) angle += 360.0;
        int idx = (int) ((angle + 22.5) / 45.0) % 8;
        if (idx < 0) idx += 8;
        return pills.get(idx).isEmpty() ? -1 : idx;
    }
}

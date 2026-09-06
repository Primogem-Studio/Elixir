package net.per.elixir.client.tdp;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TdpSlider {
    private int x;
    private int y;
    private final int w;
    private int min;
    private int max;
    private int step;
    private int value;
    private boolean hovered;

    public TdpSlider(int x, int y, int w, int min, int max, int step, int value) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = TdpUi.clamped(value, min, max);
    }

    public int w() {
        return w;
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int value() {
        return value;
    }

    public void value(int v) {
        value = TdpUi.clamped(v, min, max);
    }

    public void setFromMouse(double mouseX) {
        float t = (float) ((mouseX - x) / w);
        t = Math.max(0, Math.min(1, t));
        value = min + Math.round(t * (max - min) / (float) step) * step;
        value = TdpUi.clamped(value, min, max);
    }

    public boolean over(double mouseX, double mouseY) {
        return TdpUi.in(mouseX, mouseY, x - 2, y - 3, w + 4, 12);
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, int accent) {
        hovered = over(mouseX, mouseY);
        int ty = y;
        TdpUi.fill(g, x, ty + 3, w, 2, 0xFF3A3D42);
        float t = (max == min) ? 1 : (value - min) / (float) (max - min);
        TdpUi.fill(g, x, ty + 3, Math.max(2, (int) (w * t)), 2, accent);
        int hx = x + (int) (w * t);
        TdpUi.fill(g, hx - 3, ty, 6, 8, hovered ? 0xFFE6EAEF : 0xFF9AA0A8);
        TdpUi.text(g, font, String.valueOf(value), x + w + 5, ty, TdpUi.TEXT);
    }

    public boolean clicked(double mouseX, double mouseY) {
        return over(mouseX, mouseY);
    }
}

package net.per.elixir.client.tdp;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TdpPresetStrip {
    public interface Handler {
        List<String> names();

        void save(String name);

        void delete(String name);

        void apply(String name);
    }

    private static final Component LABEL = Component.translatable("gui.elixir.tdp.preset");
    private static final int FIELD_W = 60;
    private static final int FIELD_H = 12;
    private static final int CHIP_W = 44;
    private static final int CHIP_GAP = 2;

    private final TdpEditField field = new TdpEditField(FIELD_W, FIELD_H);
    private final Handler handler;
    private int scroll;
    private boolean dragging;
    private double pressX;
    private int pressScroll;

    public TdpPresetStrip(Handler handler) {
        this.handler = handler;
    }

    public String name() {
        return field.text();
    }

    public void setName(String s) {
        field.setText(s);
    }

    public void blur() {
        field.clearFocus();
    }

    public void render(GuiGraphics g, Font font, int px, int y, int mx, int my) {
        int x = px + 2 + font.width(LABEL);
        field.setPos(x, y);
        field.render(g, font, mx, my);
        int bx = x + FIELD_W + 3;
        boolean hs = TdpUi.in(mx, my, bx, y - 1, 24, 14);
        TdpUi.button(g, font, bx, y - 1, 24, 14, Component.translatable("gui.elixir.tdp.preset.save"), true, hs);
        int dx = bx + 27;
        boolean hd = TdpUi.in(mx, my, dx, y - 1, 24, 14);
        TdpUi.button(g, font, dx, y - 1, 24, 14, Component.translatable("gui.elixir.tdp.preset.del"), true, hd);
        int cx0 = chipsX(px, font);
        int right = px + TdpScreen.CW - 2;
        var names = handler.names();
        int content = names.size() * (CHIP_W + CHIP_GAP) - CHIP_GAP;
        int max = Math.max(0, content - (right - cx0));
        scroll = Math.clamp(scroll, 0, max);
        if (max > 0) {
            TdpUi.fill(g, cx0, y - 2, right - cx0, 20, 0xFF212328);
            TdpUi.frame(g, cx0, y - 2, right - cx0, 20, 0xFF4C5057);
            g.enableScissor(cx0, y - 1, right, y + 13);
        }
        for (int i = 0; i < names.size(); i++) {
            int chipX = cx0 + i * (CHIP_W + CHIP_GAP) - scroll;
            if (chipX + CHIP_W <= cx0 || chipX >= right) continue;
            boolean h = TdpUi.in(mx, my, chipX, y - 1, CHIP_W, 14);
            TdpUi.button(g, font, chipX, y - 1, CHIP_W, 14,
                    Component.literal(TdpUi.clip(font, names.get(i), CHIP_W - 8)), true, h);
        }
        if (max > 0) {
            g.disableScissor();
            TdpUi.trackH(g, cx0, y + 14, right - cx0, scroll, max, content);
        }
    }

    public boolean click(double mx, double my, int px, int y, Font font) {
        if (onTrack(mx, my, px, y, font)) return true;
        int x = px + 2 + font.width(LABEL);
        int bx = x + FIELD_W + 3;
        if (TdpUi.in(mx, my, bx, y - 1, 24, 14)) {
            handler.save(field.text());
            return true;
        }
        int dx = bx + 27;
        if (TdpUi.in(mx, my, dx, y - 1, 24, 14)) {
            handler.delete(field.text());
            return true;
        }
        int cx0 = chipsX(px, font);
        int right = px + TdpScreen.CW - 2;
        var names = handler.names();
        for (int i = 0; i < names.size(); i++) {
            int chipX = cx0 + i * (CHIP_W + CHIP_GAP) - scroll;
            if (chipX + CHIP_W <= cx0 || chipX >= right) continue;
            if (mx >= cx0 && mx < right && TdpUi.in(mx, my, chipX, y - 1, CHIP_W, 14)) {
                handler.apply(names.get(i));
                return true;
            }
        }
        return field.click(mx, my);
    }

    public boolean drag(double mouseX, int px, Font font) {
        if (!dragging) return false;
        var t = track(px, font);
        if (t == null || t.max <= 0) return true;
        scroll = Math.clamp(pressScroll + (int) ((mouseX - pressX) * t.max / Math.max(1, t.usable)), 0, t.max);
        return true;
    }

    public void release() {
        dragging = false;
    }

    public boolean mouseScrolled(double mx, double my, int px, int y, Font font, int dir) {
        var t = track(px, font);
        if (t == null || t.max <= 0 || !TdpUi.in(mx, my, t.cx0, y - 2, t.w, 20)) return false;
        scroll = Math.clamp(scroll - dir * (CHIP_W + CHIP_GAP), 0, t.max);
        return true;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return field.charTyped(codePoint, modifiers);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return field.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean onTrack(double mx, double my, int px, int y, Font font) {
        var t = track(px, font);
        if (t == null || t.max <= 0) return false;
        if (!TdpUi.in(mx, my, t.cx0 - 4, y + 13, t.w + 8, 5)) return false;
        dragging = true;
        pressX = mx;
        pressScroll = scroll;
        if (t.usable > 0) {
            double target = (mx - t.cx0 - t.thumb / 2.0) / t.usable;
            scroll = Math.clamp((int) (target * t.max), 0, t.max);
        }
        return true;
    }

    private Track track(int px, Font font) {
        int cx0 = chipsX(px, font);
        int right = px + TdpScreen.CW - 2;
        var names = handler.names();
        int content = names.size() * (CHIP_W + CHIP_GAP) - CHIP_GAP;
        int w = right - cx0;
        int max = Math.max(0, content - w);
        if (max <= 0) return null;
        int thumb = Math.max(8, (int) (w * (float) w / Math.max(1, content)));
        if (thumb > w) thumb = w;
        return new Track(cx0, right, content, w, max, thumb, w - thumb);
    }

    private int chipsX(int px, Font font) {
        int x = px + 2 + font.width(LABEL);
        return x + FIELD_W + 3 + 27 + 30;
    }

    private record Track(int cx0, int right, int content, int w, int max, int thumb, int usable) {
    }
}

package net.per.elixir.client.tdp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.per.elixir.client.TextHelper;

import java.util.List;

public final class TdpUi {
    public static final int TEXT = 0xFFC9CFD6;
    public static final int DIM = 0xFF8A8A8A;
    public static final int HINT = 0xFF7A828C;
    public static final int GOLD = 0xFFFFD982;
    public static final int CYAN = 0xFF54D9FF;
    public static final int GREEN = 0xFF7BFF2E;

    private static final int PANEL_BG = 0xE21C1E21;
    private static final int PANEL_EDGE = 0xFF585B60;
    private static final int BTN_BASE = 0xFF41444A;
    private static final int BTN_DISABLED = 0xFF2C2E32;
    private static final int BTN_EDGE = 0xFF66696F;
    private static final int BTN_EDGE_HOVER = 0xFF9AB39F;
    private static final int BTN_EDGE_DISABLED = 0xFF484A4F;
    private static final int FIELD_BG = 0xFF2A2C31;

    private TdpUi() {
    }

    public static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        renderFrame(g, x, y, w, h, color);
    }

    public static void renderFrame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void panel(GuiGraphics g, int ox, int oy, int w, int h) {
        fill(g, ox, oy, w, h, PANEL_BG);
        frame(g, ox, oy, w, h, PANEL_EDGE);
    }

    public static void button(GuiGraphics g, Font font, int x, int y, int w, int h, Component text, boolean enabled, boolean hover) {
        fill(g, x, y, w, h, enabled ? BTN_BASE : BTN_DISABLED);
        int edge = enabled ? (hover ? BTN_EDGE_HOVER : BTN_EDGE) : BTN_EDGE_DISABLED;
        frame(g, x, y, w, h, edge);
        int color = enabled ? (hover ? 0xFFFFFFFF : 0xFFE0E2E5) : 0xFF7C7F84;
        drawCentered(g, font, text, x + w / 2, y + (h - font.lineHeight) / 2 + 1, color);
    }

    public static void segButton(GuiGraphics g, Font font, int x, int y, int w, int h, Component text, boolean selected, boolean hover) {
        fill(g, x, y, w, h, selected ? 0xFF35404F : BTN_BASE);
        frame(g, x, y, w, h, selected ? CYAN : (hover ? BTN_EDGE_HOVER : BTN_EDGE));
        int color = selected ? 0xFFDDF4FF : (hover ? 0xFFFFFFFF : 0xFFD4D8DC);
        drawCentered(g, font, text, x + w / 2, y + (h - font.lineHeight) / 2 + 1, color);
    }

    public static void fieldBox(GuiGraphics g, int x, int y, int w, int h, boolean focused, boolean hover) {
        fill(g, x, y, w, h, FIELD_BG);
        frame(g, x, y, w, h, focused ? CYAN : (hover ? 0xFF8A8E94 : 0xFF55585D));
    }

    public static void text(GuiGraphics g, Font font, Component c, int x, int y, int color) {
        g.drawString(font, c, x, y, color);
    }

    public static void text(GuiGraphics g, Font font, String s, int x, int y, int color) {
        g.drawString(font, s, x, y, color);
    }

    public static void drawCentered(GuiGraphics g, Font font, Component c, int cx, int y, int color) {
        g.drawString(font, c, cx - font.width(c) / 2, y, color);
    }

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static String clip(Font font, String s, int maxW) {
        return font.width(s) <= maxW ? s : font.plainSubstrByWidth(s, maxW);
    }

    public static void hoverCard(GuiGraphics g, Font font, int mouseX, int mouseY, int w, int tint, Component title, List<Component> props, Component desc) {
        hoverCard(g, font, mouseX, mouseY, w, tint, title, props, desc, 4);
    }

    public static void hoverCard(GuiGraphics g, Font font, int mouseX, int mouseY, int w, int tint, Component title, List<Component> props, Component desc, int maxLines) {
        List<String> lines = wrap(font, desc == null ? null : desc.getString(), w - 14, maxLines);
        var titleText = TextHelper.splitLines(font, title, w - 12);
        int pad = 4;
        int lineH = 9;
        int titleH = lineH * titleText.size();
        int h = pad + titleH + props.size() * lineH + lines.size() * lineH + pad + 2;
        var mc = Minecraft.getInstance();
        int x = mouseX + 10;
        int y = mouseY + 8;
        if (mc.screen != null) {
            x = Math.clamp(x, 2, mc.screen.width - w - 2);
            y = Math.clamp(y, 2, mc.screen.height - h - 2);
        }
        var pose = g.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 400.0F);
        fill(g, x, y, w, h, 0xF2101216);
        frame(g, x, y, w, h, tint);
        fill(g, x + 1, y + 1, 1, h - 2, 0x40FFFFFF);
        int ty = y + pad;
        TextHelper.drawWrap(font, g, titleText, x + pad + 2, ty, tint);
        ty += titleH;
        for (var p : props) {
            String pt = TdpUi.clip(font, p.getString(), w - 14);
            g.drawString(font, Component.literal(pt), x + pad + 2, ty, 0xFFD9DEE5);
            ty += lineH;
        }
        for (var line : lines) {
            g.drawString(font, Component.literal(line), x + pad + 2, ty, 0xFFC6CCD4);
            ty += lineH;
        }
        pose.popPose();
    }

    public static List<String> wrap(Font font, String text, int maxWidth, int maxLines) {
        var out = new java.util.ArrayList<String>();
        if (text == null || text.isBlank()) return out;
        String rest = text;
        while (!rest.isEmpty() && out.size() < maxLines) {
            String fit = font.plainSubstrByWidth(rest, maxWidth);
            if (fit.isEmpty()) break;
            out.add(fit);
            rest = rest.substring(fit.length());
        }
        if (!rest.isEmpty() && !out.isEmpty()) {
            String last = out.removeLast();
            String tail = last.length() > 2 ? last.substring(0, last.length() - 2) : "";
            out.add(tail + "…");
        }
        return out;
    }

    public static void tabBar(GuiGraphics g, Font font, int x, int y, int totalW, int selected, String[] keys, int mouseX, int mouseY) {
        int n = keys.length;
        int gap = 3;
        int w = (totalW - gap * (n - 1)) / n;
        for (int i = 0; i < n; i++) {
            int cx = x + i * (w + gap);
            boolean hover = in(mouseX, mouseY, cx, y, w, 15);
            segButton(g, font, cx, y, w, 15, Component.translatable(keys[i]), i == selected, hover);
        }
    }

    public static int tabWidth(int totalW, int n) {
        return (totalW - 3 * (n - 1)) / n;
    }

    public static int tabX(int totalW, int x, int n, int i) {
        return x + i * (tabWidth(totalW, n) + 3);
    }

    public static boolean tabHit(double mouseX, double mouseY, int x, int y, int totalW, int n, int i) {
        return in(mouseX, mouseY, tabX(totalW, x, n, i), y, tabWidth(totalW, n), 15);
    }

    public static int tabAt(double mouseX, double mouseY, int x, int y, int totalW, int n) {
        for (int i = 0; i < n; i++) {
            if (tabHit(mouseX, mouseY, x, y, totalW, n, i)) return i;
        }
        return -1;
    }

    public static void track(GuiGraphics g, int x, int y, int h, int scroll, int max, int visibleRows) {
        if (max <= 0) return;
        fill(g, x, y, 3, h, 0x55FFFFFF);
        int thumb = Math.max(10, h / visibleRows);
        int usable = h - thumb;
        int off = usable <= 0 ? 0 : (int) (scroll * (float) usable / max);
        fill(g, x, y + off, 3, thumb, 0xCCCDD3DA);
    }

    public static void trackH(GuiGraphics g, int x, int y, int w, int scroll, int max, int contentW) {
        if (max <= 0) return;
        fill(g, x, y, w, 3, 0x55FFFFFF);
        int thumb = Math.max(8, (int) (w * (float) w / Math.max(1, contentW)));
        if (thumb > w) thumb = w;
        int usable = w - thumb;
        int off = usable <= 0 ? 0 : (int) (scroll * (float) usable / max);
        fill(g, x + off, y, thumb, 3, 0xCCCDD3DA);
    }

    public static int fieldRow(GuiGraphics g, Font font, TdpBoxField field, Component label, int x, int y, int mx, int my) {
        int lw = font.width(label);
        text(g, font, label, x, y + 2, TdpUi.TEXT);
        field.setPos(x + 2 + lw, y);
        field.render(g, font, mx, my);
        return x + 2 + lw + field.w() + 10;
    }
}

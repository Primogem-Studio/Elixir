package net.per.elixir.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.per.elixir.Elixir;

public final class NineSliceButton {
    public static final int TEXT_COLOR = 0xFFDDF9C2;

    private static final int TEX_W = 20;
    private static final int TEX_H = 25;
    private static final int PRESS_SINK = 1;
    private static final int TEXT_DROP = 1;

    private static final Skin NORMAL = new Skin(texture("u_button"), 2, 2, 2, 6);
    private static final Skin HIGHLIGHT = new Skin(texture("u_button_press"), 2, 2, 2, 4);
    private static final Skin DISABLED = new Skin(texture("u_button_no"), 2, 2, 2, 6);

    private record Skin(ResourceLocation texture, int left, int right, int top, int bottom) {
        int faceW() {
            return TEX_W - left - right;
        }

        int faceH() {
            return TEX_H - top - bottom;
        }
    }

    private NineSliceButton() {
    }

    public static void draw(GuiGraphics g, Font font, int x, int y, int w, int h, Component text,
                            boolean enabled, boolean hover) {
        draw(g, font, x, y, w, h, text, enabled, hover, false);
    }

    public static void draw(GuiGraphics g, Font font, int x, int y, int w, int h, Component text,
                            boolean enabled, boolean hover, boolean selected) {
        Skin s = enabled ? (hover || selected ? HIGHLIGHT : NORMAL) : DISABLED;
        int bottom = fit(s, h, font.lineHeight);
        skin(g, s, x, y, w, h, bottom);
        int faceW = Math.max(1, w - s.left() - s.right());
        int faceH = Math.max(0, h - s.top() - bottom);
        int sink = enabled && hover && leftPressed() ? PRESS_SINK : 0;
        int cx = x + s.left() + (faceW - font.width(text)) / 2;
        int cy = y + s.top() + (faceH - font.lineHeight) / 2 + TEXT_DROP + sink;
        g.drawString(font, text, cx, cy, TEXT_COLOR);
    }

    private static int fit(Skin s, int h, int lineHeight) {
        return Math.max(0, Math.min(s.bottom(), h - s.top() - lineHeight));
    }

    private static boolean leftPressed() {
        var mc = Minecraft.getInstance();
        return mc.mouseHandler != null && mc.mouseHandler.isLeftPressed();
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Elixir.MOD_ID, "textures/gui/widget/" + name + ".png");
    }

    private static void skin(GuiGraphics g, Skin s, int x, int y, int w, int h, int bottomInset) {
        int l = s.left();
        int r = s.right();
        int t = s.top();
        int b = Math.min(bottomInset, s.bottom());
        int cw = Math.max(0, w - l - r);
        int ch = Math.max(0, h - t - b);
        int sw = s.faceW();
        int sh = s.faceH();
        int bandV = TEX_H - s.bottom();
        int right = x + w - r;
        int bottom = y + h - b;
        ResourceLocation tex = s.texture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (ch > 0) {
            g.blit(tex, x, y + t, l, ch, 0, t, l, sh, TEX_W, TEX_H);
            g.blit(tex, right, y + t, r, ch, TEX_W - r, t, r, sh, TEX_W, TEX_H);
        }
        if (cw > 0) {
            g.blit(tex, x + l, y, cw, t, l, 0, sw, t, TEX_W, TEX_H);
            g.blit(tex, x + l, bottom, cw, b, l, bandV, sw, b, TEX_W, TEX_H);
        }
        if (cw > 0 && ch > 0) {
            g.blit(tex, x + l, y + t, cw, ch, l, t, sw, sh, TEX_W, TEX_H);
        }
        g.blit(tex, x, y, l, t, 0, 0, l, t, TEX_W, TEX_H);
        g.blit(tex, right, y, r, t, TEX_W - r, 0, r, t, TEX_W, TEX_H);
        g.blit(tex, x, bottom, l, b, 0, bandV, l, b, TEX_W, TEX_H);
        g.blit(tex, right, bottom, r, b, TEX_W - r, bandV, r, b, TEX_W, TEX_H);
    }
}

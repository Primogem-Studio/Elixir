package net.per.elixir.client.tdp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class TdpEditField {
    private final int w;
    private final int h;
    private Component placeholder;
    private EditBox box;

    public TdpEditField(int w, int h) {
        this.w = w;
        this.h = h;
    }

    private EditBox box() {
        if (box == null) {
            box = new EditBox(Minecraft.getInstance().font, 0, 0, w, h, Component.literal(""));
            box.setBordered(false);
            box.setTextColor(0xFFF2F4F7);
            box.setMaxLength(64);
        }
        return box;
    }

    public int w() {
        return w;
    }

    public int h() {
        return h;
    }

    public void setPos(int x, int y) {
        box().setX(x);
        box().setY(y);
    }

    public void placeholder(Component c) {
        placeholder = c;
    }

    public void setText(String s) {
        box().setValue(s == null ? "" : s);
    }

    public String text() {
        return box().getValue();
    }

    public void clearFocus() {
        if (box != null) box.setFocused(false);
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY) {
        var b = box();
        boolean hover = TdpUi.in(mouseX, mouseY, b.getX(), b.getY(), w, h);
        TdpUi.fieldBox(g, b.getX(), b.getY(), w, h, b.isFocused(), hover);
        String shown;
        int color;
        if (text().isEmpty() && !b.isFocused() && placeholder != null) {
            shown = placeholder.getString();
            color = 0xFF6E7278;
        } else {
            shown = text();
            color = 0xFFF2F4F7;
        }
        TdpUi.text(g, font, TdpUi.clip(font, shown, w - 8), b.getX() + 4, b.getY() + (h - font.lineHeight) / 2 + 1, color);
    }

    public boolean click(double mouseX, double mouseY) {
        var b = box();
        boolean inside = TdpUi.in(mouseX, mouseY, b.getX(), b.getY(), w, h);
        if (inside) {
            b.setFocused(true);
            b.mouseClicked(mouseX, mouseY, 0);
        } else {
            b.setFocused(false);
        }
        return inside;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        var b = box();
        if (!b.isFocused()) return false;
        return b.charTyped(codePoint, modifiers);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        var b = box();
        if (!b.isFocused()) return false;
        return b.keyPressed(keyCode, scanCode, modifiers);
    }
}

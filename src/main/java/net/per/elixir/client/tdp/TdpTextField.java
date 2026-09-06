package net.per.elixir.client.tdp;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class TdpTextField {
    public enum Mode {
        TEXT, INT, DECIMAL
    }

    private int x;
    private int y;
    private final int w;
    private final int h;
    private final Mode mode;
    private String text = "";
    private boolean focused;
    private boolean hovered;
    private Component placeholder;

    public TdpTextField(int x, int y, int w, int h, Mode mode) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.mode = mode;
    }

    public int w() {
        return w;
    }

    public int h() {
        return h;
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void placeholder(Component c) {
        this.placeholder = c;
    }

    public void setText(String s) {
        this.text = s == null ? "" : s;
    }

    public String text() {
        return text;
    }

    public void clearFocus() {
        focused = false;
    }

    public void focus() {
        focused = true;
    }

    public boolean focused() {
        return focused;
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY) {
        hovered = TdpUi.in(mouseX, mouseY, x, y, w, h);
        TdpUi.fieldBox(g, x, y, w, h, focused, hovered);
        String shown;
        if (text.isEmpty() && !focused && placeholder != null) {
            TdpUi.text(g, font, placeholder, x + 4, y + (h - font.lineHeight) / 2 + 1, 0xFF6E7278);
            return;
        }
        shown = TdpUi.clip(font, text, w - 8);
        TdpUi.text(g, font, shown, x + 4, y + (h - font.lineHeight) / 2 + 1, 0xFFF2F4F7);
    }

    public boolean click(double mouseX, double mouseY) {
        focused = TdpUi.in(mouseX, mouseY, x, y, w, h);
        return focused;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!focused) return false;
        if (text.length() >= 40) return true;
        if (!matches(codePoint)) return true;
        text = text + codePoint;
        return true;
    }

    private boolean matches(char c) {
        if (mode == Mode.TEXT) return true;
        if (Character.isDigit(c)) return true;
        if (c == '-' && mode == Mode.INT && !text.contains("-")) return true;
        return c == '.' && mode == Mode.DECIMAL && !text.contains(".");
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            focused = false;
            return true;
        }
        return true;
    }

    public int intValue(int fallback) {
        try {
            return (int) Math.round(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public double doubleValue(double fallback) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

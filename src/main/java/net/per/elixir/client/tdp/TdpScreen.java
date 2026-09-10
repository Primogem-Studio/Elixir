package net.per.elixir.client.tdp;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.per.elixir.client.tdp.pages.PillGenPage;
import net.per.elixir.client.tdp.pages.FormulaGenPage;

import java.util.ArrayList;
import java.util.List;

public class TdpScreen extends Screen {
    public static final int PANEL_W = 320;
    public static final int PANEL_H = 252;
    public static final int CX = 10;
    public static final int CY = 40;
    public static final int CW = PANEL_W - CX * 2;
    public static final int CH = PANEL_H - CY - 6;
    public static final int TAB_H = 18;

    private final List<TdpPage> pages = new ArrayList<>();
    private int selected;

    public Font font() {
        return font;
    }

    private record Pending(Component title, List<Component> props, Component desc, int w, int tint,
                           int mouseX, int mouseY) {
    }

    private Pending pending;

    public void queueHover(int mouseX, int mouseY, int w, int tint, Component title,
                           List<Component> props, Component desc) {
        pending = new Pending(title, props, desc, w, tint, mouseX, mouseY);
    }

    private void flushHover(GuiGraphics g) {
        if (pending == null) return;
        TdpUi.hoverCard(g, font, pending.mouseX(), pending.mouseY(), pending.w(), pending.tint(),
                pending.title(), pending.props(), pending.desc());
        pending = null;
    }

    public TdpScreen() {
        super(Component.translatable("gui.elixir.tdp.title"));
        addPage(new PillGenPage());
        addPage(new FormulaGenPage());
        showPage(0);
    }

    public void addPage(TdpPage page) {
        pages.add(page);
    }

    public void showPage(int index) {
        if (index < 0 || index >= pages.size()) return;
        selected = index;
        pages.get(index).show(this);
        pages.get(index).refresh(this);
    }

    public TdpPage currentPage() {
        return pages.get(selected);
    }

    public int pageCount() {
        return pages.size();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public int ox() {
        return (width - PANEL_W) / 2;
    }

    public int oy() {
        return (height - PANEL_H) / 2;
    }

    public void copyToClipboard(String s) {
        minecraft.keyboardHandler.setClipboard(s);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int ox = ox();
        int oy = oy();
        TdpUi.panel(g, ox, oy, PANEL_W, PANEL_H);
        g.drawString(font, Component.translatable("gui.elixir.tdp.title"), ox + CX, oy + 5, 0xFFE9E9E9);
        g.drawString(font, Component.translatable(pages.get(selected).titleKey()), ox + PANEL_W - CX - font.width(Component.translatable(pages.get(selected).titleKey())), oy + 7, 0xFFB4BAC2);
        drawTabs(g, mouseX, mouseY, ox, oy);
        TdpUi.fill(g, ox + CX, oy + CY - 2, CW, 1, 0xFF33363B);
        pages.get(selected).render(this, g, mouseX, mouseY, partialTick);
        flushHover(g);
    }

    private void drawTabs(GuiGraphics g, int mouseX, int mouseY, int ox, int oy) {
        int tabY = oy + 20;
        int gap = 2;
        int tabW = (CW - gap * (pages.size() - 1)) / pages.size();
        for (int i = 0; i < pages.size(); i++) {
            int x = ox + CX + i * (tabW + gap);
            boolean hover = TdpUi.in(mouseX, mouseY, x, tabY, tabW, TAB_H);
            TdpUi.segButton(g, font, x, tabY, tabW, TAB_H, Component.translatable(pages.get(i).titleKey()), i == selected, hover);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tabY = oy() + 20;
            int tabW = (CW - 2 * (pages.size() - 1)) / pages.size();
            for (int i = 0; i < pages.size(); i++) {
                int x = ox() + CX + i * (tabW + 2);
                if (TdpUi.in(mouseX, mouseY, x, tabY, tabW, TAB_H)) {
                    showPage(i);
                    return true;
                }
            }
        }
        if (pages.get(selected).mouseClicked(this, mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (pages.get(selected).mouseReleased(this, mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (pages.get(selected).mouseDragged(this, mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (pages.get(selected).mouseScrolled(this, mouseX, mouseY, verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (pages.get(selected).charTyped(this, codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        if (pages.get(selected).keyPressed(this, keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }
}

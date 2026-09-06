package net.per.elixir.client.tdp.pages;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.per.elixir.client.tdp.TdpData;
import net.per.elixir.client.tdp.TdpGrid;
import net.per.elixir.client.tdp.TdpPage;
import net.per.elixir.client.tdp.TdpPickState;
import net.per.elixir.client.tdp.TdpPresets;
import net.per.elixir.client.tdp.TdpPresetStrip;
import net.per.elixir.client.tdp.TdpScreen;
import net.per.elixir.client.tdp.TdpUi;
import net.per.elixir.registry.data.Material;

import java.util.List;

public abstract class TdpPickerPage implements TdpPage {
    protected static final int TAB_Y = 0;
    protected static final int BODY_Y = 17;
    protected static final int SEARCH_Y = 16;
    protected static final int GRID_Y = 36;
    protected static final int PREVIEW_Y = 124;
    protected static final int FIELDS_Y = 136;
    protected static final int PRESET_Y = 152;
    protected static final int BUTTONS_Y = 171;
    protected static final int STATUS_Y = 196;
    protected static final int CELL = 21;
    protected static final int COLS = 14;
    protected static final int GRID_ROWS = 4;
    protected static final int LIST_ROWS = 6;
    protected static final int ROW_H = 16;
    private static final String[] VIEWS = {"gui.elixir.tdp.pick.main", "gui.elixir.tdp.pick.off", "gui.elixir.tdp.tab.selected"};

    protected int view;
    protected int listScroll;
    protected String status = "";
    protected int statusAge;
    protected final TdpPickState pick;
    protected final TdpGrid<Holder<Material>> mainGrid;
    protected final TdpGrid<Holder<Material>> offGrid;
    protected final TdpPresetStrip presets;

    protected TdpPickerPage(boolean keepEmptyOff) {
        pick = new TdpPickState(keepEmptyOff);
        mainGrid = new TdpGrid<>(CELL, CELL, COLS, GRID_ROWS, pick.mains());
        offGrid = new TdpGrid<>(CELL, CELL, COLS, 1, pick.offs());
        presets = new TdpPresetStrip(new TdpPresetStrip.Handler() {
            @Override
            public List<String> names() {
                return TdpPresets.names(category());
            }

            @Override
            public void save(String name) {
                if (!name.isEmpty()) savePreset(name);
            }

            @Override
            public void delete(String name) {
                if (name.isEmpty()) return;
                TdpPresets.remove(category(), name);
                presets.setName("");
                flash(Component.translatable("gui.elixir.tdp.preset.deleted").getString());
            }

            @Override
            public void apply(String name) {
                presets.setName(name);
                applyPreset(name);
            }
        });
    }

    protected abstract String category();

    protected abstract boolean mainSelected(Holder<Material> m);

    protected abstract boolean offSelected(Holder<Material> m);

    protected abstract void onMainPick(Holder<Material> m);

    protected abstract void onOffPick(Holder<Material> m);

    protected abstract void drawListPane(GuiGraphics g, Font font, int px, int py, int mx, int my);

    protected abstract boolean listRowClick(double mouseX, double mouseY, int px, int py);

    protected abstract void drawFixed(TdpScreen host, GuiGraphics g, Font font, int px, int py, int mx, int my);

    protected abstract boolean fixedClick(TdpScreen host, double mouseX, double mouseY, int px, int py);

    protected abstract void hoverList(TdpScreen host, GuiGraphics g, int px, int py, int mouseX, int mouseY);

    protected abstract void blurFields();

    protected abstract boolean clickField(double mouseX, double mouseY);

    protected abstract boolean charField(char codePoint, int modifiers);

    protected abstract boolean keyField(int keyCode, int scanCode, int modifiers);

    protected abstract void savePreset(String name);

    protected abstract void applyPreset(String name);

    protected void flash(String s) {
        status = s;
        statusAge = 80;
    }

    @Override
    public void refresh(TdpScreen host) {
        pick.reload();
        status = "";
        statusAge = 0;
    }

    @Override
    public void render(TdpScreen host, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int px = host.ox() + TdpScreen.CX;
        int py = host.oy() + TdpScreen.CY;
        var font = host.font();
        pick.apply();
        TdpUi.tabBar(g, font, px, py + TAB_Y, TdpScreen.CW, view, VIEWS, mouseX, mouseY);
        if (view < 2) {
            pick.search().setPos(px + 2, py + SEARCH_Y);
            pick.search().render(g, font, mouseX, mouseY);
        }
        if (view == 0) drawMainGrid(g, font, px, py, mouseX, mouseY);
        else if (view == 1) drawOffGrid(g, font, px, py, mouseX, mouseY);
        else drawListPane(g, font, px, py, mouseX, mouseY);
        drawFixed(host, g, font, px, py, mouseX, mouseY);
        if (statusAge > 0) {
            TdpUi.text(g, font, status, px + 2, py + STATUS_Y, TdpUi.HINT);
            statusAge--;
        }
        queueHover(host, g, px, py, mouseX, mouseY);
    }

    private void drawMainGrid(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int x0 = px + 1;
        int y0 = py + GRID_Y;
        mainGrid.render(g, font, x0, y0, mx, my,
                this::mainSelected,
                (gr, f, x, y, w, h, hov, sel, m, mx2, my2) -> TdpData.drawCell(gr, x, y, w, m, hov, sel, TdpUi.CYAN));
        mainGrid.drawScrollbar(g, x0 + COLS * CELL + 5, y0, mainGrid.visibleHeight());
    }

    private void drawOffGrid(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int x0 = px + 1;
        int y0 = py + GRID_Y;
        offGrid.render(g, font, x0, y0, mx, my,
                this::offSelected,
                (gr, f, x, y, w, h, hov, sel, m, mx2, my2) -> TdpData.drawCell(gr, x, y, w, m, hov, sel, TdpUi.GREEN));
        offGrid.drawScrollbar(g, x0 + COLS * CELL + 5, y0, offGrid.visibleHeight());
    }

    private void queueHover(TdpScreen host, GuiGraphics g, int px, int py, int mouseX, int mouseY) {
        if (view == 0) {
            var m = mainGrid.hoveredItem();
            if (m != null) TdpData.hoverMaterial(host, g, m, mouseX, mouseY);
        } else if (view == 1) {
            var m = offGrid.hoveredItem();
            if (m != null) TdpData.hoverMaterial(host, g, m, mouseX, mouseY);
        } else {
            hoverList(host, g, px, py, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(TdpScreen host, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int px = host.ox() + TdpScreen.CX;
        int py = host.oy() + TdpScreen.CY;
        int hit = TdpUi.tabAt(mouseX, mouseY, px, py + TAB_Y, TdpScreen.CW, VIEWS.length);
        if (hit >= 0) {
            view = hit;
            return true;
        }
        if (view == 0) {
            int idx = mainGrid.indexAt(mouseX, mouseY, px + 1, py + GRID_Y);
            if (idx >= 0) {
                onMainPick(pick.mains().get(idx));
                return true;
            }
        } else if (view == 1) {
            int idx = offGrid.indexAt(mouseX, mouseY, px + 1, py + GRID_Y);
            if (idx >= 0) {
                onOffPick(pick.offs().get(idx));
                return true;
            }
        } else if (listRowClick(mouseX, mouseY, px, py)) {
            return true;
        }
        if (fixedClick(host, mouseX, mouseY, px, py)) return true;
        blurAll();
        if (view < 2 && pick.search().click(mouseX, mouseY)) return true;
        if (clickField(mouseX, mouseY)) return true;
        presets.click(mouseX, mouseY, px, py + PRESET_Y, host.font());
        return true;
    }

    private void blurAll() {
        pick.search().clearFocus();
        presets.blur();
        blurFields();
    }

    @Override
    public boolean mouseScrolled(TdpScreen host, double mouseX, double mouseY, double amount) {
        int px = host.ox() + TdpScreen.CX;
        int py = host.oy() + TdpScreen.CY;
        int dir = amount > 0 ? 1 : -1;
        if (view == 0 && mainGrid.contains(mouseX, mouseY, px + 1, py + GRID_Y)) {
            mainGrid.scroll(dir);
            return true;
        }
        if (view == 1 && offGrid.contains(mouseX, mouseY, px + 1, py + GRID_Y)) {
            offGrid.scroll(dir);
            return true;
        }
        if (view == 2 && TdpUi.in(mouseX, mouseY, px + 1, py + BODY_Y, TdpScreen.CW, LIST_ROWS * ROW_H)) {
            listScroll -= dir;
            return true;
        }
        return presets.mouseScrolled(mouseX, mouseY, px, py + PRESET_Y, host.font(), dir);
    }

    @Override
    public boolean mouseDragged(TdpScreen host, double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) return false;
        int px = host.ox() + TdpScreen.CX;
        int py = host.oy() + TdpScreen.CY;
        return presets.drag(mouseX, px, host.font());
    }

    @Override
    public boolean mouseReleased(TdpScreen host, double mouseX, double mouseY, int button) {
        presets.release();
        return false;
    }

    @Override
    public boolean charTyped(TdpScreen host, char codePoint, int modifiers) {
        if (pick.search().charTyped(codePoint, modifiers)) return true;
        if (charField(codePoint, modifiers)) return true;
        return presets.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(TdpScreen host, int keyCode, int scanCode, int modifiers) {
        if (pick.search().keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyField(keyCode, scanCode, modifiers)) return true;
        return presets.keyPressed(keyCode, scanCode, modifiers);
    }
}

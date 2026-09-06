package net.per.elixir.client.tdp;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.Predicate;

public class TdpGrid<T> {
    public interface CellRenderer<T> {
        void draw(GuiGraphics g, Font font, int x, int y, int w, int h, boolean hovered, boolean selected, T item, int mouseX, int mouseY);
    }

    private final int cellW;
    private final int cellH;
    private final int cols;
    private final int rowsVisible;
    private final List<T> items;
    private int scrollRows;
    private int hovered = -1;

    public TdpGrid(int cellW, int cellH, int cols, int rowsVisible, List<T> items) {
        this.cellW = cellW;
        this.cellH = cellH;
        this.cols = cols;
        this.rowsVisible = rowsVisible;
        this.items = items;
    }

    public int rowsTotal() {
        return (items.size() + cols - 1) / cols;
    }

    public int maxScroll() {
        return Math.max(0, rowsTotal() - rowsVisible);
    }

    public int visibleHeight() {
        return rowsVisible * cellH;
    }

    public int cellW() {
        return cellW;
    }

    public int cellH() {
        return cellH;
    }

    public int hovered() {
        return hovered;
    }

    public T hoveredItem() {
        return hovered < 0 ? null : items.get(hovered);
    }

    public List<T> items() {
        return items;
    }

    public void scroll(int delta) {
        scrollRows -= delta;
        scrollRows = TdpUi.clamped(scrollRows, 0, maxScroll());
    }

    public void render(GuiGraphics g, Font font, int x0, int y0, int mouseX, int mouseY,
                       Predicate<T> selected, CellRenderer<T> renderer) {
        scrollRows = TdpUi.clamped(scrollRows, 0, maxScroll());
        hovered = -1;
        for (int r = 0; r < rowsVisible; r++) {
            int row = scrollRows + r;
            for (int c = 0; c < cols; c++) {
                int idx = row * cols + c;
                if (idx >= items.size()) break;
                int x = x0 + c * cellW;
                int y = y0 + r * cellH;
                boolean hov = TdpUi.in(mouseX, mouseY, x, y, cellW, cellH);
                boolean sel = selected.test(items.get(idx));
                if (hov) hovered = idx;
                renderer.draw(g, font, x, y, cellW, cellH, hov, sel, items.get(idx), mouseX, mouseY);
            }
        }
    }

    public int indexAt(double mouseX, double mouseY, int x0, int y0) {
        int col = (int) ((mouseX - x0) / cellW);
        int row = (int) ((mouseY - y0) / cellH);
        if (col < 0 || col >= cols || row < 0 || row >= rowsVisible) return -1;
        int idx = (TdpUi.clamped(scrollRows, 0, maxScroll()) + row) * cols + col;
        return idx >= 0 && idx < items.size() ? idx : -1;
    }

    public boolean contains(double mouseX, double mouseY, int x0, int y0) {
        return TdpUi.in(mouseX, mouseY, x0, y0, cols * cellW, rowsVisible * cellH);
    }

    public void drawScrollbar(GuiGraphics g, int x, int y, int h) {
        TdpUi.track(g, x, y, h, scrollRows, maxScroll(), rowsVisible);
    }
}

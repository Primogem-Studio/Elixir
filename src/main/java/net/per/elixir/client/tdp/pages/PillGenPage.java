package net.per.elixir.client.tdp.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.per.elixir.client.tdp.TdpData;
import net.per.elixir.client.tdp.TdpGrid;
import net.per.elixir.client.tdp.TdpPage;
import net.per.elixir.client.tdp.TdpPresets;
import net.per.elixir.client.tdp.TdpScreen;
import net.per.elixir.client.tdp.TdpTextField;
import net.per.elixir.client.tdp.TdpUi;
import net.per.elixir.network.TdpCraftPillPayload;
import net.per.elixir.registry.data.Material;
import net.per.elixir.util.ElixirMath;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class PillGenPage implements TdpPage {
    private static final int MAX_MAIN = 8;
    private static final int TAB_Y = 0;
    private static final int BODY_Y = 17;
    private static final int PREVIEW_Y = 124;
    private static final int FIELDS_Y = 136;
    private static final int PRESET_Y = 152;
    private static final int BUTTONS_Y = 171;
    private static final int STATUS_Y = 196;
    private static final String[] VIEWS = {"gui.elixir.tdp.pick.main", "gui.elixir.tdp.pick.off", "gui.elixir.tdp.tab.selected"};
    private static final int CELL = 21;
    private static final int COLS = 13;
    private static final int GRID_ROWS = 5;
    private static final int LIST_ROWS = 6;
    private static final int ROW_H = 16;

    private int view;
    private int selScroll;
    private final LinkedHashSet<String> selectedMains = new LinkedHashSet<>();
    private String offId = "";
    private boolean pharmManual;
    private final List<Holder<Material>> mains = new ArrayList<>();
    private final List<Holder<Material>> offs = new ArrayList<>();
    private final TdpGrid<Holder<Material>> mainGrid = new TdpGrid<>(CELL, CELL, COLS, GRID_ROWS, mains);
    private final TdpGrid<Holder<Material>> offGrid = new TdpGrid<>(CELL, CELL, COLS, 1, offs);
    private final TdpTextField pharm = new TdpTextField(0, 0, 50, 13, TdpTextField.Mode.INT);
    private final TdpTextField count = new TdpTextField(0, 0, 34, 13, TdpTextField.Mode.INT);
    private final TdpTextField pillName = new TdpTextField(0, 0, 88, 13, TdpTextField.Mode.TEXT);
    private final TdpTextField presetName = new TdpTextField(0, 0, 60, 12, TdpTextField.Mode.TEXT);
    private String status = "";
    private int statusAge;

    public PillGenPage() {
        pharm.setText("0");
        count.setText("1");
    }

    @Override
    public String titleKey() {
        return "gui.elixir.tdp.tab.pill";
    }

    @Override
    public void refresh(TdpScreen host) {
        mains.clear();
        mains.addAll(TdpData.materials(true));
        offs.clear();
        offs.addAll(TdpData.materials(false));
        for (String id : List.copyOf(selectedMains)) {
            if (TdpData.byId(id) == null) selectedMains.remove(id);
        }
        if (TdpData.byId(offId) == null) {
            for (var o : offs) {
                if (TdpData.isEmpty(o)) {
                    offId = TdpData.id(o);
                    break;
                }
            }
        }
        if (!pharmManual) pharm.setText(String.valueOf(sumPharm()));
        status = "";
        statusAge = 0;
    }

    private int sumPharm() {
        int sum = 0;
        for (var id : selectedMains) {
            var m = TdpData.byId(id);
            if (m != null) sum += m.value().pharm();
        }
        return sum;
    }

    private boolean hasSelection() {
        return !selectedMains.isEmpty();
    }

    private Holder<Material> selectedOff() {
        if (!offId.isEmpty()) {
            var h = TdpData.byId(offId);
            if (h != null) return h;
        }
        for (var o : offs) {
            if (TdpData.isEmpty(o)) return o;
        }
        return offs.isEmpty() ? null : offs.get(0);
    }

    @Override
    public void render(TdpScreen host, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int px = host.ox() + TdpScreen.CX;
        int py = host.oy() + TdpScreen.CY;
        var font = host.font();
        TdpUi.tabBar(g, font, px, py + TAB_Y, TdpScreen.CW, view, VIEWS, mouseX, mouseY);
        if (view == 0) drawMainGrid(g, font, px, py, mouseX, mouseY);
        else if (view == 1) drawOffGrid(g, font, px, py, mouseX, mouseY);
        else drawSelection(g, font, px, py, mouseX, mouseY);
        drawFixed(host, g, font, px, py, mouseX, mouseY);
        if (statusAge > 0) {
            TdpUi.text(g, font, status, px + 2, py + STATUS_Y, TdpUi.HINT);
            statusAge--;
        }
        drawHover(host, g, mouseX, mouseY);
    }

    private void drawMainGrid(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int x0 = px + 1;
        int y0 = py + BODY_Y;
        mainGrid.render(g, font, x0, y0, mx, my,
                m -> selectedMains.contains(TdpData.id(m)),
                (gr, f, x, y, w, h, hov, sel, m, mx2, my2) -> TdpData.drawCell(gr, x, y, w, m, hov, sel, TdpUi.CYAN));
        mainGrid.drawScrollbar(g, x0 + COLS * CELL + 5, y0, mainGrid.visibleHeight());
    }

    private void drawOffGrid(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int x0 = px + 1;
        int y0 = py + BODY_Y;
        offGrid.render(g, font, x0, y0, mx, my,
                m -> TdpData.id(m).equals(offId),
                (gr, f, x, y, w, h, hov, sel, m, mx2, my2) -> TdpData.drawCell(gr, x, y, w, m, hov, sel, TdpUi.GREEN));
        offGrid.drawScrollbar(g, x0 + COLS * CELL + 5, y0, offGrid.visibleHeight());
    }

    private void drawSelection(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int x0 = px + 1;
        int y0 = py + BODY_Y;
        var rows = currentRows();
        int max = Math.max(0, rows.size() - LIST_ROWS);
        selScroll = TdpUi.clamped(selScroll, 0, max);
        for (int i = 0; i < LIST_ROWS; i++) {
            int idx = selScroll + i;
            if (idx >= rows.size()) break;
            var row = rows.get(idx);
            int y = y0 + i * ROW_H;
            var m = row.m();
            boolean off = row.isOff();
            g.renderItem(TdpData.icon(m), x0, y + 1);
            if (TdpData.isEmpty(m)) {
                TdpUi.fill(g, x0 + 2, y + 3, 12, 12, 0xAA2C2E33);
                TdpUi.frame(g, x0 + 2, y + 3, 12, 12, 0xFF55585D);
            }
            g.drawString(font, off ? "\u8F85" : "\u4E3B", x0 + 20, y + 5, off ? TdpUi.GREEN : TdpUi.CYAN);
            String name = TdpUi.clip(font, TdpData.name(m).getString(), 220);
            g.drawString(font, name, x0 + 32, y + 5, 0xFFC9CFD6);
            boolean hx = TdpUi.in(mx, my, px + TdpScreen.CW - 13, y, 13, ROW_H);
            g.drawString(font, "x", px + TdpScreen.CW - 10, y + 5, hx ? 0xFFFF8A80 : 0xFF9AA0A8);
        }
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable("gui.elixir.tdp.list.empty").getString(), x0, y0 + 8, TdpUi.DIM);
        }
        TdpUi.track(g, px + TdpScreen.CW - 9, y0, LIST_ROWS * ROW_H, selScroll, max, LIST_ROWS);
    }

    private record RowLine(Holder<Material> m, boolean isOff) {
    }

    private List<RowLine> currentRows() {
        var rows = new ArrayList<RowLine>();
        var off = selectedOff();
        if (off != null && !TdpData.isEmpty(off)) rows.add(new RowLine(off, true));
        for (var id : selectedMains) {
            var m = TdpData.byId(id);
            if (m != null) rows.add(new RowLine(m, false));
        }
        return rows;
    }

    private void drawFixed(TdpScreen host, GuiGraphics g, Font font, int px, int py, int mx, int my) {
        var off = selectedOff();
        if (off != null) {
            int fin = ElixirMath.finalPharm(off, pharm.intValue(0));
            TdpUi.text(g, font, Component.translatable("gui.elixir.tdp.final.pharm", fin), px + 2, py + PREVIEW_Y, TdpUi.GOLD);
        } else {
            TdpUi.text(g, font, Component.translatable("gui.elixir.tdp.no.off"), px + 2, py + PREVIEW_Y, TdpUi.DIM);
        }
        int y = py + FIELDS_Y;
        int x = px + 2;
        x = TdpUi.fieldRow(g, font, pharm, Component.translatable("gui.elixir.tdp.pharm"), x, y, mx, my);
        x = TdpUi.fieldRow(g, font, count, Component.translatable("gui.elixir.tdp.count"), x, y, mx, my);
        TdpUi.fieldRow(g, font, pillName, Component.translatable("gui.elixir.tdp.pill.name"), x, y, mx, my);
        drawPresetBar(g, font, px, py, mx, my);
        drawButtons(g, font, px, py, mx, my);
    }

    private void drawPresetBar(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int y = py + PRESET_Y;
        int x = px + 2 + font.width(Component.translatable("gui.elixir.tdp.preset"));
        presetName.setPos(x, y);
        presetName.render(g, font, mx, my);
        int bx = x + presetName.w() + 3;
        boolean hs = TdpUi.in(mx, my, bx, y - 1, 24, 14);
        TdpUi.button(g, font, bx, y - 1, 24, 14, Component.translatable("gui.elixir.tdp.preset.save"), true, hs);
        int dx = bx + 27;
        boolean hd = TdpUi.in(mx, my, dx, y - 1, 24, 14);
        TdpUi.button(g, font, dx, y - 1, 24, 14, Component.translatable("gui.elixir.tdp.preset.del"), true, hd);
        int cx0 = dx + 30;
        var names = TdpPresets.names("pill");
        int chipW = 44;
        for (int i = 0; i < names.size(); i++) {
            int chipX = cx0 + i * (chipW + 2);
            if (chipX + chipW > px + TdpScreen.CW - 2) break;
            boolean h = TdpUi.in(mx, my, chipX, y - 1, chipW, 14);
            TdpUi.button(g, font, chipX, y - 1, chipW, 14, Component.literal(TdpUi.clip(font, names.get(i), chipW - 8)), true, h);
        }
    }

    private void drawButtons(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        boolean ok = hasSelection() && selectedOff() != null && count.intValue(0) >= 1;
        int by = py + BUTTONS_Y;
        boolean h1 = TdpUi.in(mx, my, px + 2, by, 118, 18);
        TdpUi.button(g, font, px + 2, by, 118, 18, Component.translatable("gui.elixir.tdp.gen.pill", Math.max(1, count.intValue(1))), ok, h1);
        boolean h2 = TdpUi.in(mx, my, px + 124, by, 118, 18);
        TdpUi.button(g, font, px + 124, by, 118, 18, Component.translatable("gui.elixir.tdp.copy.cmd"), ok, h2);
        boolean h3 = TdpUi.in(mx, my, px + 246, by, 50, 18);
        TdpUi.button(g, font, px + 246, by, 50, 18, Component.translatable("gui.elixir.tdp.clear"), hasSelection(), h3);
    }

    private void drawHover(TdpScreen host, GuiGraphics g, int mouseX, int mouseY) {
        int px = host.ox() + TdpScreen.CX;
        int py = host.oy() + TdpScreen.CY;
        int y0 = py + BODY_Y;
        if (view == 0) {
            var m = mainGrid.hoveredItem();
            if (m != null) TdpData.hoverMaterial(host, g, m, mouseX, mouseY);
        } else if (view == 1) {
            var m = offGrid.hoveredItem();
            if (m != null) TdpData.hoverMaterial(host, g, m, mouseX, mouseY);
        } else {
            var rows = currentRows();
            int row = (int) ((mouseY - y0) / ROW_H);
            if (row >= 0 && row < LIST_ROWS) {
                int idx = selScroll + row;
                if (idx >= 0 && idx < rows.size()) TdpData.hoverMaterial(host, g, rows.get(idx).m(), mouseX, mouseY);
            }
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
            int idx = mainGrid.indexAt(mouseX, mouseY, px + 1, py + BODY_Y);
            if (idx >= 0) {
                toggleMain(mains.get(idx));
                return true;
            }
        } else if (view == 1) {
            int idx = offGrid.indexAt(mouseX, mouseY, px + 1, py + BODY_Y);
            if (idx >= 0) {
                offId = TdpData.id(offs.get(idx));
                return true;
            }
        } else {
            var rows = currentRows();
            int r = (int) ((mouseY - py - BODY_Y) / ROW_H);
            if (r >= 0 && r < LIST_ROWS) {
                int idx = selScroll + r;
                if (idx >= 0 && idx < rows.size()) {
                    if (mouseX >= px + TdpScreen.CW - 14) {
                        var row = rows.get(idx);
                        if (row.isOff()) offId = "";
                        else selectedMains.remove(TdpData.id(row.m()));
                        if (!pharmManual) pharm.setText(String.valueOf(sumPharm()));
                    }
                    return true;
                }
            }
        }
        int by = py + BUTTONS_Y;
        boolean ok = hasSelection() && selectedOff() != null && count.intValue(0) >= 1;
        if (ok && TdpUi.in(mouseX, mouseY, px + 2, by, 118, 18)) {
            sendGenerate();
            return true;
        }
        if (ok && TdpUi.in(mouseX, mouseY, px + 124, by, 118, 18)) {
            host.copyToClipboard(serialize());
            status = Component.translatable("gui.elixir.tdp.copied").getString();
            statusAge = 80;
            return true;
        }
        if (hasSelection() && TdpUi.in(mouseX, mouseY, px + 246, by, 50, 18)) {
            selectedMains.clear();
            if (!pharmManual) pharm.setText("0");
            return true;
        }
        blurFields();
        if (pharm.click(mouseX, mouseY)) return true;
        if (count.click(mouseX, mouseY)) return true;
        if (pillName.click(mouseX, mouseY)) return true;
        if (presetName.click(mouseX, mouseY)) return true;
        handlePresetButtons(host, mouseX, mouseY);
        return true;
    }

    private void blurFields() {
        pharm.clearFocus();
        count.clearFocus();
        pillName.clearFocus();
        presetName.clearFocus();
    }

    private void handlePresetButtons(TdpScreen host, double mouseX, double mouseY) {
        int px = host.ox() + TdpScreen.CX;
        int py = host.oy() + TdpScreen.CY;
        int y = py + PRESET_Y;
        int x = px + 2 + host.font().width(Component.translatable("gui.elixir.tdp.preset"));
        int bx = x + presetName.w() + 3;
        if (TdpUi.in(mouseX, mouseY, bx, y - 1, 24, 14)) {
            savePreset();
            return;
        }
        int dx = bx + 27;
        if (TdpUi.in(mouseX, mouseY, dx, y - 1, 24, 14)) {
            if (!presetName.text().isEmpty()) {
                TdpPresets.remove("pill", presetName.text());
                presetName.setText("");
                status = Component.translatable("gui.elixir.tdp.preset.deleted").getString();
                statusAge = 80;
            }
            return;
        }
        int cx0 = dx + 30;
        var names = TdpPresets.names("pill");
        int chipW = 44;
        for (int i = 0; i < names.size(); i++) {
            int chipX = cx0 + i * (chipW + 2);
            if (chipX + chipW > px + TdpScreen.CW - 2) break;
            if (TdpUi.in(mouseX, mouseY, chipX, y - 1, chipW, 14)) {
                applyPreset(names.get(i));
                return;
            }
        }
    }

    private void toggleMain(Holder<Material> m) {
        String id = TdpData.id(m);
        if (!selectedMains.remove(id) && selectedMains.size() < MAX_MAIN) {
            selectedMains.add(id);
        }
        if (!pharmManual) pharm.setText(String.valueOf(sumPharm()));
    }

    private void sendGenerate() {
        var off = selectedOff();
        if (off == null) return;
        int n = Math.max(1, Math.min(512, count.intValue(1)));
        String name = pillName.text().trim();
        PacketDistributor.sendToServer(new TdpCraftPillPayload(n, pharm.intValue(0), TdpData.id(off),
                new ArrayList<>(selectedMains), name.isEmpty() ? null : name));
    }

    private String serialize() {
        var off = selectedOff();
        var sb = new StringBuilder("elixir:elixir[elixir:elixir={pharm:").append(pharm.intValue(0));
        if (off != null) sb.append(",off:\"").append(TdpData.id(off)).append('"');
        sb.append(",main:[");
        boolean first = true;
        for (var id : selectedMains) {
            if (!first) sb.append(',');
            sb.append('"').append(id).append('"');
            first = false;
        }
        sb.append("]}]");
        return sb.toString();
    }

    private void savePreset() {
        String name = presetName.text();
        if (name.isEmpty()) return;
        var o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("pharm", pharm.intValue(0));
        o.addProperty("count", count.intValue(1));
        if (!pillName.text().isEmpty()) o.addProperty("pillName", pillName.text());
        var off = selectedOff();
        o.addProperty("off", off == null ? "" : TdpData.id(off));
        var main = new JsonArray();
        for (var id : selectedMains) main.add(id);
        o.add("main", main);
        TdpPresets.put("pill", o);
        status = Component.translatable("gui.elixir.tdp.preset.saved", name).getString();
        statusAge = 80;
    }

    private void applyPreset(String name) {
        var o = TdpPresets.get("pill", name);
        if (o == null) return;
        presetName.setText(name);
        selectedMains.clear();
        var main = o.getAsJsonArray("main");
        if (main != null) {
            for (var e : main) {
                String id = e.getAsString();
                if (selectedMains.size() < MAX_MAIN && TdpData.byId(id) != null) selectedMains.add(id);
            }
        }
        if (o.has("off")) offId = o.get("off").getAsString();
        if (o.has("pharm")) pharm.setText(String.valueOf(o.get("pharm").getAsInt()));
        if (o.has("count")) count.setText(String.valueOf(o.get("count").getAsInt()));
        if (o.has("pillName")) pillName.setText(o.get("pillName").getAsString());
    }

    @Override
    public boolean mouseScrolled(TdpScreen host, double mouseX, double mouseY, double amount) {
        int px = host.ox() + TdpScreen.CX;
        int py = host.oy() + TdpScreen.CY;
        int dir = amount > 0 ? 1 : -1;
        int y0 = py + BODY_Y;
        if (view == 0 && mainGrid.contains(mouseX, mouseY, px + 1, y0)) {
            mainGrid.scroll(dir);
            return true;
        }
        if (view == 1 && offGrid.contains(mouseX, mouseY, px + 1, y0)) {
            offGrid.scroll(dir);
            return true;
        }
        if (view == 2 && TdpUi.in(mouseX, mouseY, px + 1, y0, TdpScreen.CW, LIST_ROWS * ROW_H)) {
            selScroll -= dir;
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(TdpScreen host, char codePoint, int modifiers) {
        if (pharm.charTyped(codePoint, modifiers)) {
            pharmManual = true;
            return true;
        }
        return count.charTyped(codePoint, modifiers)
                || pillName.charTyped(codePoint, modifiers)
                || presetName.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(TdpScreen host, int keyCode, int scanCode, int modifiers) {
        return pharm.keyPressed(keyCode, scanCode, modifiers)
                || count.keyPressed(keyCode, scanCode, modifiers)
                || pillName.keyPressed(keyCode, scanCode, modifiers)
                || presetName.keyPressed(keyCode, scanCode, modifiers);
    }
}

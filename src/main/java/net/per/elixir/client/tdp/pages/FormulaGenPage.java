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
import net.per.elixir.client.tdp.TdpSlider;
import net.per.elixir.client.tdp.TdpTextField;
import net.per.elixir.client.tdp.TdpUi;
import net.per.elixir.network.TdpCraftFormulaPayload;
import net.per.elixir.registry.data.Material;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FormulaGenPage implements TdpPage {
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
    private int listScroll;
    private String draggingId;
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final List<Holder<Material>> mains = new ArrayList<>();
    private final List<Holder<Material>> offs = new ArrayList<>();
    private final TdpGrid<Holder<Material>> mainGrid = new TdpGrid<>(CELL, CELL, COLS, GRID_ROWS, mains);
    private final TdpGrid<Holder<Material>> offGrid = new TdpGrid<>(CELL, CELL, COLS, 1, offs);
    private final List<RowLine> rows = new ArrayList<>();
    private final TdpTextField name = new TdpTextField(0, 0, 118, 13, TdpTextField.Mode.TEXT);
    private final TdpTextField quantity = new TdpTextField(0, 0, 34, 13, TdpTextField.Mode.INT);
    private final TdpTextField presetName = new TdpTextField(0, 0, 60, 12, TdpTextField.Mode.TEXT);
    private String status = "";
    private int statusAge;

    private record RowLine(Holder<Material> m, TdpSlider slider) {
    }

    public FormulaGenPage() {
        name.setText("\u4E39\u65B9 - \u65B0\u4E39");
        quantity.setText("1");
    }

    @Override
    public String titleKey() {
        return "gui.elixir.tdp.tab.formula";
    }

    @Override
    public void refresh(TdpScreen host) {
        mains.clear();
        mains.addAll(TdpData.materials(true));
        offs.clear();
        for (var m : TdpData.materials(false)) {
            if (!TdpData.isEmpty(m)) offs.add(m);
        }
        for (String id : List.copyOf(counts.keySet())) {
            if (TdpData.byId(id) == null) counts.remove(id);
        }
        rebuildRows();
        status = "";
        statusAge = 0;
    }

    private void rebuildRows() {
        rows.clear();
        for (var e : counts.entrySet()) {
            var m = TdpData.byId(e.getKey());
            if (m != null) rows.add(new RowLine(m, new TdpSlider(0, 0, 46, 1, 64, 1, e.getValue())));
        }
    }

    @Override
    public void render(TdpScreen host, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int px = host.ox() + TdpScreen.CX;
        int py = host.oy() + TdpScreen.CY;
        var font = host.font();
        TdpUi.tabBar(g, font, px, py + TAB_Y, TdpScreen.CW, view, VIEWS, mouseX, mouseY);
        if (view == 0) drawMainGrid(g, font, px, py, mouseX, mouseY);
        else if (view == 1) drawOffGrid(g, font, px, py, mouseX, mouseY);
        else drawList(g, font, px, py, mouseX, mouseY);
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
                m -> counts.containsKey(TdpData.id(m)),
                (gr, f, x, y, w, h, hov, sel, m, mx2, my2) -> TdpData.drawCell(gr, x, y, w, m, hov, sel, TdpUi.CYAN));
        mainGrid.drawScrollbar(g, x0 + COLS * CELL + 5, y0, mainGrid.visibleHeight());
    }

    private void drawOffGrid(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int x0 = px + 1;
        int y0 = py + BODY_Y;
        offGrid.render(g, font, x0, y0, mx, my,
                m -> counts.containsKey(TdpData.id(m)),
                (gr, f, x, y, w, h, hov, sel, m, mx2, my2) -> TdpData.drawCell(gr, x, y, w, m, hov, sel, TdpUi.GREEN));
        offGrid.drawScrollbar(g, x0 + COLS * CELL + 5, y0, offGrid.visibleHeight());
    }

    private void drawList(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int y0 = py + BODY_Y;
        int max = Math.max(0, rows.size() - LIST_ROWS);
        listScroll = TdpUi.clamped(listScroll, 0, max);
        for (int i = 0; i < LIST_ROWS; i++) {
            int idx = listScroll + i;
            if (idx >= rows.size()) break;
            var row = rows.get(idx);
            int y = y0 + i * ROW_H;
            var m = row.m();
            boolean off = !m.value().main();
            g.renderItem(TdpData.icon(m), px + 1, y + 1);
            g.drawString(font, off ? "\u8F85" : "\u4E3B", px + 19, y + 5, off ? TdpUi.GREEN : TdpUi.CYAN);
            String sname = TdpUi.clip(font, TdpData.name(m).getString(), 92);
            g.drawString(font, sname, px + 31, y + 5, 0xFFC9CFD6);
            var slider = row.slider();
            slider.setPos(px + 128, y + 4);
            slider.render(g, font, mx, my, TdpUi.CYAN);
            boolean hx = TdpUi.in(mx, my, px + TdpScreen.CW - 13, y, 13, ROW_H);
            g.drawString(font, "x", px + TdpScreen.CW - 10, y + 5, hx ? 0xFFFF8A80 : 0xFF9AA0A8);
        }
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable("gui.elixir.tdp.list.empty").getString(), px + 2, y0 + 10, TdpUi.DIM);
        }
        TdpUi.track(g, px + TdpScreen.CW - 9, y0, LIST_ROWS * ROW_H, listScroll, max, LIST_ROWS);
    }

    private void drawFixed(TdpScreen host, GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int y = py + FIELDS_Y;
        int x = px + 2;
        x = TdpUi.fieldRow(g, font, name, Component.translatable("gui.elixir.tdp.formula.name"), x, y, mx, my);
        TdpUi.fieldRow(g, font, quantity, Component.translatable("gui.elixir.tdp.count"), x, y, mx, my);
        String hint = Component.translatable("gui.elixir.tdp.formula.serial.hint").getString();
        TdpUi.text(g, font, hint, px + 2, py + PREVIEW_Y, TdpUi.HINT);
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
        var names = TdpPresets.names("formula");
        int chipW = 44;
        for (int i = 0; i < names.size(); i++) {
            int chipX = cx0 + i * (chipW + 2);
            if (chipX + chipW > px + TdpScreen.CW - 2) break;
            boolean h = TdpUi.in(mx, my, chipX, y - 1, chipW, 14);
            TdpUi.button(g, font, chipX, y - 1, chipW, 14, Component.literal(TdpUi.clip(font, names.get(i), chipW - 8)), true, h);
        }
    }

    private void drawButtons(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        boolean ok = hasMain() && quantity.intValue(0) >= 1;
        int by = py + BUTTONS_Y;
        boolean h1 = TdpUi.in(mx, my, px + 2, by, 118, 18);
        TdpUi.button(g, font, px + 2, by, 118, 18, Component.translatable("gui.elixir.tdp.gen.formula", Math.max(1, quantity.intValue(1))), ok, h1);
        boolean h2 = TdpUi.in(mx, my, px + 124, by, 118, 18);
        TdpUi.button(g, font, px + 124, by, 118, 18, Component.translatable("gui.elixir.tdp.copy.cmd"), ok, h2);
        boolean h3 = TdpUi.in(mx, my, px + 246, by, 50, 18);
        TdpUi.button(g, font, px + 246, by, 50, 18, Component.translatable("gui.elixir.tdp.clear"), !counts.isEmpty(), h3);
    }

    private boolean hasMain() {
        for (var id : counts.keySet()) {
            var m = TdpData.byId(id);
            if (m != null && m.value().main()) return true;
        }
        return false;
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
            int row = (int) ((mouseY - y0) / ROW_H);
            if (row >= 0 && row < LIST_ROWS) {
                int idx = listScroll + row;
                if (idx >= 0 && idx < rows.size()) {
                    if (mouseX < px + TdpScreen.CW - 16) {
                        TdpData.hoverMaterial(host, g, rows.get(idx).m(), mouseX, mouseY);
                    }
                }
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
                toggle(mains.get(idx));
                return true;
            }
        } else if (view == 1) {
            int idx = offGrid.indexAt(mouseX, mouseY, px + 1, py + BODY_Y);
            if (idx >= 0) {
                toggle(offs.get(idx));
                return true;
            }
        } else {
            int row = (int) ((mouseY - py - BODY_Y) / ROW_H);
            if (row >= 0 && row < LIST_ROWS) {
                int idx = listScroll + row;
                if (idx >= 0 && idx < rows.size()) {
                    var r = rows.get(idx);
                    if (TdpUi.in(mouseX, mouseY, px + 128, py + BODY_Y + row * ROW_H + 2, 60, 12)) {
                        r.slider().setFromMouse(mouseX);
                        draggingId = TdpData.id(r.m());
                        sync(r);
                        return true;
                    }
                    if (mouseX >= px + TdpScreen.CW - 14) {
                        counts.remove(TdpData.id(r.m()));
                        rebuildRows();
                        return true;
                    }
                    return true;
                }
            }
        }
        int by = py + BUTTONS_Y;
        boolean ok = hasMain() && quantity.intValue(0) >= 1;
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
        if (!counts.isEmpty() && TdpUi.in(mouseX, mouseY, px + 246, by, 50, 18)) {
            counts.clear();
            rebuildRows();
            return true;
        }
        name.clearFocus();
        quantity.clearFocus();
        presetName.clearFocus();
        if (name.click(mouseX, mouseY)) return true;
        if (quantity.click(mouseX, mouseY)) return true;
        if (presetName.click(mouseX, mouseY)) return true;
        handlePresetButtons(host, mouseX, mouseY);
        return true;
    }

    private void sync(RowLine row) {
        counts.put(TdpData.id(row.m()), row.slider().value());
    }

    @Override
    public boolean mouseDragged(TdpScreen host, double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingId != null) {
            for (var r : rows) {
                if (TdpData.id(r.m()).equals(draggingId)) {
                    r.slider().setFromMouse(mouseX);
                    sync(r);
                    break;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(TdpScreen host, double mouseX, double mouseY, int button) {
        if (button == 0) draggingId = null;
        return false;
    }

    private void toggle(Holder<Material> m) {
        String id = TdpData.id(m);
        if (counts.containsKey(id)) counts.remove(id);
        else counts.put(id, 1);
        rebuildRows();
    }

    private void sendGenerate() {
        var mains = new ArrayList<TdpCraftFormulaPayload.Content>();
        var offs = new ArrayList<TdpCraftFormulaPayload.Content>();
        for (var e : counts.entrySet()) {
            var m = TdpData.byId(e.getKey());
            if (m == null) continue;
            var content = new TdpCraftFormulaPayload.Content(e.getKey(), e.getValue());
            if (m.value().main()) mains.add(content);
            else offs.add(content);
        }
        if (mains.isEmpty()) return;
        String n = name.text().trim();
        PacketDistributor.sendToServer(new TdpCraftFormulaPayload(n.isEmpty() ? null : n,
                Math.max(1, Math.min(64, quantity.intValue(1))), mains, offs));
    }

    private String serialize() {
        var sb = new StringBuilder("minecraft:paper[elixir:alchemical_formula={main:[");
        boolean first = true;
        for (var e : counts.entrySet()) {
            var m = TdpData.byId(e.getKey());
            if (m == null || !m.value().main()) continue;
            if (!first) sb.append(',');
            sb.append("{material:\"").append(e.getKey()).append("\",count:").append(e.getValue()).append('}');
            first = false;
        }
        sb.append("],off:[");
        first = true;
        for (var e : counts.entrySet()) {
            var m = TdpData.byId(e.getKey());
            if (m == null || m.value().main()) continue;
            if (!first) sb.append(',');
            sb.append("{material:\"").append(e.getKey()).append("\",count:").append(e.getValue()).append('}');
            first = false;
        }
        sb.append("]}");
        String n = name.text().trim();
        if (!n.isEmpty()) {
            String esc = n.replace("\\", "\\\\").replace("\"", "\\\"");
            sb.append(",minecraft:custom_name:'{\"text\":\"").append(esc).append("\"}'");
        }
        sb.append(']');
        return sb.toString();
    }

    private void savePreset() {
        String n = presetName.text();
        if (n.isEmpty()) return;
        var o = new JsonObject();
        o.addProperty("name", n);
        if (!name.text().isEmpty()) o.addProperty("label", name.text());
        o.addProperty("quantity", quantity.intValue(1));
        var main = new JsonArray();
        var off = new JsonArray();
        for (var e : counts.entrySet()) {
            var m = TdpData.byId(e.getKey());
            if (m == null) continue;
            var c = new JsonObject();
            c.addProperty("id", e.getKey());
            c.addProperty("count", e.getValue());
            (m.value().main() ? main : off).add(c);
        }
        o.add("main", main);
        o.add("off", off);
        TdpPresets.put("formula", o);
        status = Component.translatable("gui.elixir.tdp.preset.saved", n).getString();
        statusAge = 80;
    }

    private void applyPreset(String preset) {
        var o = TdpPresets.get("formula", preset);
        if (o == null) return;
        presetName.setText(preset);
        counts.clear();
        for (var key : new String[]{"main", "off"}) {
            var arr = o.getAsJsonArray(key);
            if (arr == null) continue;
            for (var e : arr) {
                var c = e.getAsJsonObject();
                String id = c.get("id").getAsString();
                if (TdpData.byId(id) != null) counts.put(id, c.get("count").getAsInt());
            }
        }
        if (o.has("quantity")) quantity.setText(String.valueOf(o.get("quantity").getAsInt()));
        if (o.has("label")) name.setText(o.get("label").getAsString());
        rebuildRows();
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
                TdpPresets.remove("formula", presetName.text());
                presetName.setText("");
                status = Component.translatable("gui.elixir.tdp.preset.deleted").getString();
                statusAge = 80;
            }
            return;
        }
        int cx0 = dx + 30;
        var names = TdpPresets.names("formula");
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
            listScroll -= dir;
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(TdpScreen host, char codePoint, int modifiers) {
        return name.charTyped(codePoint, modifiers)
                || quantity.charTyped(codePoint, modifiers)
                || presetName.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(TdpScreen host, int keyCode, int scanCode, int modifiers) {
        return name.keyPressed(keyCode, scanCode, modifiers)
                || quantity.keyPressed(keyCode, scanCode, modifiers)
                || presetName.keyPressed(keyCode, scanCode, modifiers);
    }
}

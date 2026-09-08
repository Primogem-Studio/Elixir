package net.per.elixir.client.tdp.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.per.elixir.client.tdp.TdpData;
import net.per.elixir.client.tdp.TdpEditField;
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

public class FormulaGenPage extends TdpPickerPage {
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final List<RowLine> rows = new ArrayList<>();
    private final TdpEditField name = new TdpEditField(118, 13);
    private final TdpTextField quantity = new TdpTextField(0, 0, 34, 13, TdpTextField.Mode.INT);
    private String draggingId;

    private record RowLine(Holder<Material> m, TdpSlider slider) {
    }

    public FormulaGenPage() {
        super(false);
        name.setText("\u4E39\u65B9 - \u65B0\u4E39");
        quantity.setText("1");
    }

    @Override
    public String titleKey() {
        return "gui.elixir.tdp.tab.formula";
    }

    @Override
    protected String category() {
        return "formula";
    }

    @Override
    public void refresh(TdpScreen host) {
        super.refresh(host);
        for (String id : List.copyOf(counts.keySet())) {
            if (TdpData.byId(id) == null) counts.remove(id);
        }
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        for (var e : counts.entrySet()) {
            var m = TdpData.byId(e.getKey());
            if (m != null) rows.add(new RowLine(m, new TdpSlider(0, 0, 46, 1, 64, 1, e.getValue())));
        }
    }

    @Override
    protected boolean mainSelected(Holder<Material> m) {
        return counts.containsKey(TdpData.id(m));
    }

    @Override
    protected boolean offSelected(Holder<Material> m) {
        return counts.containsKey(TdpData.id(m));
    }

    @Override
    protected void onMainPick(Holder<Material> m) {
        toggle(m);
    }

    @Override
    protected void onOffPick(Holder<Material> m) {
        toggle(m);
    }

    private void toggle(Holder<Material> m) {
        String id = TdpData.id(m);
        if (counts.containsKey(id)) counts.remove(id);
        else counts.put(id, 1);
        rebuildRows();
    }

    private boolean hasMain() {
        for (var id : counts.keySet()) {
            var m = TdpData.byId(id);
            if (m != null && m.value().main()) return true;
        }
        return false;
    }

    @Override
    protected void drawListPane(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int y0 = py + BODY_Y;
        int max = Math.max(0, rows.size() - LIST_ROWS);
        listScroll = Math.clamp(listScroll, 0, max);
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

    @Override
    protected boolean listRowClick(double mx, double my, int px, int py) {
        int row = (int) ((my - py - BODY_Y) / ROW_H);
        if (row < 0 || row >= LIST_ROWS) return false;
        int idx = listScroll + row;
        if (idx < 0 || idx >= rows.size()) return false;
        var r = rows.get(idx);
        if (TdpUi.in(mx, my, px + 128, py + BODY_Y + row * ROW_H + 2, 60, 12)) {
            r.slider().setFromMouse(mx);
            draggingId = TdpData.id(r.m());
            sync(r);
        } else if (mx >= px + TdpScreen.CW - 14) {
            counts.remove(TdpData.id(r.m()));
            rebuildRows();
        }
        return true;
    }

    private void sync(RowLine row) {
        counts.put(TdpData.id(row.m()), row.slider().value());
    }

    @Override
    protected void hoverList(TdpScreen host, GuiGraphics g, int px, int py, int mouseX, int mouseY) {
        int row = (int) ((mouseY - py - BODY_Y) / ROW_H);
        if (row < 0 || row >= LIST_ROWS || mouseX >= px + TdpScreen.CW - 16) return;
        int idx = listScroll + row;
        if (idx >= 0 && idx < rows.size()) TdpData.hoverMaterial(host, g, rows.get(idx).m(), mouseX, mouseY);
    }

    @Override
    protected void drawFixed(TdpScreen host, GuiGraphics g, Font font, int px, int py, int mx, int my) {
        String hint = Component.translatable("gui.elixir.tdp.formula.serial.hint").getString();
        TdpUi.text(g, font, hint, px + 2, py + PREVIEW_Y, TdpUi.HINT);
        int y = py + FIELDS_Y;
        int x = px + 2;
        x = TdpUi.fieldRow(g, font, name, Component.translatable("gui.elixir.tdp.formula.name"), x, y, mx, my);
        TdpUi.fieldRow(g, font, quantity, Component.translatable("gui.elixir.tdp.count"), x, y, mx, my);
        presets.render(g, font, px, py + PRESET_Y, mx, my);
        drawButtons(g, font, px, py, mx, my);
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

    @Override
    protected boolean fixedClick(TdpScreen host, double mx, double my, int px, int py) {
        boolean ok = hasMain() && quantity.intValue(0) >= 1;
        int by = py + BUTTONS_Y;
        if (ok && TdpUi.in(mx, my, px + 2, by, 118, 18)) {
            sendGenerate();
            return true;
        }
        if (ok && TdpUi.in(mx, my, px + 124, by, 118, 18)) {
            host.copyToClipboard(serialize());
            flash(Component.translatable("gui.elixir.tdp.copied").getString());
            return true;
        }
        if (!counts.isEmpty() && TdpUi.in(mx, my, px + 246, by, 50, 18)) {
            counts.clear();
            rebuildRows();
            return true;
        }
        return false;
    }

    @Override
    protected void blurFields() {
        name.clearFocus();
        quantity.clearFocus();
    }

    @Override
    protected boolean clickField(double mouseX, double mouseY) {
        if (name.click(mouseX, mouseY)) return true;
        return quantity.click(mouseX, mouseY);
    }

    @Override
    protected boolean charField(char codePoint, int modifiers) {
        if (name.charTyped(codePoint, modifiers)) return true;
        return quantity.charTyped(codePoint, modifiers);
    }

    @Override
    protected boolean keyField(int keyCode, int scanCode, int modifiers) {
        if (name.keyPressed(keyCode, scanCode, modifiers)) return true;
        return quantity.keyPressed(keyCode, scanCode, modifiers);
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

    @Override
    protected void savePreset(String n) {
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
        flash(Component.translatable("gui.elixir.tdp.preset.saved", n).getString());
    }

    @Override
    protected void applyPreset(String preset) {
        var o = TdpPresets.get("formula", preset);
        if (o == null) return;
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
        return super.mouseDragged(host, mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(TdpScreen host, double mouseX, double mouseY, int button) {
        if (button == 0) draggingId = null;
        return super.mouseReleased(host, mouseX, mouseY, button);
    }
}

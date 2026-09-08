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
import net.per.elixir.client.tdp.TdpTextField;
import net.per.elixir.client.tdp.TdpUi;
import net.per.elixir.network.TdpCraftPillPayload;
import net.per.elixir.registry.data.Material;
import net.per.elixir.util.ElixirMath;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class PillGenPage extends TdpPickerPage {
    private final LinkedHashSet<String> selectedMains = new LinkedHashSet<>();
    private final TdpTextField pharm = new TdpTextField(0, 0, 50, 13, TdpTextField.Mode.INT);
    private final TdpTextField count = new TdpTextField(0, 0, 34, 13, TdpTextField.Mode.INT);
    private final TdpEditField pillName = new TdpEditField(88, 13);
    private String offId = "";
    private boolean pharmManual;

    private record RowLine(Holder<Material> m, boolean isOff) {
    }

    public PillGenPage() {
        super(true);
        pharm.setText("0");
        count.setText("1");
    }

    @Override
    public String titleKey() {
        return "gui.elixir.tdp.tab.pill";
    }

    @Override
    protected String category() {
        return "pill";
    }

    @Override
    public void refresh(TdpScreen host) {
        super.refresh(host);
        for (String id : List.copyOf(selectedMains)) {
            if (TdpData.byId(id) == null) selectedMains.remove(id);
        }
        if (TdpData.byId(offId) == null) {
            for (var o : TdpData.materials(false)) {
                if (TdpData.isEmpty(o)) {
                    offId = TdpData.id(o);
                    break;
                }
            }
        }
        if (!pharmManual) pharm.setText(String.valueOf(sumPharm()));
    }

    private int sumPharm() {
        int sum = 0;
        for (var id : selectedMains) {
            var m = TdpData.byId(id);
            if (m != null) sum += m.value().pharm();
        }
        return sum;
    }

    private Holder<Material> selectedOff() {
        if (!offId.isEmpty()) {
            var h = TdpData.byId(offId);
            if (h != null) return h;
        }
        for (var o : TdpData.materials(false)) {
            if (TdpData.isEmpty(o)) return o;
        }
        var all = TdpData.materials(false);
        return all.isEmpty() ? null : all.getFirst();
    }

    private boolean hasSelection() {
        return !selectedMains.isEmpty();
    }

    @Override
    protected boolean mainSelected(Holder<Material> m) {
        return selectedMains.contains(TdpData.id(m));
    }

    @Override
    protected boolean offSelected(Holder<Material> m) {
        return TdpData.id(m).equals(offId);
    }

    @Override
    protected void onMainPick(Holder<Material> m) {
        toggleMain(m);
    }

    @Override
    protected void onOffPick(Holder<Material> m) {
        offId = TdpData.id(m);
    }

    private void toggleMain(Holder<Material> m) {
        String id = TdpData.id(m);
        if (!selectedMains.remove(id)) selectedMains.add(id);
        if (!pharmManual) pharm.setText(String.valueOf(sumPharm()));
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

    @Override
    protected void drawListPane(GuiGraphics g, Font font, int px, int py, int mx, int my) {
        int x0 = px + 1;
        int y0 = py + BODY_Y;
        var rows = currentRows();
        int max = Math.max(0, rows.size() - LIST_ROWS);
        listScroll = Math.clamp(listScroll, 0, max);
        for (int i = 0; i < LIST_ROWS; i++) {
            int idx = listScroll + i;
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
            g.drawString(font, off ? "辅" : "主", x0 + 20, y + 5, off ? TdpUi.GREEN : TdpUi.CYAN);
            String name = TdpUi.clip(font, TdpData.name(m).getString(), 220);
            g.drawString(font, name, x0 + 32, y + 5, 0xFFC9CFD6);
            boolean hx = TdpUi.in(mx, my, px + TdpScreen.CW - 13, y, 13, ROW_H);
            g.drawString(font, "x", px + TdpScreen.CW - 10, y + 5, hx ? 0xFFFF8A80 : 0xFF9AA0A8);
        }
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable("gui.elixir.tdp.list.empty").getString(), x0, y0 + 8, TdpUi.DIM);
        }
        TdpUi.track(g, px + TdpScreen.CW - 9, y0, LIST_ROWS * ROW_H, listScroll, max, LIST_ROWS);
    }

    @Override
    protected boolean listRowClick(double mx, double my, int px, int py) {
        int row = (int) ((my - py - BODY_Y) / ROW_H);
        if (row < 0 || row >= LIST_ROWS) return false;
        var rows = currentRows();
        int idx = listScroll + row;
        if (idx < 0 || idx >= rows.size()) return false;
        var r = rows.get(idx);
        if (mx >= px + TdpScreen.CW - 14) {
            if (r.isOff()) offId = "";
            else selectedMains.remove(TdpData.id(r.m()));
            if (!pharmManual) pharm.setText(String.valueOf(sumPharm()));
        }
        return true;
    }

    @Override
    protected void hoverList(TdpScreen host, GuiGraphics g, int px, int py, int mouseX, int mouseY) {
        int row = (mouseY - py - BODY_Y) / ROW_H;
        if (row < 0 || row >= LIST_ROWS) return;
        var rows = currentRows();
        int idx = listScroll + row;
        if (idx >= 0 && idx < rows.size()) TdpData.hoverMaterial(host, g, rows.get(idx).m(), mouseX, mouseY);
    }

    @Override
    protected void drawFixed(TdpScreen host, GuiGraphics g, Font font, int px, int py, int mx, int my) {
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
        presets.render(g, font, px, py + PRESET_Y, mx, my);
        drawButtons(g, font, px, py, mx, my);
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

    @Override
    protected boolean fixedClick(TdpScreen host, double mx, double my, int px, int py) {
        boolean ok = hasSelection() && selectedOff() != null && count.intValue(0) >= 1;
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
        if (hasSelection() && TdpUi.in(mx, my, px + 246, by, 50, 18)) {
            selectedMains.clear();
            if (!pharmManual) pharm.setText("0");
            return true;
        }
        return false;
    }

    @Override
    protected void blurFields() {
        pharm.clearFocus();
        count.clearFocus();
        pillName.clearFocus();
    }

    @Override
    protected boolean clickField(double mouseX, double mouseY) {
        if (pharm.click(mouseX, mouseY)) return true;
        if (count.click(mouseX, mouseY)) return true;
        return pillName.click(mouseX, mouseY);
    }

    @Override
    protected boolean charField(char codePoint, int modifiers) {
        if (pharm.charTyped(codePoint, modifiers)) {
            pharmManual = true;
            return true;
        }
        if (count.charTyped(codePoint, modifiers)) return true;
        return pillName.charTyped(codePoint, modifiers);
    }

    @Override
    protected boolean keyField(int keyCode, int scanCode, int modifiers) {
        if (pharm.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (count.keyPressed(keyCode, scanCode, modifiers)) return true;
        return pillName.keyPressed(keyCode, scanCode, modifiers);
    }

    private void sendGenerate() {
        var off = selectedOff();
        if (off == null) return;
        int n = Math.clamp(count.intValue(1), 1, 512);
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

    @Override
    protected void savePreset(String name) {
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
        flash(Component.translatable("gui.elixir.tdp.preset.saved", name).getString());
    }

    @Override
    protected void applyPreset(String name) {
        var o = TdpPresets.get("pill", name);
        if (o == null) return;
        selectedMains.clear();
        var main = o.getAsJsonArray("main");
        if (main != null) {
            for (var e : main) {
                String id = e.getAsString();
                if (TdpData.byId(id) != null) selectedMains.add(id);
            }
        }
        if (o.has("off")) offId = o.get("off").getAsString();
        if (o.has("pharm")) pharm.setText(String.valueOf(o.get("pharm").getAsInt()));
        if (o.has("count")) count.setText(String.valueOf(o.get("count").getAsInt()));
        if (o.has("pillName")) pillName.setText(o.get("pillName").getAsString());
    }
}

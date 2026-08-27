package net.per.elixir.render.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.per.elixir.data.AlchemicalFormulaComponent;
import net.per.elixir.data.ElixirComponent;
import net.per.elixir.registry.data.Material;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AlchemicalFormulaDetailTooltip implements TooltipComponent, ClientTooltipComponent {
    private static final Component hint = Component.translatable("tooltip.alchemical_formula.scroll");
    private static final Component unknown = Component.translatable("item.elixir.material.unknown");
    private static final int HEADER_COLOR = 0xB4FF59;
    private static final int ROW_H = 18;
    private static final int GAP = 10;
    private static final int MAX_ROWS = 5;
    private static final int BOX_W = 96;
    private static final int BOX_PAD = 6;
    private static final int LINE_H = 10;
    private static final int CHARS_PER_LINE = 14;

    private static Object active;
    private static int activeSize;
    private static int selected;
    private static boolean drawn;

    private final List<Content> rows;

    public record Content(Holder<Material> material, int count) {
    }

    public AlchemicalFormulaDetailTooltip(List<Content> rows) {
        this.rows = rows;
    }

    public static List<Content> rowsOf(AlchemicalFormulaComponent data) {
        var rows = new ArrayList<Content>();
        for (var c : data.main()) if (valid(c.material())) rows.add(new Content(c.material(), c.count()));
        for (var c : data.off()) if (valid(c.material())) rows.add(new Content(c.material(), c.count()));
        return rows;
    }

    public static List<Content> rowsOf(ElixirComponent data) {
        var rows = new ArrayList<Content>();
        for (var m : data.main()) if (valid(m)) rows.add(new Content(m, 1));
        if (valid(data.off())) rows.add(new Content(data.off(), 1));
        return rows;
    }

    private static boolean valid(Holder<Material> m) {
        return m.value().item().value() != Items.AIR;
    }

    public static void begin(Object key, int size) {
        if (active != key) {
            active = key;
            activeSize = size;
            selected = 0;
        }
        drawn = true;
    }

    public static void clear() {
        active = null;
        activeSize = 0;
        selected = 0;
        drawn = false;
    }

    public static void onFrame() {
        if (!drawn) clear();
        drawn = false;
    }

    public static boolean scroll(double delta) {
        if (active == null) return false;
        if (activeSize > 1) selected = Math.floorMod(selected + (delta > 0 ? -1 : 1), activeSize);
        return true;
    }

    private int firstRow() {
        return rows.size() <= MAX_ROWS ? 0 : Math.max(0, selected - MAX_ROWS + 1);
    }

    private int visibleRows() {
        return Math.min(rows.size(), MAX_ROWS);
    }

    @Override
    public int getHeight() {
        return Math.max(12 + visibleRows() * ROW_H + 1, boxEstimate());
    }

    @Override
    public int getWidth(Font font) {
        return leftWidth(font) + GAP + BOX_W;
    }

    private int leftWidth(Font font) {
        var w = font.width(hint);
        for (var c : rows) w = Math.max(w, 18 + font.width(name(c)));
        return w;
    }

    private int boxEstimate() {
        if (rows.isEmpty()) return BOX_PAD * 2 + LINE_H;
        var len = description(selectedMaterial()).getString().length();
        return BOX_PAD * 2 + Math.max(1, Math.ceilDiv(len, CHARS_PER_LINE)) * LINE_H;
    }

    private Material selectedMaterial() {
        return rows.get(selected).material().value();
    }

    private static Component description(Material m) {
        return m.hasDescription() ? Component.translatable(m.description()) : unknown;
    }

    @Override
    public void renderText(Font font, int mouseX, int mouseY, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        font.drawInBatch(hint, mouseX, mouseY, HEADER_COLOR, false, matrix, bufferSource, Font.DisplayMode.POLYGON_OFFSET, 0, 0xf000f0);
        var rowY = mouseY + 12;
        var end = Math.min(rows.size(), firstRow() + MAX_ROWS);
        for (int i = firstRow(); i < end; i++) {
            font.drawInBatch(name(rows.get(i)), mouseX + 18, rowY, color(rows.get(i).material().value(), i == selected), false, matrix, bufferSource, Font.DisplayMode.POLYGON_OFFSET, 0, 0xf000f0);
            rowY += ROW_H;
        }
    }

    @Override
    public void renderImage(Font font, int mouseX, int mouseY, GuiGraphics graphics) {
        var rowY = mouseY + 12;
        var end = Math.min(rows.size(), firstRow() + MAX_ROWS);
        for (int i = firstRow(); i < end; i++) {
            var c = rows.get(i);
            var stack = new ItemStack(c.material().value().item(), c.count());
            graphics.renderItem(stack, mouseX, rowY);
            graphics.renderItemDecorations(font, stack, mouseX, rowY);
            rowY += ROW_H;
        }
        if (rows.isEmpty()) return;
        var mat = selectedMaterial();
        var color = color(mat, true);
        var desc = description(mat);
        var lines = font.split(desc, BOX_W - BOX_PAD * 2);
        var boxX = mouseX + leftWidth(font) + GAP;
        var boxY = mouseY;
        var boxH = BOX_PAD + lines.size() * LINE_H + BOX_PAD;
        var border = 0x66000000 | (color & 0xFFFFFF);
        graphics.fill(boxX, boxY, boxX + BOX_W, boxY + 1, border);
        graphics.fill(boxX, boxY + boxH - 1, boxX + BOX_W, boxY + boxH, border);
        graphics.fill(boxX, boxY, boxX + 1, boxY + boxH, border);
        graphics.fill(boxX + BOX_W - 1, boxY, boxX + BOX_W, boxY + boxH, border);
        graphics.fill(boxX + 1, boxY + 1, boxX + BOX_W - 1, boxY + boxH - 1, 0x40000000);
        for (var j = 0; j < lines.size(); j++) {
            graphics.drawString(font, lines.get(j), boxX + BOX_PAD, boxY + BOX_PAD + j * LINE_H, color);
        }
    }

    private static Component name(Content c) {
        var m = c.material().value();
        var name = m.main()
                ? Component.translatable(m.item().value().getDescriptionId())
                : Component.translatable(m.nameKey(c.material()));
        return c.count() > 1 ? name.copy().append(Component.literal(" ×" + c.count())) : name;
    }

    private static int color(Material m, boolean selected) {
        var colors = m.colors();
        var base = colors == null || colors.length == 0 ? 0xFFFFFFFF : colors[0];
        return selected ? base : dim(base);
    }

    private static int dim(int color) {
        return 0xFF000000 | (color >> 16 & 0xFF) * 3 / 5 << 16 | (color >> 8 & 0xFF) * 3 / 5 << 8 | (color & 0xFF) * 3 / 5;
    }
}

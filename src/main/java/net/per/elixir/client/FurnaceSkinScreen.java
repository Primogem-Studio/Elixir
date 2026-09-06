package net.per.elixir.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.per.elixir.block.ElixirFurnaceBlock;
import net.per.elixir.block.entity.LargeFurnaceBlockEntity;
import net.per.elixir.network.SetFurnaceSkinPayload;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.registry.ElixirRegistries;
import net.per.elixir.registry.data.FurnaceVisual;
import net.per.elixir.render.entity.block.LargeFurnaceRenderer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class FurnaceSkinScreen extends Screen {
    private static final int PANEL_W = 258;
    private static final int PANEL_H = 240;
    private static final int CONTENT_X = 12;

    private static final int COLS = 4;
    private static final int CELL_W = 56;
    private static final int CELL_H = 48;
    private static final int ROWS_VISIBLE = 2;

    private static final int GRID_TOP = 98;
    private static final int GRID_H = ROWS_VISIBLE * CELL_H;
    private static final int ACTIONS_Y = 198;
    private static final int HINT_Y = 222;
    private static final int TRACK_W = 5;
    private static final int TRACK_X = PANEL_W - CONTENT_X - TRACK_W - 2;
    private static final int PREVIEW_TOP = 30;

    private record Option(FurnaceVisual visual, Component name) {
    }

    private record Models(BakedModel body, BakedModel cover, net.minecraft.world.level.block.state.BlockState bodyState,
                          net.minecraft.world.level.block.state.BlockState coverState) {
    }

    private final int size;
    private final BlockPos core;
    private final List<Option> options = new ArrayList<>();
    private int scrollRows;
    private int selected = -1;
    private int hovered = -1;
    private boolean pinned;
    private int currentIndex = -1;
    private boolean dragging;

    public FurnaceSkinScreen(int size, BlockPos core) {
        super(Component.translatable("gui.elixir.seal.title"));
        this.size = size;
        this.core = core;
        collectOptions();
        currentIndex = indexOfCurrent();
        if (currentIndex >= 0) selected = currentIndex;
    }

    private void collectOptions() {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registry = level.registryAccess().registry(ElixirRegistries.FURNACE_VISUAL).orElse(null);
        if (registry == null) return;
        var seen = new LinkedHashSet<FurnaceVisual>();
        for (var entryPair : registry.entrySet()) {
            var id = entryPair.getKey().location();
            var entry = entryPair.getValue();
            var variants = variantsOf(entry);
            for (int i = 0; i < variants.size(); i++) {
                addUnique(seen, variants.get(i), labelOf(id, i, variants.size()));
            }
            for (var tierPair : entry.tiers().entrySet()) {
                var tiered = entry.merge(tierPair.getValue()).flattened();
                if (seen.add(tiered)) {
                    options.add(new Option(tiered, Component.literal(id.getPath())
                            .append(" · ").append(Component.translatable("gui.elixir.seal.tier_variant", tierPair.getKey()))));
                }
            }
        }
    }

    private void addUnique(LinkedHashSet<FurnaceVisual> seen, FurnaceVisual visual, Component label) {
        if (!seen.add(visual)) return;
        options.add(new Option(visual, label));
    }

    private Component labelOf(ResourceLocation id, int index, int total) {
        var base = Component.literal(id.getPath());
        if (total <= 1) return base;
        return base.append(" · ").append(Component.translatable("gui.elixir.seal.variant", index + 1));
    }

    private static List<FurnaceVisual> variantsOf(FurnaceVisual entry) {
        var out = new ArrayList<FurnaceVisual>();
        if (entry.options().isEmpty()) {
            out.add(entry.flattened());
        } else {
            for (var option : entry.options()) {
                out.add(entry.merge(option).flattened());
            }
        }
        return out;
    }

    private int indexOfCurrent() {
        var level = Minecraft.getInstance().level;
        if (level == null) return -1;
        FurnaceVisual current;
        if (level.getBlockEntity(core) instanceof LargeFurnaceBlockEntity be && be.isVisualPinned()) {
            current = be.pinnedVisual();
            pinned = true;
        } else {
            var def = FurnaceVisual.getDefault(level);
            current = def == null ? null : def.select(size, core.asLong()).flattened();
        }
        if (current == null) return -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).visual().equals(current)) return i;
        }
        return -1;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int ox() {
        return (width - PANEL_W) / 2;
    }

    private int oy() {
        return (height - PANEL_H) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        updateHover(mouseX, mouseY);
        int ox = ox();
        int oy = oy();
        panel(g, ox, oy);
        g.drawString(font, Component.translatable("gui.elixir.seal.title"), ox + CONTENT_X, oy + 5, 0xFFE9E9E9);
        g.drawString(font, info(), ox + CONTENT_X, oy + 17, 0xFFB4BAC2);
        drawPreview(g, ox, oy);
        drawGrid(g, ox, oy);
        drawScrollbar(g, ox, oy);
        drawActions(g, ox, oy, mouseX, mouseY);
        g.drawString(font, Component.translatable("gui.elixir.seal.hint"), ox + CONTENT_X, oy + HINT_Y, 0xFF7A828C);
    }

    private Component info() {
        var scale = Component.translatable("gui.elixir.seal.scale", size, size, size);
        var mode = pinned ? Component.translatable("gui.elixir.seal.current_pinned")
                : Component.translatable("gui.elixir.seal.current_dynamic");
        return scale.append("  ·  ").append(mode);
    }

    private void panel(GuiGraphics g, int ox, int oy) {
        g.fill(ox, oy, ox + PANEL_W, oy + PANEL_H, 0xE21C1E21);
        g.fill(ox, oy, ox + PANEL_W, oy + 1, 0xFF585B60);
        g.fill(ox, oy + PANEL_H - 1, ox + PANEL_W, oy + PANEL_H, 0xFF585B60);
        g.fill(ox, oy, ox + 1, oy + PANEL_H, 0xFF585B60);
        g.fill(ox + PANEL_W - 1, oy, ox + PANEL_W, oy + PANEL_H, 0xFF585B60);
    }

    private void drawPreview(GuiGraphics g, int ox, int oy) {
        int x0 = ox + CONTENT_X;
        int y0 = oy + PREVIEW_TOP;
        int box = 56;
        g.fill(x0, y0, x0 + box, y0 + box, 0x9917191B);
        int idx = hovered >= 0 ? hovered : selected >= 0 ? selected : Math.max(currentIndex, 0);
        if (idx >= 0 && idx < options.size()) {
            var models = modelsOf(options.get(idx).visual());
            renderFurnace(g, models, x0 + box / 2, y0 + box / 2 + 2, 21);
            drawFlameSample(g, options.get(idx).visual(), x0, y0, box);
            int tx = x0 + box + 10;
            int maxW = ox + PANEL_W - CONTENT_X - tx - 2;
            int ty = y0 + 1;
            g.drawString(font, font.plainSubstrByWidth(options.get(idx).name().getString(), maxW),
                    tx, ty, 0xFFFFD982);
            var lines = describe(options.get(idx).visual());
            int swX = -1;
            int swY = ty + 9 * lines.size();
            for (int i = 0; i < lines.size(); i++) {
                var line = font.plainSubstrByWidth(lines.get(i), maxW);
                g.drawString(font, line, tx, ty + 9 * (i + 1), 0xFFC9CFD6);
                if (i == lines.size() - 1 && options.get(idx).visual().activeColor().isPresent()) {
                    swX = tx + font.width(line) + 4;
                }
            }
            if (swX >= 0) drawColorSwatch(g, swX, swY, options.get(idx).visual().activeColor().get());
        } else {
            g.drawString(font, Component.translatable("gui.elixir.seal.none"), x0 + box + 10, y0 + 8, 0xFF8A8A8A);
        }
    }

    private void drawFlameSample(GuiGraphics g, FurnaceVisual visual, int x0, int y0, int box) {
        var tex = visual.activeTexture().orElse(FurnaceVisual.DEFAULT_ACTIVE_TEXTURE);
        int color = visual.activeColor().orElse(0xFFFFFFFF);
        int s = 18;
        int x = x0 + box - s - 3;
        int y = y0 + box - s - 3;
        g.fill(x - 1, y - 1, x + s + 1, y + s + 1, 0xFF101214);
        var sprite = LargeFurnaceRenderer.resolveSprite(Minecraft.getInstance(), tex);
        if (sprite != null) {
            g.blit(x, y, 0, s, s, sprite,
                    ((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F, 1.0F);
        } else {
            g.fill(x, y, x + s, y + s, 0xFF000000 | (color & 0xFFFFFF));
        }
        frame(g, x - 1, y - 1, s + 2, s + 2, 0xFF5A5D62);
    }

    private void drawColorSwatch(GuiGraphics g, int x, int y, int color) {
        int rgb = color & 0xFFFFFF;
        g.fill(x, y, x + 9, y + 9, 0xFF000000 | rgb);
        g.fill(x - 1, y - 1, x + 10, y, 0xFF9AA0A8);
        g.fill(x - 1, y + 9, x + 10, y + 10, 0xFF9AA0A8);
        g.fill(x - 1, y - 1, x, y + 10, 0xFF9AA0A8);
        g.fill(x + 9, y - 1, x + 10, y + 10, 0xFF9AA0A8);
    }

    private List<String> describe(FurnaceVisual v) {
        var fallback = Component.translatable("gui.elixir.seal.default").getString();
        var out = new ArrayList<String>();
        out.add(key("gui.elixir.seal.model", v.model(), fallback));
        out.add(key("gui.elixir.seal.cover_model", v.coverModel(), fallback));
        out.add(key("gui.elixir.seal.tex", v.texture(), fallback));
        out.add(key("gui.elixir.seal.cover_tex", v.coverTexture(), fallback));
        out.add(key("gui.elixir.seal.active_tex", v.activeTexture(), fallback));
        String color = v.activeColor().map(c -> "#" + String.format("%06X", c & 0xFFFFFF)).orElse(fallback);
        out.add(Component.translatable("gui.elixir.seal.active_color").getString() + color);
        return out;
    }

    private String key(String lang, java.util.Optional<ResourceLocation> rl, String fallback) {
        return Component.translatable(lang).getString() + rl.map(Object::toString).orElse(fallback);
    }

    private void drawGrid(GuiGraphics g, int ox, int oy) {
        int rowsTotal = (options.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, rowsTotal - ROWS_VISIBLE);
        scrollRows = Math.clamp(scrollRows, 0, maxScroll);
        int shown = Math.min(ROWS_VISIBLE, rowsTotal);
        for (int r = 0; r < shown; r++) {
            int row = scrollRows + r;
            for (int c = 0; c < COLS; c++) {
                int idx = row * COLS + c;
                if (idx >= options.size()) break;
                int cellX = ox + CONTENT_X + c * CELL_W;
                int cellY = oy + GRID_TOP + r * CELL_H;
                boolean hl = idx == hovered;
                boolean sel = idx == selected;
                boolean cur = idx == currentIndex && pinned;
                if (hl) {
                    g.fill(cellX, cellY, cellX + CELL_W, cellY + CELL_H, 0x28FFFFFF);
                    frame(g, cellX, cellY, CELL_W, CELL_H, 0xFFB9C0C8);
                }
                if (sel) frame(g, cellX, cellY, CELL_W, CELL_H, 0xFF54D9FF);
                if (cur && !sel) frame(g, cellX, cellY, CELL_W, CELL_H, 0xFF7BFF2E);
                if (idx >= 0 && idx < options.size()) {
                    var models = modelsOf(options.get(idx).visual());
                    renderFurnace(g, models, cellX + CELL_W / 2 - 1, cellY + 21, 10.5f);
                    var label = options.get(idx).name().getString();
                    var clipped = font.plainSubstrByWidth(label, CELL_W - 6);
                    g.drawString(font, clipped, cellX + (CELL_W - font.width(clipped)) / 2 - 1,
                            cellY + CELL_H - 12, 0xFFAFB6BF);
                }
            }
        }
        if (options.isEmpty()) {
            g.drawString(font, Component.translatable("gui.elixir.seal.no_options"), ox + CONTENT_X, oy + GRID_TOP + 16, 0xFF8A8A8A);
        }
    }

    private void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private int gridX0() {
        return ox() + CONTENT_X;
    }

    private int gridY0() {
        return oy() + GRID_TOP;
    }

    private int rowsTotal() {
        return (options.size() + COLS - 1) / COLS;
    }

    private int maxScroll() {
        return Math.max(0, rowsTotal() - ROWS_VISIBLE);
    }

    private int trackX() {
        return ox() + TRACK_X;
    }

    private void drawScrollbar(GuiGraphics g, int ox, int oy) {
        int max = maxScroll();
        if (max <= 0) return;
        int ty = oy + GRID_TOP + 1;
        g.fill(trackX(), ty, trackX() + TRACK_W, ty + GRID_H - 2, 0x55FFFFFF);
        int handle = handleY();
        int hh = handleH();
        g.fill(trackX(), oy + GRID_TOP + 1 + handle, trackX() + TRACK_W, oy + GRID_TOP + 1 + handle + hh, 0xCCCDD3DA);
    }

    private int handleH() {
        int max = maxScroll();
        int usable = GRID_H - 2;
        return Math.max(14, usable * ROWS_VISIBLE / Math.max(1, rowsTotal()));
    }

    private int handleY() {
        int max = maxScroll();
        if (max <= 0) return 0;
        int usable = GRID_H - 2 - handleH();
        return (int) (scrollRows * (float) usable / max);
    }

    private void updateScrollFromMouse(double mouseY) {
        int max = maxScroll();
        if (max <= 0) return;
        int usable = GRID_H - 2 - handleH();
        float ratio = (float) (mouseY - gridY0() - 1 - handleH() / 2.0) / usable;
        scrollRows = Math.clamp(Math.round(ratio * max), 0, max);
    }

    private void drawActions(GuiGraphics g, int ox, int oy, int mouseX, int mouseY) {
        boolean canPin = selected >= 0;
        drawButton(g, ox + CONTENT_X, oy + ACTIONS_Y, 118, 20,
                Component.translatable("gui.elixir.seal.pin_button"),
                canPin, in(mouseX, mouseY, ox + CONTENT_X, oy + ACTIONS_Y, 118, 20));
        drawButton(g, ox + PANEL_W - CONTENT_X - 118, oy + ACTIONS_Y, 118, 20,
                Component.translatable("gui.elixir.seal.reset_button"),
                true, in(mouseX, mouseY, ox + PANEL_W - CONTENT_X - 118, oy + ACTIONS_Y, 118, 20));
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, Component text, boolean enabled, boolean hover) {
        int base = enabled ? 0xFF41444A : 0xFF2C2E32;
        int edge = enabled ? (hover ? 0xFF9AB39F : 0xFF66696F) : 0xFF484A4F;
        g.fill(x, y, x + w, y + h, base);
        frame(g, x, y, w, h, edge);
        int color = enabled ? (hover ? 0xFFFFFFFF : 0xFFE0E2E5) : 0xFF7C7F84;
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - font.lineHeight) / 2 + 1, color);
    }

    private void updateHover(double mouseX, double mouseY) {
        hovered = indexAt(mouseX, mouseY);
    }

    private int indexAt(double mouseX, double mouseY) {
        if (options.isEmpty()) return -1;
        int col = (int) ((mouseX - gridX0()) / CELL_W);
        int row = (int) ((mouseY - gridY0()) / CELL_H);
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS_VISIBLE) return -1;
        int sr = Math.clamp(scrollRows, 0, maxScroll());
        int idx = (sr + row) * COLS + col;
        return idx >= 0 && idx < options.size() ? idx : -1;
    }

    private boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean overTrack(double mx, double my) {
        return mx >= trackX() && mx < trackX() + TRACK_W
                && my >= gridY0() + 1 && my < gridY0() + GRID_H - 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int idx = indexAt(mouseX, mouseY);
            if (idx >= 0) {
                selected = idx;
                return true;
            }
            if (overTrack(mouseX, mouseY)) {
                dragging = true;
                updateScrollFromMouse(mouseY);
                return true;
            }
            int oy = oy();
            if (selected >= 0 && in(mouseX, mouseY, ox() + CONTENT_X, oy + ACTIONS_Y, 118, 20)) {
                applyAndClose(true, options.get(selected).visual());
                return true;
            }
            if (in(mouseX, mouseY, ox() + PANEL_W - CONTENT_X - 118, oy + ACTIONS_Y, 118, 20)) {
                applyAndClose(false, null);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= gridY0() && mouseY < gridY0() + GRID_H
                && mouseX >= gridX0() && mouseX < trackX() + TRACK_W + 4) {
            scrollRows -= (int) Math.signum(verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void applyAndClose(boolean pin, FurnaceVisual visual) {
        PacketDistributor.sendToServer(new SetFurnaceSkinPayload(core, pin, visual));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    private Models modelsOf(FurnaceVisual visual) {
        var mc = Minecraft.getInstance();
        var furnaceState = ElixirBlocks.elixir_furnace.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(ElixirFurnaceBlock.ACTIVE, false);
        var coverState = ElixirBlocks.elixir_furnace_cover.get().defaultBlockState();
        var body = visual.model().isPresent()
                ? LargeFurnaceRenderer.resolveModel(mc, visual.model().get(), mc.getBlockRenderer().getBlockModel(furnaceState))
                : mc.getBlockRenderer().getBlockModel(furnaceState);
        body = LargeFurnaceRenderer.swapTexture(mc, body, furnaceState, visual.texture().orElse(null));
        var cover = visual.coverModel().isPresent()
                ? LargeFurnaceRenderer.resolveModel(mc, visual.coverModel().get(), mc.getBlockRenderer().getBlockModel(coverState))
                : mc.getBlockRenderer().getBlockModel(coverState);
        cover = LargeFurnaceRenderer.swapTexture(mc, cover, coverState, visual.coverTexture().orElse(null));
        return new Models(body, cover, furnaceState, coverState);
    }

    private void renderFurnace(GuiGraphics g, Models models, float cx, float cy, float scale) {
        var mc = Minecraft.getInstance();
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 100.0F);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(13.0F));
        pose.mulPose(Axis.YP.rotationDegrees(135.0F));
        pose.translate(-0.5F, -1.0F, -0.5F);
        var buf = g.bufferSource().getBuffer(RenderType.cutout());
        var renderer = mc.getBlockRenderer().getModelRenderer();
        com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        try {
            renderer.renderModel(pose.last(), buf, models.bodyState(), models.body(), 1f, 1f, 1f, 0xF000F0, 0);
            pose.pushPose();
            pose.translate(0.0F, 1.0F, 0.0F);
            renderer.renderModel(pose.last(), buf, models.coverState(), models.cover(), 1f, 1f, 1f, 0xF000F0, 0);
            pose.popPose();
            g.flush();
        } finally {
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        }
        pose.popPose();
    }
}

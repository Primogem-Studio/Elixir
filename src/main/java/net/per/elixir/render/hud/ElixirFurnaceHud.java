package net.per.elixir.render.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.per.elixir.ElixirConfig;
import net.per.elixir.block.entity.ElixirFurnaceBlockEntity;
import net.per.elixir.item.HandheldFanItem;
import net.per.elixir.registry.ElixirBlocks;

import static net.per.elixir.Elixir.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ElixirFurnaceHud {
    private static final ResourceLocation DAN_HUD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/screens/dan_hud.png");
    private static final int TEX_W = 51;
    private static final int TEX_H = 89;
    private static final SpriteRect TEMP_BAR = new SpriteRect(0, 0, 20, 88);
    private static final SpriteRect STABILITY_BAR = new SpriteRect(26, 0, 12, 40);
    private static final SpriteRect TIME_BAR = new SpriteRect(23, 43, 20, 4);
    private static final SpriteRect MARKER = new SpriteRect(22, 49, 4, 5);
    private static final SpriteRect STATIC_LINE = new SpriteRect(23, 57, 15, 3);
    private static final int COLOR_RED = 0xFFFF3B30;
    private static final int COLOR_YELLOW = 0xFFFFD60A;
    private static final int COLOR_GREEN = 0xFF59D60A;
    private static final int COLOR_BLUE = 0xFF30A7FF;

    private record SpriteRect(int u, int v, int w, int h) {
    }

    @SubscribeEvent
    @SuppressWarnings("resource")
    private static void onRenderGui(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        if (!(player.getMainHandItem().getItem() instanceof HandheldFanItem)
                && !(player.getOffhandItem().getItem() instanceof HandheldFanItem)) return;
        var hit = player.pick(8.0, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return;
        if (!(player.level().getBlockEntity(((BlockHitResult) hit).getBlockPos()) instanceof ElixirFurnaceBlockEntity furnace)) return;
        var g = event.getGuiGraphics();
        var pw = TEMP_BAR.w();
        var ph = TEMP_BAR.h();
        var left = g.guiWidth() / 2 - pw / 2;
        var top = g.guiHeight() / 2 - ph / 2;
        var right = left + pw;
        var t = ((furnace.pharma() / (float) Math.max(1, ElixirConfig.pharmaLimited)) + 1) / 2f * 500;
        var range = furnace.tempRange > 0 ? furnace.tempRange : ElixirConfig.extremeTemperatureRange;
        var explode = Mth.clamp(t + range, 0, 500);
        var low = Mth.clamp(t - range, 0, 500);
        var temp = Mth.clamp(furnace.temperature, 0, 500);
        var scale = ph / 500f;
        var ey = top + (int) ((500 - explode) * scale);
        var ly = top + (int) ((500 - low) * scale);
        var ty = Mth.clamp(top + (int) ((500 - temp) * scale), top + 2, top + ph - 5);
        blitSprite(g, TEMP_BAR, left, top);
        blitScaled(g, left + 2, ey, 16, 2);
        blitScaled(g, left + 2, ly, 16, 2);
        blitScaledTinted(g, left + 2, ty + 1, 12, 1, COLOR_YELLOW);
        blitMarker(g, left + 14, ty - 1, COLOR_YELLOW);
        var font = mc.font;
        var explodeLabelY = ey - 8;
        if (explodeLabelY < top + 2) explodeLabelY = ey + 4;
        var lowLabelY = ly + 2;
        if (lowLabelY > top + ph - 8) lowLabelY = ly - 10;
        var tempLabelY = ty - 2;
        if (Math.abs(ty - ey) < 12) tempLabelY = ty + 8;
        else if (Math.abs(ty - ly) < 12) tempLabelY = ty - 10;
        drawSmall(g, font, String.valueOf(Math.round(explode)), right + 4, explodeLabelY, COLOR_RED);
        drawSmall(g, font, String.valueOf(Math.round(low)), right + 4, lowLabelY, COLOR_BLUE);
        drawSmall(g, font, String.valueOf(Math.round(temp)), right + 4, tempLabelY, COLOR_YELLOW);
        if (furnace.started()) {
            var level = furnace.getLevel();
            if (level == null) return;
            var barW = STABILITY_BAR.w();
            var barH = STABILITY_BAR.h();
            var barLeft = left - barW - 10;
            var barTop = top + (ph - barH) / 2;
            var s = (ElixirFurnaceBlockEntity.calcStability(level, furnace.getBlockPos()) + furnace.stability())
                    * (1 + furnace.tempStability() / (Math.abs(furnace.tempStability()) + 50));
            var lim = Math.max(1, ElixirConfig.pharmaLimited);
            var covered = level.getBlockState(furnace.getBlockPos().above()).is(ElixirBlocks.elixir_furnace_cover);
            var threshold = covered ? -lim : -lim * 2f;
            var ok = s > threshold;
            var minS = threshold - lim * 1.5f;
            var maxS = threshold + lim * 1.5f;
            var ratio = Mth.clamp((float) ((s - minS) / (maxS - minS)), 0, 1);
            var sy = Mth.clamp(barTop + (int) (barH * (1 - ratio)), barTop + 2, barTop + barH - 5);
            var thY = Mth.clamp(barTop + (int) (barH * (1 - (threshold - minS) / (maxS - minS))), barTop + 1, barTop + barH - 1);
            blitSprite(g, STABILITY_BAR, barLeft, barTop);
            blitScaled(g, barLeft + 1, thY, 10, 1);
            var sColor = ok ? COLOR_GREEN : COLOR_RED;
            blitScaledTinted(g, barLeft + 1, sy + 1, 7, 1, sColor);
            blitMarker(g, barLeft + 8, sy - 1, sColor);
            var stateText = ok ? "Stable" : "Unstable";
            drawSmall(g, font, stateText, barLeft + barW - font.width(stateText) / 2, barTop + barH + 2, ok ? COLOR_GREEN : COLOR_RED);
            var barY = top + ph + 6;
            var totalTicks = Math.clamp((long) furnace.pharma() * ElixirConfig.refineTicks, 200, 1200);
            var fill = Mth.clamp((float) furnace.progress() / totalTicks, 0, 1);
            blitSprite(g, TIME_BAR, left, barY);
            blitScaledTinted(g, left + 1, barY, (int) (fill * (pw - 2)), 4, COLOR_GREEN);
            var timeText = String.format("%.1f", Math.max(0, totalTicks - furnace.progress()) / 20f) + "s";
            g.drawString(font, timeText, left - font.width(timeText) - 4, barY - 2, 0xffe0e0e0, true);
        }
    }

    private static void blitSprite(GuiGraphics g, SpriteRect s, int x, int y) {
        g.blit(DAN_HUD, x, y, s.u(), s.v(), s.w(), s.h(), TEX_W, TEX_H);
    }

    private static void blitScaled(GuiGraphics g, int x, int y, int dw, int dh) {
        g.blit(DAN_HUD, x, y, dw, dh, STATIC_LINE.u(), STATIC_LINE.v(), STATIC_LINE.w(), STATIC_LINE.h(), TEX_W, TEX_H);
    }

    private static void tint(int color) {
        RenderSystem.setShaderColor(
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f);
    }

    private static void resetTint() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void blitScaledTinted(GuiGraphics g, int x, int y, int dw, int dh, int color) {
        if (dw <= 0 || dh <= 0) return;
        tint(color);
        g.blit(DAN_HUD, x, y, dw, dh, MARKER.u(), MARKER.v(), MARKER.w(), MARKER.h(), TEX_W, TEX_H);
        resetTint();
    }

    private static void blitMarker(GuiGraphics g, int x, int y, int color) {
        tint(color);
        g.blit(DAN_HUD, x, y, MARKER.u(), MARKER.v(), MARKER.w(), MARKER.h(), TEX_W, TEX_H);
        resetTint();
    }

    private static void drawSmall(GuiGraphics g, Font font, String text, int x, int y, int color) {
        var pose = g.pose();
        pose.pushPose();
        pose.scale(0.5f, 0.5f, 1f);
        g.drawString(font, text, x * 2, y * 2, color, true);
        pose.popPose();
    }
}

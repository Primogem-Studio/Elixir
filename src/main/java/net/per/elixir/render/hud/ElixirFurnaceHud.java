package net.per.elixir.render.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.per.elixir.ElixirConfig;
import net.per.elixir.data.IFurnaceView;
import net.per.elixir.item.HandheldFanItem;
import net.per.elixir.util.ElixirMath;
import net.per.elixir.util.MultiFurnaceStructure;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

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
    private static final SpriteRect TRI_TIP = new SpriteRect(28, 49, 1, 3);
    private static final SpriteRect STATIC_LINE = new SpriteRect(23, 57, 15, 3);
    private static final int COLOR_RED = 0xFFFF3B30;
    private static final int COLOR_YELLOW = 0xFFFFD60A;
    private static final int COLOR_GREEN = 0xFF59D60A;
    private static final int COLOR_BLUE = 0xFF30A7FF;
    private static final float SPRING_STIFFNESS = 324f;
    private static final float[] tempState = {Float.NaN, 0f};
    private static final float[] stabilityState = {Float.NaN, 0f};
    private static BlockPos smoothPos = null;
    private static long smoothNanos = Long.MIN_VALUE;

    private record SpriteRect(int u, int v, int w, int h) {
    }

    @SubscribeEvent
    @SuppressWarnings("resource")
    private static void onRenderGui(RenderGuiLayerEvent.Pre event) {
        if (!isHudActive(Minecraft.getInstance())) return;
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            event.setCanceled(true);
        } else if (event.getName().equals(VanillaGuiLayers.CHAT)) {
            renderHud(event.getGuiGraphics());
        }
    }

    @SuppressWarnings("resource")
    private static void renderHud(GuiGraphics g) {
        var mc = Minecraft.getInstance();
        if (!isHudActive(mc)) return;
        var player = mc.player;
        var hit = player.pick(8.0, 1.0f, false);
        var level = player.level();
        var hitPos = ((BlockHitResult) hit).getBlockPos();
        var be = level.getBlockEntity(hitPos);
        IFurnaceView furnace = null;
        if (be instanceof IFurnaceView v) furnace = v;
        else {
            var core = MultiFurnaceStructure.findCore(level, hitPos);
            if (core != null && level.getBlockEntity(core) instanceof IFurnaceView v) furnace = v;
        }
        if (furnace == null) return;
        var now = System.nanoTime();
        float dt;
        if (smoothPos == null || !smoothPos.equals(furnace.blockPos())) {
            dt = -1f;
            smoothPos = furnace.blockPos().immutable();
        } else {
            dt = (now - smoothNanos) / 1e9f;
        }
        smoothNanos = now;
        var pose = g.pose();
        pose.pushPose();
        var hudScale = Math.clamp((float) ElixirConfig.hudScale, 1f, 4f);
        var cx = g.guiWidth() / 2f;
        var cy = g.guiHeight() / 2f;
        pose.translate(cx, cy, 0);
        pose.scale(hudScale, hudScale, 1);
        pose.translate(-cx, -cy, 0);
        var pw = TEMP_BAR.w();
        var ph = TEMP_BAR.h();
        var left = g.guiWidth() / 2 - pw / 2;
        var top = g.guiHeight() / 2 - ph / 2;
        var right = left + pw;
        var t = furnace.targetTemp();
        var range = furnace.tempRange() > 0 ? furnace.tempRange() : ElixirConfig.EXTREME_TEMP_RANGE_FALLBACK;
        var explode = t + range;
        var low = Mth.clamp(t - range, 0, 500);
        var upperTemp = Math.max(1, explode + ElixirConfig.tempSafeMargin);
        var temp = spring(dt, tempState, Mth.clamp(furnace.temperature(), 0, upperTemp));
        var scale = ph / upperTemp;
        var ey = top + (int) ((upperTemp - explode) * scale);
        var ly = top + (int) ((upperTemp - low) * scale);
        var tyf = Mth.clamp(top + (upperTemp - temp) * scale, top + 2f, top + ph - 5f);
        var ty = Mth.floor(tyf);
        var tyFrac = tyf - ty;
        blitSprite(g, TEMP_BAR, left, top);
        blitScaled(g, left + 2, ey, 16, 2);
        blitScaled(g, left + 2, ly, 16, 2);
        pose.pushPose();
        pose.translate(0, tyFrac, 0);
        blitScaledTinted(g, left + 2, ty + 1, 12, 1, COLOR_YELLOW);
        blitMarker(g, left + 14, ty - 1, COLOR_YELLOW);
        pose.popPose();
        var font = mc.font;
        var explodeLabelY = ey - 8;
        if (explodeLabelY < top + 2) explodeLabelY = ey + 4;
        var lowLabelY = ly + 2;
        if (lowLabelY > top + ph - 8) lowLabelY = ly - 10;
        var tempLabelY = ty - 2;
        if (Math.abs(ty - ey) < 12) tempLabelY = ty + 8;
        else if (Math.abs(ty - ly) < 12) tempLabelY = ty - 10;
        if (Math.abs(explodeLabelY - lowLabelY) < 10) {
            if (explodeLabelY <= lowLabelY) lowLabelY = explodeLabelY + 10;
            else explodeLabelY = lowLabelY + 10;
        }
        if (Math.abs(tempLabelY - explodeLabelY) < 10) tempLabelY = explodeLabelY + 10;
        if (Math.abs(tempLabelY - lowLabelY) < 10) tempLabelY = lowLabelY + 10;
        explodeLabelY = Mth.clamp(explodeLabelY, top, top + ph);
        lowLabelY = Mth.clamp(lowLabelY, top, top + ph);
        tempLabelY = Mth.clamp(tempLabelY, top, top + ph);
        drawSmall(g, font, Math.round(explode) + "℃", right + 4, explodeLabelY, COLOR_RED);
        drawSmall(g, font, Math.round(low) + "℃", right + 4, lowLabelY, COLOR_BLUE);
        drawSmall(g, font, Math.round(temp) + "℃", right + 4, tempLabelY, COLOR_YELLOW);
        if (furnace.started()) {
            var barW = STABILITY_BAR.w();
            var barH = STABILITY_BAR.h();
            var barLeft = left - barW - 10;
            var barTop = top + (ph - barH) / 2;
            var rawS = (furnace.stabilityBonus(level) + furnace.stability())
                    * (1 + furnace.tempStability() / (Math.abs(furnace.tempStability()) + 50));
            var s = spring(dt, stabilityState, (float) rawS);
            var lim = Math.max(1, furnace.pharmaLimit());
            var covered = furnace.isCovered(level);
            var threshold = covered ? -lim : -lim * 2f;
            var offs = furnace.offs();
            var offMat = offs == null || offs.isEmpty() ? null
                    : offs.stream().min(Comparator.comparing(o -> o.unwrapKey().map(k -> k.location().toString()).orElse(""))).orElseThrow();
            var pharmZero = offMat == null
                    ? covered ? -(100 + furnace.exp()) : -2 * (100 + furnace.exp())
                    : ElixirMath.findPharmZero(offMat, Math.max(1, furnace.pharma()), furnace.exp(), threshold - lim * 3f - 100f, threshold + lim * 3f + 100f, covered);
            var minS = Math.min(threshold - lim * 1.5f, pharmZero - lim * 0.5f);
            var maxS = Math.max(threshold + lim * 1.5f, pharmZero + lim * 0.5f);
            var ok = rawS > threshold;
            var ratio = Mth.clamp((float) ((s - minS) / (maxS - minS)), 0, 1);
            var syf = Mth.clamp(barTop + barH * (1 - ratio), barTop + 2f, barTop + barH - 5f);
            var sy = Mth.floor(syf);
            var syFrac = syf - sy;
            var thY = Mth.clamp(barTop + (int) (barH * (1 - (threshold - minS) / (maxS - minS))), barTop + 1, barTop + barH - 1);
            blitSprite(g, STABILITY_BAR, barLeft, barTop);
            blitScaled(g, barLeft + 1, thY, 10, 1);
            blitScaledTinted(g, barLeft + 5, thY, 2, 1, COLOR_RED);
            var pzRatio = Mth.clamp((float) ((pharmZero - minS) / (maxS - minS)), 0, 1);
            var pzY = Mth.clamp(barTop + (int) (barH * (1 - pzRatio)), barTop + 1, barTop + barH - 1);
            blitScaled(g, barLeft + 1, pzY, 10, 1);
            blitScaledTinted(g, barLeft + 5, pzY, 2, 1, COLOR_YELLOW);
            var sColor = ok ? COLOR_GREEN : COLOR_RED;
            pose.pushPose();
            pose.translate(0, syFrac, 0);
            blitScaledTinted(g, barLeft + 1, sy + 1, 7, 1, sColor);
            blitMarker(g, barLeft + 8, sy - 1, sColor);
            pose.popPose();
            var stateText = ok ? Component.translatable("hud.elixir.stable") : Component.translatable("hud.elixir.unstable");
            drawSmall(g, font, stateText, barLeft + barW - font.width(stateText) / 2, barTop + barH + 2, ok ? COLOR_GREEN : COLOR_RED);
            var barY = top + ph + 6;
            var totalTicks = furnace.totalTicks();
            var fill = Mth.clamp((float) furnace.progress() / Math.max(1, totalTicks), 0, 1);
            blitSprite(g, TIME_BAR, left, barY);
            blitScaledTinted(g, left + 1, barY + 1, (int) (fill * (pw - 2)), 2, COLOR_GREEN);
            var timeText = String.format("%.1f", Math.max(0, totalTicks - furnace.progress()) / 20f) + "s";
            g.drawString(font, timeText, left - font.width(timeText) - 4, barY - 2, 0xffe0e0e0, true);
        }
        pose.popPose();
    }

    private static boolean isHudActive(Minecraft mc) {
        var player = mc.player;
        if (player == null) return false;
        if (!(player.getMainHandItem().getItem() instanceof HandheldFanItem)
                && !(player.getOffhandItem().getItem() instanceof HandheldFanItem)) return false;
        var hit = player.pick(8.0, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        var pos = ((BlockHitResult) hit).getBlockPos();
        var be = player.level().getBlockEntity(pos);
        if (be instanceof IFurnaceView) return true;
        return MultiFurnaceStructure.findCore(player.level(), pos) != null;
    }

    @SubscribeEvent
    private static void onClientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && isHudActive(mc) && mc.screen instanceof ChatScreen) {
            GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
    }

    private static float spring(float dt, float[] state, float target) {
        if (dt < 0f || Float.isNaN(state[0]) || dt > 0.5f) {
            state[0] = target;
            state[1] = 0f;
            return target;
        }
        var omega = (float) Math.sqrt(SPRING_STIFFNESS);
        var damping = 2f * omega;
        var a = SPRING_STIFFNESS * (target - state[0]) - damping * state[1];
        state[1] += a * dt;
        state[0] += state[1] * dt;
        return state[0];
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
        g.blit(DAN_HUD, x, y, dw, dh, TRI_TIP.u(), TRI_TIP.v(), TRI_TIP.w(), TRI_TIP.h(), TEX_W, TEX_H);
        resetTint();
    }

    private static void blitMarker(GuiGraphics g, int x, int y, int color) {
        tint(color);
        g.blit(DAN_HUD, x, y, MARKER.u(), MARKER.v(), MARKER.w(), MARKER.h(), TEX_W, TEX_H);
        resetTint();
    }

    private static void drawSmall(GuiGraphics g, Font font, String text, int x, int y, int color) {
        drawSmall(g, font, Component.literal(text), x, y, color);
    }

    private static void drawSmall(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        var pose = g.pose();
        pose.pushPose();
        pose.scale(0.5f, 0.5f, 1f);
        g.drawString(font, text, x * 2, y * 2, color, true);
        pose.popPose();
    }
}

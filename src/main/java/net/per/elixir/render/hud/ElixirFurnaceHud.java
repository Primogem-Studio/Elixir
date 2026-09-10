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

import static net.per.elixir.Elixir.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ElixirFurnaceHud {
    private static final ResourceLocation DAN_HUD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/screens/dan_hud.png");
    private static final ResourceLocation THERMOMETER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/screens/thermometer.png");
    private static final int TEX_W = 51;
    private static final int TEX_H = 89;
    private static final int THERMO_W = 23;
    private static final int THERMO_H = 80;
    private static final SpriteRect TEMP_BAR = new SpriteRect(0, 0, 20, 88);
    private static final SpriteRect TEMP_WINDOW = new SpriteRect(3, 3, 14, 80);
    private static final SpriteRect THERMO_TEMP = new SpriteRect(0, 0, 14, 80);
    private static final SpriteRect STABILITY_BAR = new SpriteRect(26, 0, 12, 40);
    private static final SpriteRect STABILITY_WINDOW = new SpriteRect(3, 3, 6, 33);
    private static final SpriteRect THERMO_STABILITY = new SpriteRect(17, 0, 6, 33);
    private static final SpriteRect TIME_BAR = new SpriteRect(23, 43, 20, 5);
    private static final SpriteRect MARKER = new SpriteRect(22, 49, 4, 5);
    private static final SpriteRect TRI_TIP = new SpriteRect(28, 49, 1, 3);
    private static final SpriteRect STATIC_LINE = new SpriteRect(23, 57, 15, 3);
    private static final int COLOR_RED = 0xFFFF3B30;
    private static final int COLOR_YELLOW = 0xFFFFD60A;
    private static final int COLOR_GREEN = 0xFF59D60A;
    private static final int COLOR_BLUE = 0xFF30A7FF;
    private static final int COLOR_PURPLE = 0xFFB44DFF;
    private static final int STABILITY_BAND_LIFT = 5;
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
        blitSprite(g, TEMP_BAR, left, top);
        blitReveal(g, THERMO_TEMP, TEMP_WINDOW, left, top, tyf - top);
        blitScaled(g, left + TEMP_WINDOW.u(), ey, TEMP_WINDOW.w(), 2);
        blitScaled(g, left + TEMP_WINDOW.u(), ly, TEMP_WINDOW.w(), 2);
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
            var lim = Math.max(1, furnace.pharmaLimit());
            var covered = furnace.isCovered(level);
            var threshold = covered ? -lim : -lim * 2f;
            var rawS = (furnace.stabilityBonus(level) + furnace.stability())
                    * (1 + furnace.tempStability() / (Math.abs(furnace.tempStability()) + 50));
            var s = spring(dt, stabilityState, (float) rawS);
            var off = furnace.offMaterial();
            var winTop = barTop + STABILITY_WINDOW.v();
            var warnY = winTop + STABILITY_WINDOW.h() / 3 - STABILITY_BAND_LIFT;
            var failY = winTop + STABILITY_WINDOW.h() * 2 / 3 - STABILITY_BAND_LIFT;
            var failS = (double) threshold;
            var pharma = furnace.pharma();
            var exp = furnace.exp();
            var pharmS = covered ? s : s * 0.5f;
            var predicted = off == null
                    ? Mth.clamp(ElixirMath.rawPharm(pharma, exp, pharmS), -ElixirConfig.pharmaLimited, ElixirConfig.pharmaLimited)
                    : ElixirMath.predictPharm(off, pharma, exp, pharmS);
            var warnS = off == null ? -(100d + exp)
                    : ElixirMath.findPharmZero(off, pharma, exp, failS - lim * 3f - 100f, failS + lim * 3f + 100f, covered);
            var gap = (float) (warnS - failS);
            var merged = gap <= lim * 0.25f;
            var span = merged ? (float) lim : gap;
            var warnLine = merged ? failY : warnY;
            var warnTint = merged ? COLOR_PURPLE : COLOR_YELLOW;
            var failTint = merged ? COLOR_PURPLE : COLOR_RED;
            var band = (float) ((s - failS) / span);
            var syf = Mth.clamp(failY - band * (failY - warnY), (float) winTop, (float) (winTop + STABILITY_WINDOW.h()));
            var sy = Mth.floor(syf);
            var syFrac = syf - sy;
            var sColor = s <= threshold ? COLOR_RED : predicted > 0 ? COLOR_GREEN : COLOR_YELLOW;
            var stateText = Component.translatable(s <= threshold ? "hud.elixir.doomed"
                    : predicted > 0 ? "hud.elixir.stable" : "hud.elixir.unstable");
            blitSprite(g, STABILITY_BAR, barLeft, barTop);
            blitReveal(g, THERMO_STABILITY, STABILITY_WINDOW, barLeft, barTop, syf - barTop);
            blitScaled(g, barLeft + STABILITY_WINDOW.u(), warnLine, STABILITY_WINDOW.w(), 1);
            blitScaledTinted(g, barLeft + STABILITY_WINDOW.u() + 2, warnLine, 2, 1, warnTint);
            blitScaled(g, barLeft + STABILITY_WINDOW.u(), failY, STABILITY_WINDOW.w(), 1);
            blitScaledTinted(g, barLeft + STABILITY_WINDOW.u() + 2, failY, 2, 1, failTint);
            pose.pushPose();
            pose.translate(0, syFrac, 0);
            blitScaledTinted(g, barLeft + STABILITY_WINDOW.u(), sy, STABILITY_WINDOW.w(), 1, sColor);
            blitMarker(g, barLeft + 8, sy - (MARKER.h() - 1) / 2, sColor);
            pose.popPose();
            var stateWidth = font.width(stateText);
            var stateCenter = barLeft + barW - stateWidth / 4;
            drawSmall(g, font, stateText, stateCenter - stateWidth / 4, barTop + barH + 2, sColor);
            if (ElixirConfig.hudStabilityNumber) {
                var valueText = Integer.toString(predicted);
                drawSmall(g, font, valueText, stateCenter - font.width(valueText) / 4, barTop + barH + 10, sColor);
            }
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

    private static void blitReveal(GuiGraphics g, SpriteRect texture, SpriteRect window, int originX, int originY, float edge) {
        var head = Mth.clamp(edge - window.v(), 0f, window.h());
        var row = Mth.floor(head);
        if (row >= window.h()) return;
        var body = window.h() - row - 1;
        if (body > 0) {
            g.blit(THERMOMETER,
                    originX + window.u(), originY + window.v() + row + 1,
                    texture.u(), texture.v() + row + 1,
                    window.w(), body, THERMO_W, THERMO_H);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        tintAlpha(row + 1f - head);
        g.blit(THERMOMETER,
                originX + window.u(), originY + window.v() + row,
                texture.u(), texture.v() + row,
                window.w(), 1, THERMO_W, THERMO_H);
        RenderSystem.disableBlend();
        resetTint();
    }

    private static void tint(int color) {
        RenderSystem.setShaderColor(
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f);
    }

    private static void tintAlpha(float alpha) {
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
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

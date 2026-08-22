package net.per.elixir.render.entity.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.per.elixir.ElixirConfig;
import net.per.elixir.block.entity.ElixirFurnaceBlockEntity;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirFurnaceRenderer implements BlockEntityRenderer<ElixirFurnaceBlockEntity> {
    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/entity/temperature.png");
    private static final float W = 0.5f, H = 1.0f, D = 0.06f;

    public ElixirFurnaceRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ElixirFurnaceBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0, 0, -D);
        var tex = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        renderQuadTex(tex, poseStack.last(), 0, 0, D, W, H, D);
        renderQuadTex(tex, poseStack.last(), 0, 0, 0, W, H, 0);
        var fill = bufferSource.getBuffer(RenderType.GUI);
        renderQuad(fill, poseStack.last(), 0, H, 0, W, H, D, 0xff3a3a3a);
        renderQuad(fill, poseStack.last(), 0, 0, D, W, 0, 0, 0xff3a3a3a);
        renderQuad(fill, poseStack.last(), 0, 0, 0, 0, H, D, 0xff3a3a3a);
        renderQuad(fill, poseStack.last(), W, 0, D, W, H, 0, 0xff3a3a3a);
        var t = ((blockEntity.pharma() / (float) Math.max(1, ElixirConfig.pharmaLimited)) + 1) / 2f * 500;
        var range = blockEntity.tempRange > 0 ? blockEntity.tempRange : ElixirConfig.extremeTemperatureRange;
        var explode = Mth.clamp(t + range, 0, 500) / 500f;
        var low = Mth.clamp(t - range, 0, 500) / 500f;
        var temp = Mth.clamp(blockEntity.temperature, 0, 500) / 500f;
        var lz = D + 0.002f;
        renderQuad(fill, poseStack.last(), 0, H - 0.015f, lz, W, H, lz, 0xffffffff);
        renderQuad(fill, poseStack.last(), 0, explode, lz, W, explode + 0.02f, lz, 0xffff3b30);
        renderQuad(fill, poseStack.last(), 0, low, lz, W, low + 0.02f, lz, 0xff30a7ff);
        renderQuad(fill, poseStack.last(), 0, temp, lz, W, temp + 0.07f, lz, 0xffffd60a);
        poseStack.popPose();
    }

    private static void renderQuadTex(VertexConsumer buf, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2) {
        var matrix = pose.pose();
        buf.addVertex(matrix, x1, y1, z1).setColor(0xffffffff).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose, 0, 1, 0);
        buf.addVertex(matrix, x1, y2, z1).setColor(0xffffffff).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose, 0, 1, 0);
        buf.addVertex(matrix, x2, y2, z2).setColor(0xffffffff).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose, 0, 1, 0);
        buf.addVertex(matrix, x2, y1, z2).setColor(0xffffffff).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose, 0, 1, 0);
    }

    private static void renderQuad(VertexConsumer buf, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        var matrix = pose.pose();
        buf.addVertex(matrix, x1, y1, z1).setColor(color);
        buf.addVertex(matrix, x1, y2, z1).setColor(color);
        buf.addVertex(matrix, x2, y2, z2).setColor(color);
        buf.addVertex(matrix, x2, y1, z2).setColor(color);
    }
}

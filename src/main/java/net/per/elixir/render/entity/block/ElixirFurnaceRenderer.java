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
import net.per.elixir.block.entity.ElixirFurnaceBlockEntity;
import org.joml.Matrix4f;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirFurnaceRenderer implements BlockEntityRenderer<ElixirFurnaceBlockEntity> {
    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/entity/temperature.png");

    public ElixirFurnaceRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ElixirFurnaceBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0, 0, -0.001);
        renderTexture(bufferSource.getBuffer(RenderType.entityCutout(texture)), poseStack.last(), 0.5f, 1);
        poseStack.popPose();
        var y = Mth.clamp(blockEntity.temperature, 0, 500) / 500;
        poseStack.translate(0, y, -0.02);
        renderRect(bufferSource.getBuffer(RenderType.GUI), poseStack.last().pose(), 0.5f, 0.1f, 0xff000000);
    }

    private static void renderTexture(VertexConsumer buf, PoseStack.Pose pose, float width, float height) {
        var matrix = pose.pose();
        buf.addVertex(matrix, 0, 0, 0).setColor(0xffffffff).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose, 0, 1, 0);
        buf.addVertex(matrix, 0, height, 0).setColor(0xffffffff).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose, 0, 1, 0);
        buf.addVertex(matrix, width, height, 0).setColor(0xffffffff).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose, 0, 1, 0);
        buf.addVertex(matrix, width, 0, 0).setColor(0xffffffff).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose, 0, 1, 0);
    }

    private static void renderRect(VertexConsumer buf, Matrix4f matrix, float width, float height, int color) {
        buf.addVertex(matrix, 0, 0, 0).setColor(color);
        buf.addVertex(matrix, 0, height, 0).setColor(color);
        buf.addVertex(matrix, width, height, 0).setColor(color);
        buf.addVertex(matrix, width, 0, 0).setColor(color);
    }
}

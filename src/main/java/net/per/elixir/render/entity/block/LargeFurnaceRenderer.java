package net.per.elixir.render.entity.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.per.elixir.block.ElixirFurnaceBlock;
import net.per.elixir.block.entity.LargeFurnaceBlockEntity;
import net.per.elixir.registry.ElixirBlocks;

public class LargeFurnaceRenderer implements BlockEntityRenderer<LargeFurnaceBlockEntity> {

    public LargeFurnaceRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(LargeFurnaceBlockEntity be) {
        int n = Math.max(1, be.size());
        double r = (n - 1) / 2.0 + 1.0;
        return new AABB(be.getBlockPos()).inflate(r);
    }

    @Override
    public void render(LargeFurnaceBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var level = be.getLevel();
        var mc = Minecraft.getInstance();
        if (level == null) return;
        var dispatcher = mc.getBlockRenderer();
        int n = be.size();
        int half = (n - 1) / 2;
        int light = be.started() ? 0xF000F0 : packedLight;
        var buf = bufferSource.getBuffer(RenderType.cutout());

        var furnaceState = ElixirBlocks.elixir_furnace.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, be.facing())
                .setValue(ElixirFurnaceBlock.ACTIVE, be.started());
        var furnaceModel = dispatcher.getBlockModel(furnaceState);
        poseStack.pushPose();
        poseStack.translate(-half, -half, -half);
        poseStack.scale(n, n, n);
        dispatcher.getModelRenderer().renderModel(poseStack.last(), buf, furnaceState, furnaceModel, 1f, 1f, 1f, light, packedOverlay);
        poseStack.popPose();

        var coverState = ElixirBlocks.elixir_furnace_cover.get().defaultBlockState();
        var coverModel = dispatcher.getBlockModel(coverState);
        poseStack.pushPose();
        poseStack.translate(-half, n - half, -half);
        poseStack.scale(n, n, n);
        dispatcher.getModelRenderer().renderModel(poseStack.last(), buf, coverState, coverModel, 1f, 1f, 1f, light, packedOverlay);
        poseStack.popPose();
    }
}

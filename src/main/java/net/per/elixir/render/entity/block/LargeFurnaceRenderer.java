package net.per.elixir.render.entity.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.per.elixir.block.ElixirFurnaceBlock;
import net.per.elixir.block.entity.LargeFurnaceBlockEntity;
import net.per.elixir.event.ClientEvent;
import net.per.elixir.registry.ElixirBlocks;
import net.per.elixir.registry.data.FurnaceVisual;

import java.util.HashMap;
import java.util.Map;

public class LargeFurnaceRenderer implements BlockEntityRenderer<LargeFurnaceBlockEntity> {
    private static final Map<SwapKey, BakedModel> SWAP_CACHE = new HashMap<>();
    private static final int SWAP_CACHE_LIMIT = 256;

    private record SwapKey(BakedModel model, TextureAtlasSprite sprite, BlockState state) {
    }

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

        var visual = FurnaceVisual.getDefault(level);
        var part = visual != null ? visual.select(n, be.getBlockPos().asLong()) : null;
        var customModel = part != null && part.model().isPresent();

        var furnaceModel = customModel ? resolveModel(mc, part.model().get(), dispatcher.getBlockModel(furnaceState))
                : dispatcher.getBlockModel(furnaceState);
        furnaceModel = swapTexture(mc, furnaceModel, furnaceState, part != null ? part.texture().orElse(null) : null);
        poseStack.pushPose();
        poseStack.translate(-half, -half, -half);
        poseStack.scale(n, n, n);
        if (customModel) applyFacing(poseStack, be.facing());
        dispatcher.getModelRenderer().renderModel(poseStack.last(), buf, furnaceState, furnaceModel, 1f, 1f, 1f, light, packedOverlay);
        poseStack.popPose();

        var coverState = ElixirBlocks.elixir_furnace_cover.get().defaultBlockState();
        var customCover = part != null && part.coverModel().isPresent();
        var coverModel = customCover ? resolveModel(mc, part.coverModel().get(), dispatcher.getBlockModel(coverState))
                : dispatcher.getBlockModel(coverState);
        coverModel = swapTexture(mc, coverModel, coverState, part != null ? part.coverTexture().orElse(null) : null);
        poseStack.pushPose();
        poseStack.translate(-half, n - half, -half);
        poseStack.scale(n, n, n);
        dispatcher.getModelRenderer().renderModel(poseStack.last(), buf, coverState, coverModel, 1f, 1f, 1f, light, packedOverlay);
        poseStack.popPose();
    }

    private static void applyFacing(PoseStack poseStack, Direction facing) {
        float yaw = switch (facing) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            default -> 270f;
        };
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-0.5, 0, -0.5);
    }

    private static BakedModel resolveModel(Minecraft mc, ResourceLocation location, BakedModel fallback) {
        var model = mc.getModelManager().getModel(new ModelResourceLocation(location, "standalone"));
        if (model == mc.getModelManager().getMissingModel()) {
            model = mc.getModelManager().getModel(new ModelResourceLocation(location, ""));
        }
        if (model == mc.getModelManager().getMissingModel()) {
            model = mc.getModelManager().getModel(new ModelResourceLocation(location, "inventory"));
        }
        return model == mc.getModelManager().getMissingModel() ? fallback : model;
    }

    private static BakedModel swapTexture(Minecraft mc, BakedModel model, BlockState state, ResourceLocation texture) {
        if (texture == null) return model;
        var sprite = resolveSprite(mc, texture);
        if (sprite == null) return model;
        var key = new SwapKey(model, sprite, state);
        var cached = SWAP_CACHE.get(key);
        if (cached != null) return cached;
        if (SWAP_CACHE.size() >= SWAP_CACHE_LIMIT) SWAP_CACHE.clear();
        var swapped = new SpriteSwapModel(model, sprite, state);
        SWAP_CACHE.put(key, swapped);
        return swapped;
    }

    private static TextureAtlasSprite resolveSprite(Minecraft mc, ResourceLocation texture) {
        var blockAtlas = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        var sprite = blockAtlas.getSprite(texture);
        if (!sprite.atlasLocation().equals(MissingTextureAtlasSprite.getLocation())) return sprite;
        var elixirAtlas = mc.getModelManager().getAtlas(ClientEvent.ELIXIR_BLOCK_ATLAS);
        if (elixirAtlas == null) return null;
        sprite = elixirAtlas.getSprite(texture);
        return sprite.atlasLocation().equals(MissingTextureAtlasSprite.getLocation()) ? null : sprite;
    }
}

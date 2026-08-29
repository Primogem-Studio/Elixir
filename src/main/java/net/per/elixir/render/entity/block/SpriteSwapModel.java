package net.per.elixir.render.entity.block;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SpriteSwapModel implements BakedModel {
    private final BakedModel delegate;
    private final Map<Direction, List<BakedQuad>> quads;
    private final List<BakedQuad> noSideQuads;

    public SpriteSwapModel(BakedModel delegate, TextureAtlasSprite sprite, BlockState state) {
        this.delegate = delegate;
        var random = RandomSource.create();
        this.quads = new EnumMap<>(Direction.class);
        for (var side : Direction.values()) {
            this.quads.put(side, swap(delegate.getQuads(state, side, random), sprite));
        }
        this.noSideQuads = swap(delegate.getQuads(state, null, random), sprite);
    }

    private static List<BakedQuad> swap(List<BakedQuad> source, TextureAtlasSprite sprite) {
        if (source.isEmpty()) return List.of();
        var out = new ArrayList<BakedQuad>(source.size());
        for (var quad : source) {
            out.add(new BakedQuad(remap(quad.getVertices(), quad.getSprite(), sprite), quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade()));
        }
        return out;
    }

    private static int[] remap(int[] vertices, TextureAtlasSprite from, TextureAtlasSprite to) {
        var out = vertices.clone();
        float uScale = (to.getU1() - to.getU0()) / (from.getU1() - from.getU0());
        float vScale = (to.getV1() - to.getV0()) / (from.getV1() - from.getV0());
        for (int i = 0; i < out.length; i += 8) {
            float u = Float.intBitsToFloat(out[i + 4]);
            float v = Float.intBitsToFloat(out[i + 5]);
            out[i + 4] = Float.floatToRawIntBits(to.getU0() + (u - from.getU0()) * uScale);
            out[i + 5] = Float.floatToRawIntBits(to.getV0() + (v - from.getV0()) * vScale);
        }
        return out;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return side != null ? quads.get(side) : noSideQuads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return delegate.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return delegate.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return delegate.getOverrides();
    }
}

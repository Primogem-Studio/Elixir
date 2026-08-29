package net.per.elixir.registry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.per.elixir.registry.ElixirRegistries;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.per.elixir.Elixir.MOD_ID;

public record FurnaceVisual(
        Optional<ResourceLocation> model,
        Optional<ResourceLocation> texture,
        Optional<ResourceLocation> coverModel,
        Optional<ResourceLocation> coverTexture,
        Map<Integer, FurnaceVisual> tiers,
        boolean random,
        int fixed,
        List<FurnaceVisual> options
) {
    public static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "default");

    public static final Codec<FurnaceVisual> CODEC = Codec.recursive(
            "elixir:furnace_visual",
            self -> RecordCodecBuilder.create(instance -> instance.group(
                    ResourceLocation.CODEC.lenientOptionalFieldOf("model").forGetter(FurnaceVisual::model),
                    ResourceLocation.CODEC.lenientOptionalFieldOf("texture").forGetter(FurnaceVisual::texture),
                    ResourceLocation.CODEC.lenientOptionalFieldOf("cover_model").forGetter(FurnaceVisual::coverModel),
                    ResourceLocation.CODEC.lenientOptionalFieldOf("cover_texture").forGetter(FurnaceVisual::coverTexture),
                    Codec.unboundedMap(Codec.STRING.xmap(Integer::parseInt, String::valueOf), self)
                            .lenientOptionalFieldOf("tiers", Map.of()).forGetter(FurnaceVisual::tiers),
                    Codec.BOOL.lenientOptionalFieldOf("random", false).forGetter(FurnaceVisual::random),
                    Codec.INT.lenientOptionalFieldOf("fixed", 0).forGetter(FurnaceVisual::fixed),
                    self.listOf().lenientOptionalFieldOf("options", List.of()).forGetter(FurnaceVisual::options)
            ).apply(instance, FurnaceVisual::new))
    );

    public FurnaceVisual select(int size, long seed) {
        var part = this;
        if (!options.isEmpty()) {
            var index = random ? (int) Math.floorMod(seed, options.size()) : Math.clamp(fixed, 0, options.size() - 1);
            part = part.merge(options.get(index));
        }
        var tier = tiers.get(size);
        if (tier != null) part = part.merge(tier);
        return part;
    }

    public FurnaceVisual merge(FurnaceVisual other) {
        return new FurnaceVisual(
                other.model.isPresent() ? other.model : model,
                other.texture.isPresent() ? other.texture : texture,
                other.coverModel.isPresent() ? other.coverModel : coverModel,
                other.coverTexture.isPresent() ? other.coverTexture : coverTexture,
                tiers, random, fixed, options);
    }

    public static FurnaceVisual getDefault(Level level) {
        return level.registryAccess().registry(ElixirRegistries.FURNACE_VISUAL)
                .map(registry -> registry.get(DEFAULT_ID))
                .orElse(null);
    }
}

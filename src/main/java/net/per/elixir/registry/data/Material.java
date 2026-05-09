package net.per.elixir.registry.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.per.elixir.registry.ElixirRegistries;
import net.per.elixir.util.IElixirAction;
import net.per.elixir.util.IElixirCalc;

import java.util.List;

public record Material(Holder<Item> item, Either<Holder<IElixirAction>, Holder<IElixirCalc>> effect, int pharm,
                       double stability, double base,
                       boolean main, int[] colors) {
    private Material(Holder<Item> item, Holder<IElixirAction> effect, int pharm, double stability, List<Integer> colors) {
        this(item, Either.left(effect), pharm, stability, 0, true, colors.stream().mapToInt(Integer::intValue).toArray());
    }

    private Material(Holder<Item> item, Holder<IElixirCalc> calc, int pharm, double stability, double base) {
        this(item, Either.right(calc), pharm, stability, base, false, null);
    }

    private static final Codec<Material> MAIN_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(Material::item),
                    ElixirRegistries.ACTION_REGISTRY.holderByNameCodec().fieldOf("effect").forGetter(m -> m.effect.left().orElseThrow()),
                    Codec.INT.fieldOf("pharm").forGetter(Material::pharm),
                    Codec.DOUBLE.fieldOf("stability").forGetter(Material::stability),
                    Codec.INT.listOf().fieldOf("colors").forGetter(m -> IntList.of(m.colors))
            ).apply(instance, Material::new)
    );

    private static final Codec<Material> OFF_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(Material::item),
                    ElixirRegistries.CALCULATOR_REGISTRY.holderByNameCodec().fieldOf("calc").forGetter(m -> m.effect.right().orElseThrow()),
                    Codec.INT.fieldOf("pharm").forGetter(Material::pharm),
                    Codec.DOUBLE.fieldOf("stability").forGetter(Material::stability),
                    Codec.DOUBLE.fieldOf("base").forGetter(Material::base)
            ).apply(instance, Material::new)
    );

    public static final Codec<Material> CODEC = Codec.xor(MAIN_CODEC, OFF_CODEC).xmap(Either::unwrap, m -> m.main ? Either.left(m) : Either.right(m));
    public static final Codec<Holder<Material>> HOLDER_CODEC = RegistryFixedCodec.create(ElixirRegistries.MATERIAL);
}

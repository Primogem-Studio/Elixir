package net.per.elixir.data;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.per.elixir.registry.ElixirRegistries;
import net.per.elixir.registry.data.Material;

import java.util.List;

public record AlchemicalFormulaComponent(List<Content> main, List<Content> off) {
    public static final Codec<AlchemicalFormulaComponent> codec = RecordCodecBuilder.create(
            instance -> instance.group(
                    Content.codec.listOf().fieldOf("main").forGetter(AlchemicalFormulaComponent::main),
                    Content.codec.listOf().fieldOf("off").forGetter(AlchemicalFormulaComponent::off)
            ).apply(instance, AlchemicalFormulaComponent::new)
    );

    public record Content(Holder<Material> material, int count) {
        private static final Codec<Content> codec = RecordCodecBuilder.create(
                instance -> instance.group(
                        Material.HOLDER_CODEC.fieldOf("material").forGetter(Content::material),
                        ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(Content::count)
                ).apply(instance, Content::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, Content> streamCodec = StreamCodec.of((buf, c) -> {
            buf.writeById(buf.registryAccess().registryOrThrow(ElixirRegistries.MATERIAL).asHolderIdMap()::getId, c.material);
            buf.writeInt(c.count);
        }, buf -> new Content(buf.readById(buf.registryAccess().registryOrThrow(ElixirRegistries.MATERIAL).asHolderIdMap()::byId), buf.readInt()));
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemicalFormulaComponent> streamCodec = StreamCodec.ofMember(AlchemicalFormulaComponent::encode, AlchemicalFormulaComponent::decode);

    static void writeContents(RegistryFriendlyByteBuf buf, List<Content> list) {
        buf.writeVarInt(list.size());
        for (var c : list) Content.streamCodec.encode(buf, c);
    }

    static List<Content> readContent(RegistryFriendlyByteBuf buf) {
        var list = ImmutableList.<Content>builder();
        var size = buf.readVarInt();
        for (var i = 0; i < size; ++i) list.add(Content.streamCodec.decode(buf));
        return list.build();
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        writeContents(buf, main);
        writeContents(buf, off);
    }

    private static AlchemicalFormulaComponent decode(RegistryFriendlyByteBuf buf) {
        return new AlchemicalFormulaComponent(readContent(buf), readContent(buf));
    }
}
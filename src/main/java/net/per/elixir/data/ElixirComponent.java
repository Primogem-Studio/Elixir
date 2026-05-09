package net.per.elixir.data;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.per.elixir.registry.ElixirRegistries;
import net.per.elixir.registry.data.Material;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public record ElixirComponent(Holder<Material> off, int pharm, List<Holder<Material>> main) {
    public static final Codec<ElixirComponent> codec = RecordCodecBuilder.create(
            instance -> instance.group(
                    Material.HOLDER_CODEC.fieldOf("off").forGetter(ElixirComponent::off),
                    Codec.INT.fieldOf("pharm").forGetter(ElixirComponent::pharm),
                    Material.HOLDER_CODEC.listOf().fieldOf("main").forGetter(ElixirComponent::main)
            ).apply(instance, ElixirComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ElixirComponent> streamCodec = StreamCodec.ofMember(ElixirComponent::encode, ElixirComponent::decode);

    static void writeMaterials(RegistryFriendlyByteBuf buf, List<Holder<Material>> ms) {
        ToIntFunction<Holder<Material>> getId = buf.registryAccess().registryOrThrow(ElixirRegistries.MATERIAL).asHolderIdMap()::getId;
        buf.writeVarInt(ms.size());
        for (var m : ms) buf.writeById(getId, m);
    }

    static List<Holder<Material>> readMaterials(RegistryFriendlyByteBuf buf) {
        IntFunction<Holder<Material>> byId = buf.registryAccess().registryOrThrow(ElixirRegistries.MATERIAL).asHolderIdMap()::byId;
        var list = ImmutableList.<Holder<Material>>builder();
        var size = buf.readVarInt();
        for (var i = 0; i < size; ++i) list.add(buf.readById(byId));
        return list.build();
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeById(buf.registryAccess().registryOrThrow(ElixirRegistries.MATERIAL).asHolderIdMap()::getId, off);
        buf.writeInt(pharm);
        writeMaterials(buf, main);
    }

    private static ElixirComponent decode(RegistryFriendlyByteBuf buf) {
        return new ElixirComponent(buf.readById(buf.registryAccess().registryOrThrow(ElixirRegistries.MATERIAL).asHolderIdMap()::byId), buf.readInt(), readMaterials(buf));
    }
}

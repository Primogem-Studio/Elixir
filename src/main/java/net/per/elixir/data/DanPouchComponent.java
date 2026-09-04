package net.per.elixir.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record DanPouchComponent(int selected, List<ItemStack> items) {
    public static final int SIZE = 8;

    public static final Codec<DanPouchComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("selected").forGetter(DanPouchComponent::selected),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(DanPouchComponent::items)
    ).apply(instance, DanPouchComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DanPouchComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            DanPouchComponent::selected,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
            DanPouchComponent::items,
            DanPouchComponent::new
    );

    public DanPouchComponent {
        var fixed = new ArrayList<ItemStack>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            var stack = i < items.size() ? items.get(i) : ItemStack.EMPTY;
            fixed.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        items = List.copyOf(fixed);
        if (selected < -1 || selected >= SIZE) selected = -1;
    }

    public static DanPouchComponent empty() {
        return new DanPouchComponent(-1, List.of());
    }

    public ItemStack get(int slot) {
        return items.get(slot);
    }

    public DanPouchComponent setItem(int slot, ItemStack stack) {
        var copy = new ArrayList<>(items);
        copy.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        return new DanPouchComponent(selected, copy);
    }

    public DanPouchComponent withSelected(int slot) {
        return slot == selected ? this : new DanPouchComponent(slot, items);
    }
}

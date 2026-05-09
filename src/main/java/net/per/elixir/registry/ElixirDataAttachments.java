package net.per.elixir.registry;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);
    public static final Supplier<AttachmentType<Float>> ELIXIR_EXP = ATTACHMENT_TYPES.register("elixir_exp", () -> AttachmentType.builder(() -> 0f).serialize(Codec.FLOAT).build());
}

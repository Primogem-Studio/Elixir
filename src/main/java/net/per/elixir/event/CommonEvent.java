package net.per.elixir.event;

import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.per.elixir.item.ElixirItem;
import net.per.elixir.util.ElixirHelper;

import java.nio.file.Path;

import static net.per.elixir.Elixir.MOD_ID;
import static net.per.elixir.registry.ElixirDataAttachments.ELIXIR_EXP;

@EventBusSubscriber(modid = MOD_ID)
public class CommonEvent {
    @SubscribeEvent
    private static void onPlayerClone(PlayerEvent.Clone event) {
        event.getEntity().setData(ELIXIR_EXP, event.getOriginal().getData(ELIXIR_EXP));
    }

    @SubscribeEvent
    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        var player = event.getEntity();
        if (!player.isShiftKeyDown()) return;
        var stack = player.getItemInHand(event.getHand());
        if (stack.getItem() instanceof ElixirItem) {
            ElixirItem.launch(player, stack);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private static void onServerStarted(ServerStartedEvent event) {
        ElixirHelper.flush(event.getServer().registryAccess());
    }

    @SubscribeEvent
    private static void onAddPackFinders(AddPackFindersEvent event) {
        event.addRepositorySource(new FolderRepositorySource(FMLPaths.getOrCreateGameRelativePath(Path.of("elixir")), event.getPackType(), PackSource.DEFAULT, new DirectoryValidator(p -> true)));
    }
}

package net.per.elixir.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.per.elixir.event.ModClientEvents;
import net.per.elixir.item.DanPouchItem;
import net.per.elixir.network.SelectPillPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class DanWheelClient {
    private static boolean wasHeld;

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        var key = ModClientEvents.DAN_WHEEL.getKey();
        boolean held = key.getType() == InputConstants.Type.KEYSYM
                ? InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue())
                : ModClientEvents.DAN_WHEEL.isDown();
        boolean hasPouch = mc.player.getMainHandItem().getItem() instanceof DanPouchItem;
        if (mc.screen == null) {
            if (held && !wasHeld && hasPouch) {
                mc.setScreen(new DanWheelScreen());
            }
        } else if (mc.screen instanceof DanWheelScreen wheel && !held) {
            wheel.commitNow();
        }
        wasHeld = held;
    }

    public static void sendSelected(int slot) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var pouch = mc.player.getMainHandItem();
        if (!(pouch.getItem() instanceof DanPouchItem)) return;
        if (DanPouchItem.selectedSlot(pouch) == slot) return;
        DanPouchItem.setSelected(pouch, slot);
        PacketDistributor.sendToServer(new SelectPillPayload(slot));
    }
}

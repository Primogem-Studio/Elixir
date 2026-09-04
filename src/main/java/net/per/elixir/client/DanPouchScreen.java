package net.per.elixir.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.per.elixir.Elixir;
import net.per.elixir.data.DanPouchMenu;
import net.per.elixir.item.DanPouchItem;

public class DanPouchScreen extends AbstractContainerScreen<DanPouchMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Elixir.MOD_ID, "textures/gui/dan_pouch.png");

    public DanPouchScreen(DanPouchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = DanPouchMenu.IMAGE_W;
        imageHeight = DanPouchMenu.IMAGE_H;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        int sel = selectedSlot();
        if (sel >= 0 && sel < 8) {
            var slot = menu.slots.get(sel);
            int x0 = leftPos + slot.x - 2;
            int y0 = topPos + slot.y - 2;
            graphics.fill(x0, y0, x0 + 20, y0 + 20, 0x4DB3FF59);
            graphics.fill(x0, y0, x0 + 20, y0 + 1, 0xFF7BFF2E);
            graphics.fill(x0, y0 + 19, x0 + 20, y0 + 20, 0xFF7BFF2E);
            graphics.fill(x0, y0, x0 + 1, y0 + 20, 0xFF7BFF2E);
            graphics.fill(x0 + 19, y0, x0 + 20, y0 + 20, 0xFF7BFF2E);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private int selectedSlot() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return -1;
        return DanPouchItem.selectedSlot(mc.player.getMainHandItem());
    }
}

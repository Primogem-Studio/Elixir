package net.per.elixir.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.per.elixir.data.ElixirFurnaceMenu;

import static net.per.elixir.Elixir.MOD_ID;

public class ElixirFurnaceScreen extends AbstractContainerScreen<ElixirFurnaceMenu> {
    private static final ResourceLocation bg = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/screens/elixir_furnace.png");

    public ElixirFurnaceScreen(ElixirFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 180;
        imageHeight = 180;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(bg, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }
}

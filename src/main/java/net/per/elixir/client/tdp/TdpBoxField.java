package net.per.elixir.client.tdp;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public interface TdpBoxField {
    void setPos(int x, int y);

    void render(GuiGraphics g, Font font, int mouseX, int mouseY);

    int w();
}

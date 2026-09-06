package net.per.elixir.client.tdp;

import net.minecraft.client.gui.GuiGraphics;

public interface TdpPage {
    String titleKey();

    default void show(TdpScreen host) {
    }

    default void refresh(TdpScreen host) {
    }

    void render(TdpScreen host, GuiGraphics g, int mouseX, int mouseY, float partialTick);

    default boolean mouseClicked(TdpScreen host, double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseReleased(TdpScreen host, double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(TdpScreen host, double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    default boolean mouseScrolled(TdpScreen host, double mouseX, double mouseY, double amount) {
        return false;
    }

    default boolean charTyped(TdpScreen host, char codePoint, int modifiers) {
        return false;
    }

    default boolean keyPressed(TdpScreen host, int keyCode, int scanCode, int modifiers) {
        return false;
    }
}

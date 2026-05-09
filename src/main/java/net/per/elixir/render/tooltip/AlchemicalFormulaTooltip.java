package net.per.elixir.render.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.per.elixir.data.AlchemicalFormulaComponent;
import org.joml.Matrix4f;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AlchemicalFormulaTooltip implements TooltipComponent, ClientTooltipComponent {
    private static final Component title1 = Component.translatable("tooltip.alchemical_formula.main");
    private static final Component title2 = Component.translatable("tooltip.alchemical_formula.off");
    private final AlchemicalFormulaComponent data;

    public AlchemicalFormulaTooltip(AlchemicalFormulaComponent data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return mainHeight() + data.off().size() / 5 * 16 + 10 + 18;
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(data.main().size() > 5 ? 80 : data.main().size() * 16, data.off().size() > 5 ? 80 : data.off().size() * 16);
    }

    private int mainHeight() {
        return data.main().size() / 5 * 16 + 10 + 18;
    }

    @Override
    public void renderText(Font font, int mouseX, int mouseY, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        font.drawInBatch(title1, mouseX, mouseY, 0xB4FF59, false, matrix, bufferSource, Font.DisplayMode.POLYGON_OFFSET, 0, 0xf000f0);
        font.drawInBatch(title2, mouseX, mouseY + 2 + mainHeight(), 0xB4FF59, false, matrix, bufferSource, Font.DisplayMode.POLYGON_OFFSET, 0, 0xf000f0);
    }

    private static void renderContents(List<AlchemicalFormulaComponent.Content> contents, Font font, int x, int y, int bound, GuiGraphics graphics) {
        var ox = x;
        for (var c : contents) {
            var it = new ItemStack(c.material().value().item(), c.count());
            graphics.renderItem(it, x, y);
            graphics.renderItemDecorations(font, it, x, y);
            x += 16;
            if (x > bound) {
                y += 16;
                x = ox;
            }
        }
    }

    @Override
    public void renderImage(Font font, int mouseX, int mouseY, GuiGraphics graphics) {
        renderContents(data.main(), font, mouseX - 1, mouseY + 10, mouseX + 64, graphics);
        renderContents(data.off(), font, mouseX - 1, mouseY + 10 + mainHeight(), mouseX + 64, graphics);
    }
}

package net.per.elixir.client;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TextHelper {
    public static FormattedText tailByWidth(Font font, FormattedText text, int tailWidth, Style defaultStyle) {
        if (tailWidth <= 0 || text == FormattedText.EMPTY) return FormattedText.EMPTY;
        var sp = font.getSplitter();
        var segments = new ArrayList<Pair<Style, String>>();
        text.visit((style, string) -> {
            if (!string.isEmpty()) segments.add(Pair.of(style, string));
            return Optional.empty();
        }, defaultStyle);
        if (segments.isEmpty()) return FormattedText.EMPTY;
        int totalWidth = 0;
        for (var entry : segments) totalWidth += (int) sp.stringWidth(entry.right());
        if (tailWidth >= totalWidth) return text;
        int headWidth = totalWidth - tailWidth;
        var tailComponent = Component.empty();
        int accumulatedWidth = 0;
        boolean inTail = false;
        for (var entry : segments) {
            var style = entry.left();
            var full = entry.right();
            int segWidth = (int) sp.stringWidth(full);
            if (!inTail) {
                if (accumulatedWidth + segWidth <= headWidth) {
                    accumulatedWidth += segWidth;
                } else {
                    int remainingHeadWidth = headWidth - accumulatedWidth;
                    if (remainingHeadWidth <= 0) {
                        appendStyledText(tailComponent, full, style);
                    } else {
                        var headPart = sp.plainHeadByWidth(full, remainingHeadWidth, style);
                        int splitIndex = headPart.length();
                        if (splitIndex < full.length()) {
                            var tailPart = full.substring(splitIndex);
                            appendStyledText(tailComponent, tailPart, style);
                        }
                    }
                    inTail = true;
                }
            } else appendStyledText(tailComponent, full, style);
        }
        if (isEmpty(tailComponent)) return FormattedText.EMPTY;
        return tailComponent;
    }

    public static FormattedText tailByWidth(Font font, FormattedText text, int tailWidth) {
        return tailByWidth(font, text, tailWidth, Style.EMPTY);
    }

    public static List<FormattedText> splitLines(Font font, FormattedText text, int maxWidth) {
        var sp = font.getSplitter();
        var lines = new ArrayList<FormattedText>();
        while (!text.getString().isEmpty()) {
            var line = sp.headByWidth(text, maxWidth, Style.EMPTY);
            lines.add(line);
            text = tailByWidth(font, text, font.width(text) - font.width(line));
        }
        return lines;
    }

    public static void drawWrap(Font font, GuiGraphics g, FormattedText text, int x, int y, int lineWidth, int color) {
        drawWrap(font, g, splitLines(font, text, lineWidth), x, y, color);
    }

    public static void drawWrap(Font font, GuiGraphics g, List<FormattedText> text, int x, int y, int color) {
        for (var cs : Language.getInstance().getVisualOrder(text)) {
            g.drawString(font, cs, x, y, color, false);
            y += 9;
        }
    }

    public static boolean isEmpty(Component c) {
        return c.getSiblings().isEmpty() && c.getContents().equals(Component.empty().getContents());
    }

    private static void appendStyledText(MutableComponent target, String text, Style style) {
        if (!text.isEmpty()) target.append(Component.literal(text).setStyle(style));
    }
}

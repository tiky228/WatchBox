package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to build book pages with automatic pagination based on line count.
 */
public class BookPageBuilder {
    private final int maxLines;
    private final List<Component> pages = new ArrayList<>();
    private TextComponent.Builder builder = Component.text();
    private int lines = 0;

    public BookPageBuilder(int maxLines) {
        this.maxLines = maxLines;
    }

    public void addLine(Component component) {
        ensureSpace();
        builder.append(component).append(Component.newline());
        lines++;
    }

    public void addBlankLine() {
        addLine(Component.text(""));
    }

    public void addHeading(String text, net.kyori.adventure.text.format.NamedTextColor color) {
        addLine(Component.text(text, color));
    }

    public void newPage() {
        if (lines > 0 || pages.isEmpty()) {
            pages.add(builder.build());
        }
        builder = Component.text();
        lines = 0;
    }

    public List<Component> build() {
        if (lines > 0 || pages.isEmpty()) {
            pages.add(builder.build());
        }
        return pages;
    }

    private void ensureSpace() {
        if (lines >= maxLines) {
            newPage();
        }
    }
}

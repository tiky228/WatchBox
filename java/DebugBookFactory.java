package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a debug control book with live state and command shortcuts.
 */
public class DebugBookFactory {
    private static final int LINES_PER_PAGE = 12;
    private final NamespacedKey debugKey;

    public DebugBookFactory(JavaPlugin plugin) {
        this.debugKey = new NamespacedKey(plugin, "debug_book");
    }

    public ItemStack createDebugBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text("Debug Controls", NamedTextColor.AQUA));
        meta.author(Component.text("WatchBox"));

        List<Component> pages = new ArrayList<>();
        pages.add(buildIntroPage());
        pages.addAll(buildPlayerPages());

        meta.pages(pages);
        meta.getPersistentDataContainer().set(debugKey, PersistentDataType.BYTE, (byte) 1);
        book.setItemMeta(meta);
        return book;
    }

    public boolean isDebugBook(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(debugKey, PersistentDataType.BYTE);
    }

    private Component buildIntroPage() {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text("Debug Control Menu", NamedTextColor.DARK_AQUA)).append(Component.newline());
        builder.append(Component.text("Use the actions on the following pages to manage players.", NamedTextColor.GRAY));
        return builder.build();
    }

    private List<Component> buildPlayerPages() {
        BookPageBuilder builder = new BookPageBuilder(LINES_PER_PAGE);
        builder.addHeading("Players", NamedTextColor.GREEN);
        for (Player player : Bukkit.getOnlinePlayers()) {
            builder.addLine(buildPlayerLine(player));
        }
        return builder.build();
    }

    private Component buildPlayerLine(Player player) {
        return Component.text(player.getName(), NamedTextColor.AQUA)
                .append(Component.space())
                .append(button("+Mark", "/maniacdebug mark " + player.getName(), NamedTextColor.GREEN))
                .append(Component.space())
                .append(button("ClearMarks", "/maniacdebug clearmarks " + player.getName(), NamedTextColor.DARK_RED))
                .append(Component.space())
                .append(button("SetManiac", "/role set " + player.getName() + " maniac", NamedTextColor.RED))
                .append(Component.space())
                .append(button("SetCivilian", "/role set " + player.getName() + " civilian", NamedTextColor.GRAY))
                .append(Component.space())
                .append(button("Kill", "/maniacdebug kill " + player.getName(), NamedTextColor.DARK_GRAY))
                .append(Component.space())
                .append(button("Respawn", "/maniacdebug respawn " + player.getName(), NamedTextColor.AQUA));
    }

    private Component button(String text, String command, NamedTextColor color) {
        return Component.text("[" + text + "]", color).clickEvent(ClickEvent.runCommand(command));
    }
}

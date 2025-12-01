package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import java.util.stream.Collectors;

/**
 * Generates a debug control book with live state and command shortcuts.
 */
public class DebugBookFactory {
    private final RoleManager roleManager;
    private final RoundManager roundManager;
    private final NamespacedKey debugKey;

    public DebugBookFactory(JavaPlugin plugin, RoleManager roleManager, RoundManager roundManager, MarkManager markManager) {
        this.roleManager = roleManager;
        this.roundManager = roundManager;
        this.debugKey = new NamespacedKey(plugin, "debug_book");
    }

    public ItemStack createDebugBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text("Debug Controls", NamedTextColor.AQUA));
        meta.author(Component.text("WatchBox"));

        List<Component> pages = new ArrayList<>();
        pages.addAll(buildStatePages());
        pages.add(buildRoundControlPage());
        pages.add(buildPhaseControlPage());
        pages.add(buildMarkPage());
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

    private List<Component> buildStatePages() {
        PageAccumulator accumulator = new PageAccumulator(10, Component.text("Game State", NamedTextColor.GREEN));
        accumulator.addLine(Component.text("Phase: " + roundManager.getCurrentPhase(), NamedTextColor.AQUA));
        accumulator.addLine(Component.text("Time Left: " + roundManager.getRemainingSeconds() + "s", NamedTextColor.AQUA));
        accumulator.addBlankLine();

        appendPlayerList(accumulator, "Alive Civilians:", NamedTextColor.GREEN, getPlayersByRole(Role.CIVILIAN));
        appendPlayerList(accumulator, "Alive Maniacs:", NamedTextColor.RED, getPlayersByRole(Role.MANIAC));
        appendPlayerList(accumulator, "Spectators / Dead:", NamedTextColor.DARK_GRAY, getSpectators());

        return accumulator.buildPages();
    }

    private Component buildRoundControlPage() {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text("Round Controls", NamedTextColor.GREEN)).append(Component.newline());
        builder.append(clickable("Start", "/maniacdebug start", NamedTextColor.GREEN)).append(Component.newline());
        builder.append(clickable("Stop", "/maniacdebug stop", NamedTextColor.RED)).append(Component.newline());
        builder.append(clickable("Next Phase", "/maniacdebug nextphase", NamedTextColor.AQUA)).append(Component.newline());
        return builder.build();
    }

    private Component buildPhaseControlPage() {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text("Phase Overrides", NamedTextColor.GREEN)).append(Component.newline());
        builder.append(clickable("ACTION", "/maniacdebug phase action", NamedTextColor.AQUA)).append(Component.newline());
        builder.append(clickable("DISCUSSION", "/maniacdebug phase discussion", NamedTextColor.AQUA)).append(Component.newline());
        builder.append(clickable("VOTING", "/maniacdebug phase voting", NamedTextColor.AQUA)).append(Component.newline());
        builder.append(Component.newline());
        builder.append(clickable("Give Voting Book", "/maniac votebook", NamedTextColor.GREEN));
        return builder.build();
    }

    private Component buildMarkPage() {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text("Marks & Debug", NamedTextColor.GREEN)).append(Component.newline());
        builder.append(clickable("List Marks", "/maniacdebug listmarks", NamedTextColor.AQUA)).append(Component.newline());
        builder.append(Component.text("Add mark to player:", NamedTextColor.DARK_GRAY)).append(Component.newline());
        for (Player player : Bukkit.getOnlinePlayers()) {
            builder.append(clickable(player.getName(), "/maniacdebug mark " + player.getName(), NamedTextColor.GRAY)).append(Component.newline());
        }
        builder.append(Component.newline());
        builder.append(Component.text("Clear / Empowered:", NamedTextColor.DARK_GRAY)).append(Component.newline());
        for (Player player : Bukkit.getOnlinePlayers()) {
            builder.append(clickable("Clear " + player.getName(), "/maniacdebug clearmarks " + player.getName(), NamedTextColor.RED)).append(Component.newline());
            builder.append(clickable("Empower " + player.getName(), "/maniacdebug emark " + player.getName(), NamedTextColor.LIGHT_PURPLE)).append(Component.newline());
        }
        return builder.build();
    }

    private List<String> getPlayersByRole(Role role) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR && !p.isDead())
                .filter(p -> roleManager.getRole(p) == role)
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> getSpectators() {
        List<String> spectators = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                spectators.add(player.getName());
            }
        }
        return spectators;
    }

    private Component clickable(String label, String command, NamedTextColor color) {
        return Component.text(label, color).clickEvent(ClickEvent.runCommand(command));
    }

    private void appendPlayerList(PageAccumulator accumulator, String header, NamedTextColor color, List<String> names) {
        accumulator.addLine(Component.text(header, color));
        if (names.isEmpty()) {
            accumulator.addLine(Component.text("- none", NamedTextColor.DARK_GRAY));
        } else {
            for (String name : names) {
                accumulator.addLine(Component.text("- " + name, NamedTextColor.GRAY));
            }
        }
        accumulator.addBlankLine();
    }

    private static class PageAccumulator {
        private final int maxLines;
        private final List<Component> pages = new ArrayList<>();
        private TextComponent.Builder builder = Component.text();
        private int lines = 0;
        private final Component header;

        PageAccumulator(int maxLines, Component header) {
            this.maxLines = maxLines;
            this.header = header;
            addHeader();
        }

        void addLine(Component line) {
            builder.append(line).append(Component.newline());
            lines++;
            checkPage();
        }

        void addBlankLine() {
            addLine(Component.text(""));
        }

        List<Component> buildPages() {
            if (lines > 0 || pages.isEmpty()) {
                pages.add(builder.build());
            }
            return pages;
        }

        private void checkPage() {
            if (lines >= maxLines) {
                pages.add(builder.build());
                builder = Component.text();
                lines = 0;
                addHeader();
            }
        }

        private void addHeader() {
            if (header != null) {
                builder.append(header).append(Component.newline());
                lines++;
            }
        }
    }
}

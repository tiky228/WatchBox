package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
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
    private static final int LINES_PER_PAGE = 13;
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
        pages.addAll(buildRoundControlPages());
        pages.addAll(buildMarkPages());
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
        BookPageBuilder builder = new BookPageBuilder(LINES_PER_PAGE);
        builder.addHeading("Game State", NamedTextColor.DARK_AQUA);
        builder.addLine(Component.text("Phase: " + roundManager.getCurrentPhase(), NamedTextColor.AQUA));
        builder.addLine(Component.text("Time Left: " + roundManager.getRemainingSeconds() + "s", NamedTextColor.AQUA));
        builder.addBlankLine();

        appendPlayerList(builder, "Alive Civilians:", NamedTextColor.GREEN, getPlayersByRole(Role.CIVILIAN));
        appendPlayerList(builder, "Alive Maniacs:", NamedTextColor.RED, getPlayersByRole(Role.MANIAC));
        appendPlayerList(builder, "Spectators / Dead:", NamedTextColor.DARK_GRAY, getSpectators());
        return builder.build();
    }

    private List<Component> buildRoundControlPages() {
        BookPageBuilder builder = new BookPageBuilder(LINES_PER_PAGE);

        builder.addHeading("Round Controls", NamedTextColor.DARK_GREEN);
        builder.addLine(clickable("Start", "/maniacdebug start", NamedTextColor.GREEN));
        builder.addLine(clickable("Stop", "/maniacdebug stop", NamedTextColor.RED));
        builder.addLine(clickable("Next Phase", "/maniacdebug nextphase", NamedTextColor.AQUA));
        builder.addBlankLine();

        builder.addHeading("Phase Overrides", NamedTextColor.DARK_GREEN);
        builder.addLine(clickable("ACTION", "/maniacdebug phase action", NamedTextColor.AQUA));
        builder.addLine(clickable("DISCUSSION", "/maniacdebug phase discussion", NamedTextColor.AQUA));
        builder.addLine(clickable("VOTING", "/maniacdebug phase voting", NamedTextColor.AQUA));
        builder.addBlankLine();
        builder.addLine(clickable("Give Voting Book", "/maniac votebook", NamedTextColor.GREEN));

        return builder.build();
    }

    private List<Component> buildMarkPages() {
        BookPageBuilder builder = new BookPageBuilder(LINES_PER_PAGE);
        builder.addHeading("Marks & Debug", NamedTextColor.DARK_AQUA);
        builder.addLine(clickable("List Marks", "/maniacdebug listmarks", NamedTextColor.AQUA));
        builder.addBlankLine();

        builder.addHeading("Add mark to player:", NamedTextColor.GRAY);
        appendPlayerCommands(builder, Bukkit.getOnlinePlayers(), "/maniacdebug mark %s", NamedTextColor.GRAY);

        builder.addBlankLine();
        builder.addHeading("Clear / Empowered:", NamedTextColor.GRAY);
        for (Player player : Bukkit.getOnlinePlayers()) {
            builder.addLine(clickable("Clear " + player.getName(), "/maniacdebug clearmarks " + player.getName(), NamedTextColor.RED));
            builder.addLine(clickable("Empower " + player.getName(), "/maniacdebug emark " + player.getName(), NamedTextColor.LIGHT_PURPLE));
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

    private void appendPlayerList(BookPageBuilder builder, String header, NamedTextColor color, List<String> names) {
        builder.addHeading(header, color);
        if (names.isEmpty()) {
            builder.addLine(Component.text("- none", NamedTextColor.DARK_GRAY));
        } else {
            for (String name : names) {
                builder.addLine(Component.text("- " + name, NamedTextColor.GRAY));
            }
        }
        builder.addBlankLine();
    }

    private void appendPlayerCommands(BookPageBuilder builder, Iterable<Player> players, String commandFormat, NamedTextColor color) {
        for (Player player : players) {
            String command = String.format(commandFormat, player.getName());
            builder.addLine(clickable(player.getName(), command, color));
        }
    }
}

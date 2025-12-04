package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Generates a debug control book with live state and command shortcuts.
 */
public class DebugBookFactory {
    public static final Map<String, Consumer<Player>> DEBUG_ACTIONS = new ConcurrentHashMap<>();

    private final NamespacedKey debugKey;
    private final RoundManager roundManager;
    private final RoleManager roleManager;
    private final MarkManager markManager;

    public DebugBookFactory(JavaPlugin plugin, RoundManager roundManager, RoleManager roleManager, MarkManager markManager) {
        this.debugKey = new NamespacedKey(plugin, "debug_book");
        this.roundManager = roundManager;
        this.roleManager = roleManager;
        this.markManager = markManager;
    }

    public ItemStack createDebugBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text("Debug Controls", NamedTextColor.AQUA));
        meta.author(Component.text("WatchBox"));

        List<Component> pages = buildPages();
        meta.pages(pages.toArray(new Component[0]));
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

    public boolean executeAction(String actionId, Player player) {
        Consumer<Player> action = DEBUG_ACTIONS.get(actionId);
        if (action == null) {
            player.sendMessage(Component.text("That debug action is no longer available.", NamedTextColor.RED));
            return false;
        }
        action.accept(player);
        return true;
    }

    private List<Component> buildPages() {
        List<Component> pages = new ArrayList<>();
        pages.add(buildPlayersPage());
        pages.add(buildPlayerManipulationLauncher());
        return pages;
    }

    private Component buildPlayersPage() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Players debug", NamedTextColor.DARK_AQUA));
        lines.add(Component.text("Click below to view player roles and status in chat.", NamedTextColor.GRAY));
        lines.add(Component.empty());
        lines.add(Component.text("[Open players debug in chat]", NamedTextColor.AQUA, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/maniacdebug players")));
        return joinLines(lines);
    }

    private Component buildPlayerManipulationLauncher() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("PlayerManipulation", NamedTextColor.GOLD));
        lines.add(Component.empty());
        lines.add(Component.text("[OpenPlayerList]", NamedTextColor.AQUA, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/maniac debug players")));
        return joinLines(lines);
    }

    private Component buildMatchInfoLine() {
        return Component.text()
                .append(Component.text("Phase: ", NamedTextColor.BLUE))
                .append(Component.text(roundManager.getCurrentPhase().name(), NamedTextColor.AQUA))
                .append(Component.text(" | Remaining: ", NamedTextColor.BLUE))
                .append(Component.text(roundManager.getRemainingSeconds() + "s", NamedTextColor.AQUA))
                .append(Component.text(" | Maniacs: ", NamedTextColor.BLUE))
                .append(Component.text(String.valueOf(roundManager.countAliveManiacs()), NamedTextColor.RED))
                .append(Component.text(" | Civilians: ", NamedTextColor.BLUE))
                .append(Component.text(String.valueOf(roundManager.countAliveCivilians()), NamedTextColor.GREEN))
                .append(Component.text(" | Spectators: ", NamedTextColor.BLUE))
                .append(Component.text(String.valueOf(roundManager.countSpectators()), NamedTextColor.DARK_GRAY))
                .build();
    }

    public void sendManipulationInfo(Player player) {
        player.sendMessage(Component.text("Manipulation overview", NamedTextColor.DARK_AQUA));
        player.sendMessage(buildMatchInfoLine());

        List<Player> marked = roundManager.getMarkedPlayersThisRound();
        if (marked.isEmpty()) {
            player.sendMessage(Component.text("Marked players: none this round.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Marked players: ", NamedTextColor.YELLOW)
                    .append(listNames(marked, NamedTextColor.YELLOW)));
        }

        player.sendMessage(Component.text("Use /maniacdebug start|stop|nextphase or /maniac commands for deeper control.", NamedTextColor.DARK_GRAY));
    }

    public void sendPlayersDebugInfo(Player viewer) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        viewer.sendMessage(Component.text("PlayerManipulation", NamedTextColor.DARK_AQUA));
        if (players.isEmpty()) {
            viewer.sendMessage(Component.text("No players online.", NamedTextColor.GRAY));
            return;
        }

        for (Player target : players) {
            String name = target.getName();
            Component line = Component.text(name, NamedTextColor.GRAY)
                    .append(Component.space())
                    .append(manipulationButton("KILL", NamedTextColor.RED, "/maniac debug kill " + name))
                    .append(Component.space())
                    .append(manipulationButton("REVIVE", NamedTextColor.GREEN, "/maniac debug revive " + name))
                    .append(Component.space())
                    .append(manipulationButton("SET MANIAC", NamedTextColor.DARK_RED, "/maniac debug setrole maniac " + name))
                    .append(Component.space())
                    .append(manipulationButton("SET CIVILIAN", NamedTextColor.AQUA, "/maniac debug setrole civilian " + name));
            viewer.sendMessage(line);
        }
    }

    private Component button(String text, NamedTextColor color, Consumer<Player> action) {
        String actionId = registerAction(action);
        return Component.text("[" + text + "]", color).clickEvent(ClickEvent.runCommand("/maniacdebug " + actionId));
    }

    private Component manipulationButton(String text, NamedTextColor color, String command) {
        return Component.text("[" + text + "]", color, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand(command));
    }

    private String registerAction(Consumer<Player> action) {
        String id = UUID.randomUUID().toString();
        DEBUG_ACTIONS.put(id, action);
        return id;
    }

    private Component joinLines(List<Component> lines) {
        return Component.join(JoinConfiguration.newlines(), lines);
    }
}

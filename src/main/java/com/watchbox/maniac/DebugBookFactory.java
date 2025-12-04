package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
        pages.add(buildManipulationPage());
        pages.add(buildMatchInfoPage());
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

    private Component buildManipulationPage() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Manipulation", NamedTextColor.GOLD));
        lines.add(Component.text("Click the button below to close the book and print live manipulation info to chat.", NamedTextColor.GRAY));
        lines.add(Component.empty());
        lines.add(Component.text("[Open manipulation commands]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/maniacdebug manip")));
        lines.add(Component.text("The chat output always uses the latest game state.", NamedTextColor.DARK_GRAY));
        return joinLines(lines);
    }

    private Component buildMatchInfoPage() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Match Info", NamedTextColor.BLUE));
        lines.add(buildMatchInfoLine());

        List<Player> maniacs = roundManager.getAlivePlayers().stream()
                .filter(roleManager::isManiac)
                .toList();
        List<Player> innocents = roundManager.getAlivePlayers().stream()
                .filter(player -> !roleManager.isManiac(player))
                .toList();
        List<Player> spectators = Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getGameMode() == GameMode.SPECTATOR || player.isDead())
                .toList();

        lines.add(Component.text("Maniacs: ", NamedTextColor.RED).append(listNames(maniacs, NamedTextColor.RED)));
        lines.add(Component.text("Innocents: ", NamedTextColor.GREEN).append(listNames(innocents, NamedTextColor.GREEN)));
        lines.add(Component.text("Spectators: ", NamedTextColor.DARK_GRAY).append(listNames(spectators, NamedTextColor.DARK_GRAY)));

        List<Player> marked = roundManager.getMarkedPlayersThisRound();
        lines.add(Component.text("Marked this round: ", NamedTextColor.YELLOW)
                .append(marked.isEmpty() ? Component.text("None", NamedTextColor.GRAY) : listNames(marked, NamedTextColor.YELLOW)));

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

        viewer.sendMessage(Component.text("Players debug", NamedTextColor.DARK_AQUA));
        if (players.isEmpty()) {
            viewer.sendMessage(Component.text("No players online.", NamedTextColor.GRAY));
            return;
        }

        Set<UUID> markedThisRound = roundManager.getMarkedPlayersThisRound().stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toSet());

        for (Player target : players) {
            Role role = roleManager.getRole(target);
            NamedTextColor roleColor = role == Role.MANIAC ? NamedTextColor.RED : NamedTextColor.GREEN;
            Component status = (target.getGameMode() == GameMode.SPECTATOR || target.isDead())
                    ? Component.text("DEAD", NamedTextColor.DARK_GRAY)
                    : Component.text("ALIVE", NamedTextColor.GREEN);
            int normalMarks = markManager.getNormalMarks(target);
            int empoweredMarks = markManager.getEmpoweredMarks(target);
            boolean marked = markedThisRound.contains(target.getUniqueId());

            Component line = Component.text(target.getName(), roleColor)
                    .append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text(role.name(), roleColor))
                    .append(Component.text(") ", NamedTextColor.GRAY))
                    .append(status)
                    .append(Component.text(" | marks: " + normalMarks + "/" + empoweredMarks, NamedTextColor.GOLD));

            if (marked) {
                line = line.append(Component.text(" [MARKED]", NamedTextColor.YELLOW));
            }

            viewer.sendMessage(line);
        }
    }

    private Component button(String text, NamedTextColor color, Consumer<Player> action) {
        String actionId = registerAction(action);
        return Component.text("[" + text + "]", color).clickEvent(ClickEvent.runCommand("/maniacdebug " + actionId));
    }

    private String registerAction(Consumer<Player> action) {
        String id = UUID.randomUUID().toString();
        DEBUG_ACTIONS.put(id, action);
        return id;
    }

    private Component listNames(List<Player> players, NamedTextColor color) {
        if (players.isEmpty()) {
            return Component.text("None", NamedTextColor.GRAY);
        }
        return Component.join(JoinConfiguration.commas(true), players.stream()
                .map(player -> Component.text(player.getName(), color))
                .toList());
    }

    private Component joinLines(List<Component> lines) {
        return Component.join(JoinConfiguration.newlines(), lines);
    }
}

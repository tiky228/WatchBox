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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Generates a debug control book with live state and command shortcuts.
 */
public class DebugBookFactory {
    private static final int LINES_PER_PAGE = 11;
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

        meta.pages(buildPages());
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
        BookPageBuilder builder = new BookPageBuilder(LINES_PER_PAGE);
        builder.addHeading("Players & Roles", NamedTextColor.DARK_AQUA);

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            builder.addLine(Component.text("No players online.", NamedTextColor.GRAY));
        } else {
            for (Player player : players) {
                builder.addLine(buildPlayerLine(player));
            }
        }

        builder.addBlankLine();
        builder.addHeading("Match Info", NamedTextColor.BLUE);
        builder.addLine(button("Show alive counts", NamedTextColor.AQUA, this::sendMatchInfo));

        builder.addBlankLine();
        builder.addHeading("Round Control", NamedTextColor.BLUE);
        builder.addLine(Component.text()
                .append(button("Start Round", NamedTextColor.GREEN, p -> roundManager.startRound()))
                .append(Component.space())
                .append(button("Next Phase", NamedTextColor.AQUA, p -> roundManager.advancePhase()))
                .append(Component.space())
                .append(button("End Round", NamedTextColor.RED, p -> roundManager.endRound()))
                .build());

        builder.addBlankLine();
        builder.addHeading("Marks Tools", NamedTextColor.GREEN);
        builder.addLine(button("List marked players", NamedTextColor.AQUA, this::listMarkedPlayers));
        builder.addLine(button("Clear all marks", NamedTextColor.RED, viewer -> {
            markManager.clearAll();
            viewer.sendMessage(Component.text("All marks cleared.", NamedTextColor.RED));
        }));

        return builder.build();
    }

    private Component buildPlayerLine(Player player) {
        NamedTextColor nameColor = NamedTextColor.AQUA;
        Role role = roleManager.getRole(player);
        if (role == Role.MANIAC) {
            nameColor = NamedTextColor.RED;
        } else if (role == Role.CIVILIAN) {
            nameColor = NamedTextColor.GRAY;
        }

        UUID targetId = player.getUniqueId();
        TextComponent.Builder line = Component.text();
        line.append(Component.text(player.getName(), nameColor)).append(Component.space());

        line.append(button("Maniac", NamedTextColor.RED, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                roleManager.assignRole(target, Role.MANIAC);
                viewer.sendMessage(Component.text("Assigned MANIAC to " + target.getName(), NamedTextColor.GREEN));
            }
        })).append(Component.space());

        line.append(button("Civilian", NamedTextColor.GREEN, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                roleManager.assignRole(target, Role.CIVILIAN);
                viewer.sendMessage(Component.text("Assigned CIVILIAN to " + target.getName(), NamedTextColor.GREEN));
            }
        })).append(Component.space());

        line.append(button("Clear Role", NamedTextColor.DARK_GRAY, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                roleManager.clearRole(target);
                viewer.sendMessage(Component.text("Cleared role for " + target.getName(), NamedTextColor.GRAY));
            }
        })).append(Component.space());

        line.append(button("Add Mark", NamedTextColor.GREEN, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                markManager.addNormalMark(target, 1);
                viewer.sendMessage(Component.text("Added mark to " + target.getName(), NamedTextColor.GREEN));
            }
        })).append(Component.space());

        line.append(button("Clear Marks", NamedTextColor.DARK_GRAY, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                markManager.clearAllMarks(target);
                viewer.sendMessage(Component.text("Cleared marks for " + target.getName(), NamedTextColor.GRAY));
            }
        })).append(Component.space());

        line.append(button("Kill", NamedTextColor.RED, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                target.setHealth(0.0);
                target.setGameMode(GameMode.SPECTATOR);
                roundManager.removeDisguise(target);
                roundManager.onPlayerDeathOrLeave();
                viewer.sendMessage(Component.text("Killed " + target.getName(), NamedTextColor.RED));
            }
        }));

        return line.build();
    }

    private void sendMatchInfo(Player player) {
        Component info = Component.text()
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
        player.sendMessage(info);
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

    private void listMarkedPlayers(Player viewer) {
        Map<UUID, MarkManager.MarkCounts> marks = markManager.getAllMarks();
        if (marks.isEmpty()) {
            viewer.sendMessage(Component.text("No players are marked.", NamedTextColor.GRAY));
            return;
        }

        viewer.sendMessage(Component.text("Marked players:", NamedTextColor.AQUA));
        for (Map.Entry<UUID, MarkManager.MarkCounts> entry : new ArrayList<>(marks.entrySet())) {
            Player target = Bukkit.getPlayer(entry.getKey());
            if (target == null) {
                continue;
            }
            MarkManager.MarkCounts counts = entry.getValue();
            viewer.sendMessage(Component.text(
                    target.getName() + " - normal: " + counts.normal + ", empowered: " + counts.empowered,
                    NamedTextColor.GRAY));
        }
    }
}

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Generates a debug control book with live state and command shortcuts.
 */
public class DebugBookFactory {
    private static final int LINES_PER_PAGE = 12;

    private final NamespacedKey debugKey;
    private final RoundManager roundManager;
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final Map<String, Consumer<Player>> actions = new HashMap<>();

    public DebugBookFactory(JavaPlugin plugin, RoundManager roundManager, RoleManager roleManager, MarkManager markManager) {
        this.debugKey = new NamespacedKey(plugin, "debug_book");
        this.roundManager = roundManager;
        this.roleManager = roleManager;
        this.markManager = markManager;
    }

    public ItemStack createDebugBook() {
        actions.clear();

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text("Debug Controls", NamedTextColor.AQUA));
        meta.author(Component.text("WatchBox"));

        List<Component> playerPages = buildPlayerPages();
        List<Component> pages = new ArrayList<>();
        pages.add(buildControlPage(playerPages.size()));
        pages.addAll(playerPages);

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

    public boolean executeAction(String actionId, Player player) {
        Consumer<Player> action = actions.get(actionId);
        if (action == null) {
            player.sendMessage(Component.text("That debug action is no longer available.", NamedTextColor.RED));
            return false;
        }
        action.accept(player);
        return true;
    }

    private Component buildControlPage(int playerPageCount) {
        BookPageBuilder builder = new BookPageBuilder(LINES_PER_PAGE);
        builder.addHeading("Match Info & Round Control", NamedTextColor.GOLD);

        builder.addLine(Component.text().append(button("Match Info", NamedTextColor.AQUA, this::sendMatchInfo)).build());
        builder.addBlankLine();

        TextComponent startEndLine = Component.text()
                .append(button("Start Round", NamedTextColor.GREEN, p -> roundManager.startRound()))
                .append(Component.space())
                .append(button("End Round", NamedTextColor.RED, p -> roundManager.endRound()))
                .append(Component.space())
                .append(button("Next Phase", NamedTextColor.AQUA, p -> roundManager.advancePhase()))
                .build();
        builder.addLine(startEndLine);

        builder.addLine(Component.text("Force Phase:", NamedTextColor.GRAY));
        builder.addLine(forceButtons(RoundPhase.PRE_ROUND, RoundPhase.ROUND_START, RoundPhase.ACTION));
        builder.addLine(forceButtons(RoundPhase.DISCUSSION, RoundPhase.VOTING, RoundPhase.ROUND_END));

        if (playerPageCount > 0) {
            builder.addBlankLine();
            builder.addLine(Component.text("Player controls continue on next page →", NamedTextColor.DARK_AQUA));
        }

        return builder.build().get(0);
    }

    private Component forceButtons(RoundPhase... phases) {
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < phases.length; i++) {
            RoundPhase phase = phases[i];
            builder.append(button("Force " + phase.name(), NamedTextColor.YELLOW,
                    p -> roundManager.forcePhase(phase, roundManager.getRemainingSeconds())));
            if (i < phases.length - 1) {
                builder.append(Component.space());
            }
        }
        return builder.build();
    }

    private List<Component> buildPlayerPages() {
        BookPageBuilder builder = new BookPageBuilder(LINES_PER_PAGE);
        builder.addHeading("Player Controls", NamedTextColor.GREEN);

        for (Player player : Bukkit.getOnlinePlayers()) {
            builder.addLine(buildPlayerLine(player));
        }

        List<Component> pages = builder.build();
        if (pages.isEmpty()) {
            pages.add(Component.text("No players online.", NamedTextColor.GRAY));
        }
        return pages;
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

        line.append(button("Give Maniac", NamedTextColor.RED, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                roleManager.assignRole(target, Role.MANIAC);
                viewer.sendMessage(Component.text("Assigned MANIAC to " + target.getName(), NamedTextColor.GREEN));
            }
        })).append(Component.space());

        line.append(button("Give Civilian", NamedTextColor.GRAY, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                roleManager.assignRole(target, Role.CIVILIAN);
                viewer.sendMessage(Component.text("Assigned CIVILIAN to " + target.getName(), NamedTextColor.GREEN));
            }
        })).append(Component.space());

        line.append(button("Clear Role", NamedTextColor.GOLD, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                roleManager.clearRole(target);
                viewer.sendMessage(Component.text("Cleared role for " + target.getName(), NamedTextColor.YELLOW));
            }
        })).append(Component.space());

        line.append(button("Add Mark", NamedTextColor.GREEN, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                markManager.addNormalMark(target, 1);
                viewer.sendMessage(Component.text("Added mark to " + target.getName(), NamedTextColor.GREEN));
            }
        })).append(Component.space());

        line.append(button("Clear Marks", NamedTextColor.DARK_RED, viewer -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                markManager.clearAllMarks(target);
                viewer.sendMessage(Component.text("Cleared marks for " + target.getName(), NamedTextColor.YELLOW));
            }
        })).append(Component.space());

        line.append(button("Kill", NamedTextColor.DARK_GRAY, viewer -> {
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
                .append(Component.text("Phase: ", NamedTextColor.YELLOW))
                .append(Component.text(roundManager.getCurrentPhase().name(), NamedTextColor.AQUA))
                .append(Component.text(" | Remaining: ", NamedTextColor.YELLOW))
                .append(Component.text(roundManager.getRemainingSeconds() + "s", NamedTextColor.AQUA))
                .append(Component.text(" | Maniacs: ", NamedTextColor.YELLOW))
                .append(Component.text(String.valueOf(roundManager.countAliveManiacs()), NamedTextColor.RED))
                .append(Component.text(" | Civilians: ", NamedTextColor.YELLOW))
                .append(Component.text(String.valueOf(roundManager.countAliveCivilians()), NamedTextColor.GRAY))
                .append(Component.text(" | Spectators: ", NamedTextColor.YELLOW))
                .append(Component.text(String.valueOf(roundManager.countSpectators()), NamedTextColor.WHITE))
                .build();
        player.sendMessage(info);
    }

    private Component button(String text, NamedTextColor color, Consumer<Player> action) {
        String actionId = registerAction(action);
        return Component.text("[" + text + "]", color).clickEvent(ClickEvent.runCommand("/maniacdebug action " + actionId));
    }

    private String registerAction(Consumer<Player> action) {
        String id = UUID.randomUUID().toString();
        actions.put(id, action);
        return id;
    }
}

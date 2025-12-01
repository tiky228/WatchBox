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
        meta.title(Component.text("Debug Controls", NamedTextColor.GOLD));
        meta.author(Component.text("WatchBox"));

        List<Component> pages = new ArrayList<>();
        pages.add(buildStatePage());
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

    private Component buildStatePage() {
        Component.Builder builder = Component.text();
        builder.append(Component.text("Game State", NamedTextColor.GOLD)).append(Component.newline());
        builder.append(Component.text("Phase: " + roundManager.getCurrentPhase(), NamedTextColor.YELLOW)).append(Component.newline());
        builder.append(Component.text("Time Left: " + roundManager.getRemainingSeconds() + "s", NamedTextColor.YELLOW)).append(Component.newline());
        builder.append(Component.newline());

        builder.append(Component.text("Alive Civilians:", NamedTextColor.GREEN)).append(Component.newline());
        for (String civilian : getPlayersByRole(Role.CIVILIAN)) {
            builder.append(Component.text("- " + civilian, NamedTextColor.WHITE)).append(Component.newline());
        }
        builder.append(Component.newline());

        builder.append(Component.text("Alive Maniacs:", NamedTextColor.RED)).append(Component.newline());
        for (String maniac : getPlayersByRole(Role.MANIAC)) {
            builder.append(Component.text("- " + maniac, NamedTextColor.WHITE)).append(Component.newline());
        }
        builder.append(Component.newline());

        builder.append(Component.text("Spectators / Dead:", NamedTextColor.GRAY)).append(Component.newline());
        for (String dead : getSpectators()) {
            builder.append(Component.text("- " + dead, NamedTextColor.WHITE)).append(Component.newline());
        }
        return builder.build();
    }

    private Component buildRoundControlPage() {
        Component.Builder builder = Component.text();
        builder.append(Component.text("Round Controls", NamedTextColor.GOLD)).append(Component.newline());
        builder.append(clickable("Start", "/maniacdebug start", NamedTextColor.GREEN)).append(Component.newline());
        builder.append(clickable("Stop", "/maniacdebug stop", NamedTextColor.RED)).append(Component.newline());
        builder.append(clickable("Next Phase", "/maniacdebug nextphase", NamedTextColor.AQUA)).append(Component.newline());
        return builder.build();
    }

    private Component buildPhaseControlPage() {
        Component.Builder builder = Component.text();
        builder.append(Component.text("Phase Overrides", NamedTextColor.GOLD)).append(Component.newline());
        builder.append(clickable("ACTION", "/maniacdebug phase action", NamedTextColor.YELLOW)).append(Component.newline());
        builder.append(clickable("DISCUSSION", "/maniacdebug phase discussion", NamedTextColor.YELLOW)).append(Component.newline());
        builder.append(clickable("VOTING", "/maniacdebug phase voting", NamedTextColor.YELLOW)).append(Component.newline());
        builder.append(Component.newline());
        builder.append(clickable("Give Voting Book", "/maniac votebook", NamedTextColor.AQUA));
        return builder.build();
    }

    private Component buildMarkPage() {
        Component.Builder builder = Component.text();
        builder.append(Component.text("Marks & Debug", NamedTextColor.GOLD)).append(Component.newline());
        builder.append(clickable("List Marks", "/maniacdebug listmarks", NamedTextColor.AQUA)).append(Component.newline());
        builder.append(Component.text("Add mark to player:", NamedTextColor.GRAY)).append(Component.newline());
        for (Player player : Bukkit.getOnlinePlayers()) {
            builder.append(clickable(player.getName(), "/maniacdebug mark " + player.getName(), NamedTextColor.WHITE)).append(Component.newline());
        }
        builder.append(Component.newline());
        builder.append(Component.text("Clear / Empowered:", NamedTextColor.GRAY)).append(Component.newline());
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
}

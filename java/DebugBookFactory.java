import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the debug control book content.
 */
public class DebugBookFactory {
    private final JavaPlugin plugin;
    private final RoleManager roleManager;
    private final RoundManager roundManager;
    private final MarkManager markManager;
    private final NamespacedKey key;

    public DebugBookFactory(JavaPlugin plugin, RoleManager roleManager, RoundManager roundManager, MarkManager markManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.roundManager = roundManager;
        this.markManager = markManager;
        this.key = new NamespacedKey(plugin, "debug_book");
    }

    public ItemStack createDebugBook() {
        ItemStack stack = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) stack.getItemMeta();
        meta.title(Component.text("Book of Debug Control", NamedTextColor.GOLD));
        meta.author(Component.text("Watchbox"));
        meta.addPages(buildStatePage(), buildControlPage(), buildMarksPage());
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isDebugBook(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private Component buildStatePage() {
        Component.Builder builder = Component.text().color(NamedTextColor.DARK_AQUA);
        builder.append(Component.text("Game State", NamedTextColor.GOLD)).append(Component.newline());
        builder.append(Component.text("Phase: " + roundManager.getCurrentPhase(), NamedTextColor.YELLOW)).append(Component.newline());
        builder.append(Component.text("Remaining: " + roundManager.getRemainingSeconds() + "s", NamedTextColor.YELLOW)).append(Component.newline());
        builder.append(Component.newline());
        builder.append(Component.text("Civilians:", NamedTextColor.GREEN)).append(Component.newline());
        for (String name : getPlayersByRole(Role.CIVILIAN)) {
            builder.append(Component.text("- " + name, NamedTextColor.WHITE)).append(Component.newline());
        }
        builder.append(Component.text("Maniacs:", NamedTextColor.RED)).append(Component.newline());
        for (String name : getPlayersByRole(Role.MANIAC)) {
            builder.append(Component.text("- " + name, NamedTextColor.WHITE)).append(Component.newline());
        }
        builder.append(Component.text("Dead/Spectators:", NamedTextColor.GRAY)).append(Component.newline());
        for (String name : getSpectators()) {
            builder.append(Component.text("- " + name, NamedTextColor.WHITE)).append(Component.newline());
        }
        return builder.build();
    }

    private List<String> getPlayersByRole(Role role) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .filter(p -> roleManager.getRole(p) == role)
                .map(player -> player.getName())
                .collect(Collectors.toList());
    }

    private List<String> getSpectators() {
        List<String> list = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                list.add(player.getName());
            }
        });
        return list;
    }

    private Component buildControlPage() {
        Component.Builder builder = Component.text();
        builder.append(Component.text("Round Controls", NamedTextColor.GOLD)).append(Component.newline());
        builder.append(clickable("Start round", "/maniacdebug start", NamedTextColor.GREEN)).append(Component.newline());
        builder.append(clickable("Stop round", "/maniacdebug stop", NamedTextColor.RED)).append(Component.newline());
        builder.append(clickable("Next phase", "/maniacdebug nextphase", NamedTextColor.AQUA)).append(Component.newline());
        builder.append(clickable("Set ACTION", "/maniacdebug phase action", NamedTextColor.YELLOW)).append(Component.newline());
        builder.append(clickable("Set DISCUSSION", "/maniacdebug phase discussion", NamedTextColor.YELLOW)).append(Component.newline());
        builder.append(clickable("Set VOTING", "/maniacdebug phase voting", NamedTextColor.YELLOW)).append(Component.newline());
        return builder.build();
    }

    private Component buildMarksPage() {
        Component.Builder builder = Component.text();
        builder.append(Component.text("Marks / Roles", NamedTextColor.GOLD)).append(Component.newline());
        builder.append(Component.text("Click to view marked players.", NamedTextColor.YELLOW)).append(Component.newline());
        builder.append(clickable("List marked", "/maniacdebug listmarks", NamedTextColor.AQUA)).append(Component.newline());
        builder.append(Component.newline());
        builder.append(Component.text("Add mark to online player:", NamedTextColor.GRAY)).append(Component.newline());
        Bukkit.getOnlinePlayers().forEach(player -> {
            builder.append(clickable(player.getName(), "/maniacdebug mark " + player.getName(), NamedTextColor.WHITE)).append(Component.newline());
        });
        return builder.build();
    }

    private Component clickable(String label, String command, NamedTextColor color) {
        return Component.text(label, color).clickEvent(ClickEvent.runCommand(command));
    }
}

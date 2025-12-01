import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Handles the Killer Sign interaction flow.
 */
public class KillerSignListener implements Listener {
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final NamespacedKey killerSignKey;

    public KillerSignListener(JavaPlugin plugin, RoleManager roleManager, MarkManager markManager, KillerSignItem killerSignItem) {
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.killerSignKey = new NamespacedKey(plugin, "killer_sign");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isKillerSign(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!(player.isOp() || roleManager.isManiac(player))) {
            player.sendMessage(Component.text("Only the Maniac may use this sign.", NamedTextColor.RED));
            return;
        }

        sendAbilityMenu(player);
    }

    private boolean isKillerSign(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(killerSignKey, PersistentDataType.BYTE);
    }

    private void sendAbilityMenu(Player player) {
        player.sendMessage(Component.text(" ").append(Component.text("-= Killer Sign =-", NamedTextColor.DARK_RED)));
        player.sendMessage(Component.text("[Silence Player]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/maniac silence")));
        player.sendMessage(Component.text("[Show Marked Players]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand("/maniac showmarks")));
        player.sendMessage(Component.text("[Swap Places]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/maniac swap")));
    }

    public void sendSilenceTargets(Player player) {
        player.sendMessage(Component.text("Select a target to silence:", NamedTextColor.YELLOW));
        for (Player target : getAlivePlayers(player.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " ", NamedTextColor.WHITE)
                    .append(Component.text("[SILENCE]", NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/maniac silence " + target.getName()))));
        }
    }

    public void sendSwapTargets(Player player) {
        player.sendMessage(Component.text("Swap with:", NamedTextColor.YELLOW));
        for (Player target : getAlivePlayers(player.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " ", NamedTextColor.WHITE)
                    .append(Component.text("[SWAP]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/maniac swap " + target.getName()))));
        }
    }

    public void sendMarked(Player player) {
        if (markManager.getAllMarks().isEmpty()) {
            player.sendMessage(Component.text("No marked players.", NamedTextColor.GRAY));
            return;
        }
        List<UUID> sorted = new ArrayList<>(markManager.getAllMarks().keySet());
        sorted.sort(Comparator.comparing(uuid -> {
            Player online = Bukkit.getPlayer(uuid);
            return online != null ? online.getName() : uuid.toString();
        }));

        player.sendMessage(Component.text("Marked players:", NamedTextColor.GOLD));
        for (UUID uuid : sorted) {
            MarkManager.MarkCounts counts = markManager.getAllMarks().get(uuid);
            Player online = Bukkit.getPlayer(uuid);
            String name = online != null ? online.getName() : uuid.toString();
            int total = counts.normal + counts.empowered;
            player.sendMessage(Component.text(name + " - " + total + " mark" + (total == 1 ? "" : "s"), NamedTextColor.YELLOW));
        }
    }

    private List<Player> getAlivePlayers(UUID exclude) {
        List<Player> alive = new ArrayList<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(exclude)) {
                continue;
            }
            if (target.getGameMode() == GameMode.SPECTATOR || target.isDead()) {
                continue;
            }
            alive.add(target);
        }
        return alive;
    }
}

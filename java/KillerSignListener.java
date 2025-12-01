import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles interaction with the Killer Sign item.
 */
public class KillerSignListener implements Listener {
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final KillerSignItem killerSignItem;

    public KillerSignListener(JavaPlugin plugin, RoleManager roleManager, MarkManager markManager, KillerSignItem killerSignItem) {
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.killerSignItem = killerSignItem;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        ItemStack item = event.getItem();
        if (!killerSignItem.isKillerSign(item)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!(player.isOp() || roleManager.isManiac(player))) {
            player.sendMessage(Component.text("You cannot wield this power.", NamedTextColor.RED));
            return;
        }
        sendAbilityMenu(player);
    }

    private void sendAbilityMenu(Player player) {
        player.sendMessage(Component.text("Killer Abilities", NamedTextColor.DARK_RED));
        player.sendMessage(Component.text("[Silence player]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/maniac silence")));
        player.sendMessage(Component.text("[Show marked players]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand("/maniac showmarks")));
        player.sendMessage(Component.text("[Swap places with player]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/maniac swap")));
    }

    public void sendSilenceTargets(Player player) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getGameMode() == GameMode.SPECTATOR || target.equals(player)) {
                continue;
            }
            player.sendMessage(Component.text(target.getName() + " - ", NamedTextColor.WHITE)
                    .append(Component.text("[SILENCE]", NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/maniac silence " + target.getName()))));
        }
    }

    public void sendSwapTargets(Player player) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getGameMode() == GameMode.SPECTATOR || target.equals(player)) {
                continue;
            }
            player.sendMessage(Component.text(target.getName() + " - ", NamedTextColor.WHITE)
                    .append(Component.text("[SWAP]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/maniac swap " + target.getName()))));
        }
    }

    public void sendMarked(Player player) {
        if (markManager.getAllMarks().isEmpty()) {
            player.sendMessage(Component.text("No marked players.", NamedTextColor.GRAY));
            return;
        }
        markManager.getAllMarks().forEach((uuid, counts) -> {
            Player online = Bukkit.getPlayer(uuid);
            String name = online != null ? online.getName() : uuid.toString();
            player.sendMessage(Component.text(name + " - Normal:" + counts.normal + " Empowered:" + counts.empowered, NamedTextColor.YELLOW));
        });
    }
}

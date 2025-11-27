import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /dbghighlight role <role> and /dbghighlight marked for quick glowing debug highlights.
 */
public class DebugHighlightCommand implements CommandExecutor {
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final JavaPlugin plugin;

    public DebugHighlightCommand(RoleManager roleManager, MarkManager markManager, JavaPlugin plugin) {
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /" + label + " role <murderer|innocent> OR /" + label + " marked", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("role") && args.length >= 2) {
            Role role = parseRole(args[1]);
            if (role == null) {
                sender.sendMessage(Component.text("Unknown role. Use murderer or innocent.", NamedTextColor.RED));
                return true;
            }
            List<Player> players = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> roleManager.getRole(p.getUniqueId()) == role)
                    .collect(Collectors.toList());
            applyGlowing(players, 20 * 5);
            sender.sendMessage(Component.text("Highlighted " + players.size() + " player(s) with role " + role + ".", NamedTextColor.AQUA));
            return true;
        }

        if (args[0].equalsIgnoreCase("marked")) {
            List<LivingEntity> entities = markManager.getMarkedEntities();
            applyGlowing(entities, 20 * 5);
            sender.sendMessage(Component.text("Highlighted " + entities.size() + " marked entity(ies).", NamedTextColor.AQUA));
            return true;
        }

        sender.sendMessage(Component.text("Unknown usage.", NamedTextColor.RED));
        return true;
    }

    private Role parseRole(String input) {
        if (input.equalsIgnoreCase("murderer")) return Role.MURDERER;
        if (input.equalsIgnoreCase("innocent")) return Role.INNOCENT;
        return null;
    }

    private void applyGlowing(Collection<? extends LivingEntity> entities, int durationTicks) {
        // Apply glowing and schedule removal to highlight for a short duration.
        entities.forEach(entity -> entity.setGlowing(true));
        new BukkitRunnable() {
            @Override
            public void run() {
                entities.forEach(entity -> entity.setGlowing(false));
            }
        }.runTaskLater(plugin, durationTicks);
    }
}

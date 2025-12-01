import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to set/get roles.
 */
public class RoleCommand implements CommandExecutor {
    private final RoleManager roleManager;

    public RoleCommand(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /" + label + " set <player> <MANIAC|CIVILIAN> | /" + label + " get <player>", NamedTextColor.YELLOW));
            return true;
        }
        if (args[0].equalsIgnoreCase("set") && args.length >= 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }
            Role role = parseRole(args[2]);
            if (role == null) {
                sender.sendMessage(Component.text("Unknown role.", NamedTextColor.RED));
                return true;
            }
            roleManager.assignRole(target, role);
            sender.sendMessage(Component.text("Assigned " + role + " to " + target.getName() + ".", NamedTextColor.GREEN));
            return true;
        }
        if (args[0].equalsIgnoreCase("get") && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }
            sender.sendMessage(Component.text(target.getName() + " is " + roleManager.getRole(target) + ".", NamedTextColor.AQUA));
            return true;
        }
        sender.sendMessage(Component.text("Unknown usage.", NamedTextColor.RED));
        return true;
    }

    private Role parseRole(String raw) {
        if (raw.equalsIgnoreCase("maniac")) return Role.MANIAC;
        if (raw.equalsIgnoreCase("civilian")) return Role.CIVILIAN;
        return null;
    }
}

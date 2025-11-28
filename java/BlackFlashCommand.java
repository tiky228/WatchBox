import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Debug command to give the Black Flash axe for testing.
 */
public class BlackFlashCommand implements CommandExecutor {
    private final BlackFlashManager manager;

    public BlackFlashCommand(BlackFlashManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            Player target;
            if (args.length > 1) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }
            } else {
                if (!(sender instanceof Player playerSender)) {
                    sender.sendMessage(ChatColor.RED + "Only players can receive the Black Flash axe directly.");
                    return true;
                }
                target = playerSender;
            }

            target.getInventory().addItem(manager.createBlackFlashAxe());
            sender.sendMessage(ChatColor.DARK_RED + "Gave Black Flash axe to " + target.getName());
            if (sender != target) {
                target.sendMessage(ChatColor.GRAY + "You received the " + ChatColor.DARK_RED + "Black Flash" + ChatColor.GRAY + " axe.");
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " give [player]");
        sender.sendMessage(ChatColor.GRAY + "Debug command to test the Black Flash ability.");
        return true;
    }
}

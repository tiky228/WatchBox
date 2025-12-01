package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /dbgrole set <player> <role> and /dbgrole get [player].
 */
public class DebugRoleCommand implements CommandExecutor {
    private final RoleManager roleManager;

    public DebugRoleCommand(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /" + label + " set <player> <murderer|innocent> OR /" + label + " get [player]", NamedTextColor.YELLOW));
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
                sender.sendMessage(Component.text("Unknown role. Use murderer or innocent.", NamedTextColor.RED));
                return true;
            }
            roleManager.setRole(target.getUniqueId(), role);
            sender.sendMessage(Component.text("Set role of " + target.getName() + " to " + role + ".", NamedTextColor.GREEN));
            return true;
        }

        if (args[0].equalsIgnoreCase("get")) {
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("Console must specify a player.", NamedTextColor.RED));
                    return true;
                }
                target = (Player) sender;
            }
            Role role = roleManager.getRole(target.getUniqueId());
            sender.sendMessage(Component.text(target.getName() + " is " + role + ".", NamedTextColor.AQUA));
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
}

package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

/**
 * /dbgweapon give [player] gives the murderer weapon and sets the role if needed.
 */
public class DebugWeaponCommand implements CommandExecutor {
    private final RoleManager roleManager;
    private final MurdererWeapon weapon;

    public DebugWeaponCommand(RoleManager roleManager, MurdererWeapon weapon) {
        this.roleManager = roleManager;
        this.weapon = weapon;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Console must provide a player.", NamedTextColor.RED));
                return true;
            }
            target = (Player) sender;
        }

        // Ensure the player is a murderer so they can use the item.
        if (roleManager.getRole(target.getUniqueId()) != Role.MURDERER) {
            roleManager.setRole(target.getUniqueId(), Role.MURDERER);
            target.sendMessage(Component.text("You were set to MURDERER to use the weapon.", NamedTextColor.GOLD));
        }

        PlayerInventory inv = target.getInventory();
        inv.addItem(weapon.getWeapon());
        target.sendMessage(Component.text("Murderer weapon added to your inventory.", NamedTextColor.GREEN));
        if (!target.equals(sender)) {
            sender.sendMessage(Component.text("Gave the murderer weapon to " + target.getName() + ".", NamedTextColor.AQUA));
        }
        return true;
    }
}

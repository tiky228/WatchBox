package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides /debugbook command for admins.
 */
public class DebugBookCommand implements CommandExecutor {
    private final DebugBookFactory debugBookFactory;

    public DebugBookCommand(DebugBookFactory debugBookFactory) {
        this.debugBookFactory = debugBookFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("maniac.debug")) {
            sender.sendMessage(Component.text("You lack permission.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;
        player.getInventory().addItem(debugBookFactory.createDebugBook());
        player.sendMessage(Component.text("Debug book updated.", NamedTextColor.GREEN));
        return true;
    }
}

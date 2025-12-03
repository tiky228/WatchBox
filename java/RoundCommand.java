package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RoundCommand implements CommandExecutor {
    private final RoundManager roundManager;

    public RoundCommand(RoundManager roundManager) {
        this.roundManager = roundManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(Component.text(
                    "Phase: " + roundManager.getCurrentPhase() + " | Remaining: " + roundManager.getRemainingSeconds() + "s",
                    NamedTextColor.AQUA));
            return true;
        }

        if (args[0].equalsIgnoreCase("time") && args.length >= 2) {
            try {
                int seconds = Integer.parseInt(args[1]);
                roundManager.setRemainingSeconds(seconds);
                sender.sendMessage(Component.text("Remaining time set to " + seconds + "s.", NamedTextColor.GREEN));
            } catch (NumberFormatException ex) {
                sender.sendMessage(Component.text("Invalid number.", NamedTextColor.RED));
            }
            return true;
        }

        sender.sendMessage(Component.text("Usage: /" + label + " info|time <seconds>", NamedTextColor.YELLOW));
        return true;
    }
}

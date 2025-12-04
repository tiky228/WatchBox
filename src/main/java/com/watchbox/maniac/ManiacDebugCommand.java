package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class ManiacDebugCommand implements CommandExecutor {
    private final RoundManager roundManager;
    private final DebugBookFactory debugBookFactory;

    public ManiacDebugCommand(RoundManager roundManager, DebugBookFactory debugBookFactory) {
        this.roundManager = roundManager;
        this.debugBookFactory = debugBookFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        if (tryExecuteDebugAction(sender, args[0])) {
            return true;
        }

        if (sender instanceof Player player && !player.isOp()) {
            sender.sendMessage(Component.text("Operator permission required.", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                roundManager.startRound();
                sender.sendMessage(Component.text("Round started.", NamedTextColor.GREEN));
            }
            case "stop" -> {
                roundManager.endRound();
                sender.sendMessage(Component.text("Round ended.", NamedTextColor.RED));
            }
            case "nextphase" -> {
                roundManager.advancePhase();
                sender.sendMessage(Component.text("Advanced to next phase.", NamedTextColor.GREEN));
            }
            case "manip", "manipulation" -> handleManipulation(sender);
            case "phase" -> handlePhaseCommand(sender, label, args);
            case "time" -> handleTimeCommand(sender, args);
            default -> sender.sendMessage(Component.text("That debug action is no longer available.", NamedTextColor.RED));
        }
        return true;
    }

    private void handlePhaseCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(Component.text(
                    "Current phase: " + roundManager.getCurrentPhase() + " | Remaining: " + roundManager.getRemainingSeconds() + "s",
                    NamedTextColor.AQUA));
            return;
        }

        RoundPhase phase;
        try {
            phase = RoundPhase.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Unknown phase.", NamedTextColor.RED));
            return;
        }

        int seconds = roundManager.getRemainingSeconds();
        if (args.length >= 3) {
            try {
                seconds = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(Component.text("Invalid seconds value.", NamedTextColor.RED));
                return;
            }
        }

        roundManager.forcePhase(phase, seconds);
        sender.sendMessage(Component.text("Forced phase to " + phase + " for " + seconds + "s.", NamedTextColor.GREEN));
    }

    private void handleTimeCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /maniacdebug time <seconds>", NamedTextColor.DARK_AQUA));
            return;
        }
        try {
            int seconds = Integer.parseInt(args[1]);
            roundManager.setRemainingSeconds(seconds);
            sender.sendMessage(Component.text("Remaining time set to " + seconds + "s.", NamedTextColor.GREEN));
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text("Invalid seconds value.", NamedTextColor.RED));
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("Usage: /" + label + " start|stop|nextphase|manip|phase [phase] [seconds]|time <seconds>", NamedTextColor.DARK_AQUA));
    }

    private boolean tryExecuteDebugAction(CommandSender sender, String id) {
        Consumer<Player> action = DebugBookFactory.DEBUG_ACTIONS.get(id);
        if (action == null && !isKnownSubCommand(id)) {
            sender.sendMessage(Component.text("That debug action is no longer available.", NamedTextColor.RED));
            return true;
        }
        if (action == null) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        action.accept(player);
        return true;
    }

    private boolean isKnownSubCommand(String id) {
        return switch (id.toLowerCase()) {
            case "start", "stop", "nextphase", "phase", "time", "manip", "manipulation" -> true;
            default -> false;
        };
    }

    private void handleManipulation(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return;
        }
        if (debugBookFactory == null) {
            sender.sendMessage(Component.text("Manipulation details are unavailable.", NamedTextColor.RED));
            return;
        }
        debugBookFactory.sendManipulationInfo(player);
    }
}

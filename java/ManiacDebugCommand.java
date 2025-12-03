package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ManiacDebugCommand implements CommandExecutor {
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final SilenceManager silenceManager;
    private final TaskManager taskManager;
    private final RoundManager roundManager;
    private final ManiacAbilityManager abilityManager;
    private final DebugBookFactory debugBookFactory;
    private final VoteManager voteManager;
    private final long silenceDuration;
    private final long normalCooldown;
    private final long empoweredCooldown;
    private final boolean empoweredEnabled;

    public ManiacDebugCommand(RoleManager roleManager, MarkManager markManager, SilenceManager silenceManager, TaskManager taskManager,
                              RoundManager roundManager, ManiacAbilityManager abilityManager, DebugBookFactory debugBookFactory,
                              VoteManager voteManager, long silenceDuration, long normalCooldown, long empoweredCooldown,
                              boolean empoweredEnabled) {
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.silenceManager = silenceManager;
        this.taskManager = taskManager;
        this.roundManager = roundManager;
        this.abilityManager = abilityManager;
        this.debugBookFactory = debugBookFactory;
        this.voteManager = voteManager;
        this.silenceDuration = silenceDuration;
        this.normalCooldown = normalCooldown;
        this.empoweredCooldown = empoweredCooldown;
        this.empoweredEnabled = empoweredEnabled;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.isOp() || !(sender instanceof org.bukkit.entity.Player))) {
            sender.sendMessage(Component.text("Operator permission required.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
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
            case "phase" -> handlePhaseCommand(sender, label, args);
            case "time" -> handleTimeCommand(sender, args);
            default -> sendUsage(sender, label);
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
            sender.sendMessage(Component.text("Usage: /maniacdebug time <seconds>", NamedTextColor.YELLOW));
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
        sender.sendMessage(Component.text("Usage: /" + label + " start|stop|nextphase|phase [phase] [seconds]|time <seconds>", NamedTextColor.YELLOW));
    }
}

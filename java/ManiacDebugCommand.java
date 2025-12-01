package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


/**
 * Debug command for testing the Maniac mode.
 */
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

    public ManiacDebugCommand(RoleManager roleManager, MarkManager markManager, SilenceManager silenceManager,
                              TaskManager taskManager, RoundManager roundManager, ManiacAbilityManager abilityManager,
                              DebugBookFactory debugBookFactory, VoteManager voteManager,
                              long silenceDuration, long normalCooldown, long empoweredCooldown, boolean empoweredEnabled) {
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
        if (!(sender.isOp() || sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage(Component.text("Operator permission required.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: start|stop|phase|nextphase|mark|emark|clearmarks|silence|lanterns|bells|listmarks|book", NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "start":
                roundManager.startRound();
                sender.sendMessage(Component.text("Round started.", NamedTextColor.GREEN));
                return true;
            case "stop":
                roundManager.stopRound();
                sender.sendMessage(Component.text("Round stopped.", NamedTextColor.RED));
                return true;
            case "nextphase":
                roundManager.advancePhase();
                sender.sendMessage(Component.text("Advanced to next phase.", NamedTextColor.GREEN));
                return true;
            case "phase":
                if (args.length >= 2) {
                    RoundPhase phase = parsePhase(args[1]);
                    if (phase == null) {
                        sender.sendMessage(Component.text("Unknown phase.", NamedTextColor.RED));
                        return true;
                    }
                    int time = args.length >= 3 ? Integer.parseInt(args[2]) : roundManager.getRemainingSeconds();
                    roundManager.forcePhase(phase, time);
                    sender.sendMessage(Component.text("Phase forced to " + phase + ".", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Usage: /" + label + " phase <lobby|action|discussion|voting|ended> [seconds]", NamedTextColor.YELLOW));
                }
                return true;
            case "mark":
                Player target = Bukkit.getPlayer(args.length >= 2 ? args[1] : "");
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                if (sender instanceof Player) {
                    Player playerSender = (Player) sender;
                    if (abilityManager.isOnCooldown(playerSender, ManiacAbilityManager.Ability.NORMAL_MARK, normalCooldown)) {
                        long remain = abilityManager.remainingMs(playerSender, ManiacAbilityManager.Ability.NORMAL_MARK) / 1000L;
                        sender.sendMessage(Component.text("Normal mark on cooldown for " + remain + "s.", NamedTextColor.RED));
                        return true;
                    }
                    abilityManager.triggerCooldown(playerSender, ManiacAbilityManager.Ability.NORMAL_MARK, normalCooldown);
                }
                markManager.addNormalMark(target, 1);
                sender.sendMessage(Component.text("Added normal mark to " + target.getName() + ".", NamedTextColor.GREEN));
                return true;
            case "emark":
                if (!empoweredEnabled) {
                    sender.sendMessage(Component.text("Empowered marks are disabled in config.", NamedTextColor.RED));
                    return true;
                }
                Player etarget = Bukkit.getPlayer(args.length >= 2 ? args[1] : "");
                if (etarget == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                if (sender instanceof Player) {
                    Player playerSender = (Player) sender;
                    if (abilityManager.isOnCooldown(playerSender, ManiacAbilityManager.Ability.EMPOWERED_MARK, empoweredCooldown)) {
                        long remain = abilityManager.remainingMs(playerSender, ManiacAbilityManager.Ability.EMPOWERED_MARK) / 1000L;
                        sender.sendMessage(Component.text("Empowered mark on cooldown for " + remain + "s.", NamedTextColor.RED));
                        return true;
                    }
                    abilityManager.triggerCooldown(playerSender, ManiacAbilityManager.Ability.EMPOWERED_MARK, empoweredCooldown);
                }
                markManager.addEmpoweredMark(etarget, 1);
                sender.sendMessage(Component.text("Added empowered mark to " + etarget.getName() + ".", NamedTextColor.GREEN));
                return true;
            case "clearmarks":
                Player ctarget = Bukkit.getPlayer(args.length >= 2 ? args[1] : "");
                if (ctarget == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                markManager.clearAllMarks(ctarget);
                sender.sendMessage(Component.text("Cleared marks for " + ctarget.getName() + ".", NamedTextColor.GREEN));
                return true;
            case "listmarks":
                if (markManager.getAllMarks().isEmpty()) {
                    sender.sendMessage(Component.text("No players are marked.", NamedTextColor.GRAY));
                    return true;
                }
                markManager.getAllMarks().forEach((uuid, counts) -> {
                    Player online = Bukkit.getPlayer(uuid);
                    String name = online != null ? online.getName() : uuid.toString();
                    sender.sendMessage(Component.text(name + " - Normal:" + counts.normal + " Empowered:" + counts.empowered, NamedTextColor.YELLOW));
                });
                return true;
            case "silence":
                Player starget = Bukkit.getPlayer(args.length >= 2 ? args[1] : "");
                if (starget == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                if (markManager.getTotalMarks(starget) <= 0) {
                    sender.sendMessage(Component.text("Target must have at least one mark to be silenced.", NamedTextColor.RED));
                    return true;
                }
                silenceManager.silence(starget, silenceDuration);
                sender.sendMessage(Component.text("Silenced " + starget.getName() + " for sign writing.", NamedTextColor.RED));
                return true;
            case "book":
                Player bookTarget;
                if (args.length >= 2) {
                    bookTarget = Bukkit.getPlayer(args[1]);
                    if (bookTarget == null) {
                        sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                        return true;
                    }
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Component.text("Specify a player.", NamedTextColor.RED));
                        return true;
                    }
                    bookTarget = (Player) sender;
                }
                bookTarget.getInventory().addItem(debugBookFactory.createDebugBook());
                sender.sendMessage(Component.text("Debug book created.", NamedTextColor.GREEN));
                return true;
            case "lanterns":
                if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
                    taskManager.resetLanterns();
                    sender.sendMessage(Component.text("Lanterns reset.", NamedTextColor.GREEN));
                }
                return true;
            case "bells":
                if (args.length >= 2 && args[1].equalsIgnoreCase("test")) {
                    if (sender instanceof Player) {
                        taskManager.handleBellInteract((Player) sender, ((Player) sender).getLocation());
                    }
                }
                return true;
            default:
                sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                return true;
        }
    }

    private RoundPhase parsePhase(String raw) {
        switch (raw.toLowerCase()) {
            case "lobby":
                return RoundPhase.LOBBY;
            case "action":
                return RoundPhase.ACTION;
            case "discussion":
                return RoundPhase.DISCUSSION;
            case "voting":
                return RoundPhase.VOTING;
            case "ended":
                return RoundPhase.ENDED;
            default:
                return null;
        }
    }
}

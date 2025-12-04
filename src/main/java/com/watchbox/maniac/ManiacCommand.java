package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Command entrypoint for Maniac utilities (voting books, killer sign, abilities).
 */
public class ManiacCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final RoleManager roleManager;
    private final SilenceManager silenceManager;
    private final ManiacAbilityManager abilityManager;
    private final VoteManager voteManager;
    private final KillerSignItem killerSignItem;
    private final KillerSignListener killerSignListener;
    private final long silenceDuration;
    private final RoundManager roundManager;
    private final DebugBookFactory debugBookFactory;

    public ManiacCommand(JavaPlugin plugin, RoleManager roleManager, SilenceManager silenceManager,
                         ManiacAbilityManager abilityManager, VoteManager voteManager, RoundManager roundManager,
                         KillerSignItem killerSignItem, KillerSignListener killerSignListener, long silenceDuration,
                         DebugBookFactory debugBookFactory) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.silenceManager = silenceManager;
        this.abilityManager = abilityManager;
        this.voteManager = voteManager;
        this.roundManager = roundManager;
        this.killerSignItem = killerSignItem;
        this.killerSignListener = killerSignListener;
        this.silenceDuration = silenceDuration;
        this.debugBookFactory = debugBookFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: votebook|killersign|silence|swap|entityid|debug", NamedTextColor.DARK_AQUA));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "votebook":
                return handleVoteBook(sender, args);
            case "killersign":
                return handleKillerSign(sender, args);
            case "silence":
                return handleSilence(sender, args);
            case "entityid":
                return handleEntityId(sender, args);
            case "swap":
                return handleSwap(sender, args);
            case "debug":
                return handleDebug(sender, args);
            default:
                sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                return true;
        }
    }

    private boolean handleVoteBook(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("Operator permission required.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /maniac votebook <player>", NamedTextColor.DARK_AQUA));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }
        voteManager.giveVotingBook(target);
        sender.sendMessage(Component.text("Voting book given to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleKillerSign(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("Operator permission required.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /maniac killersign <player>", NamedTextColor.DARK_AQUA));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }
        if (!roleManager.isManiac(target)) {
            sender.sendMessage(Component.text("Target must be the Maniac.", NamedTextColor.RED));
            return true;
        }
        target.getInventory().addItem(killerSignItem.createItem());
        sender.sendMessage(Component.text("Killer sign given to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSilence(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("In-game only.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;
        if (!(player.isOp() || roleManager.isManiac(player))) {
            player.sendMessage(Component.text("Only the Maniac may silence others.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            killerSignListener.sendSilenceTargets(player);
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || target.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(Component.text("Invalid target.", NamedTextColor.RED));
            return true;
        }
        if (abilityManager.isOnCooldown(player, ManiacAbilityManager.Ability.SILENCE, silenceDuration)) {
            long remaining = abilityManager.remainingMs(player, ManiacAbilityManager.Ability.SILENCE) / 1000L;
            player.sendMessage(Component.text("Silence on cooldown for " + remaining + "s.", NamedTextColor.RED));
            return true;
        }
        abilityManager.triggerCooldown(player, ManiacAbilityManager.Ability.SILENCE, silenceDuration);
        silenceManager.silence(target, silenceDuration);
        player.sendMessage(Component.text("Silenced " + target.getName() + " for a short time.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSwap(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("In-game only.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;
        if (!(player.isOp() || roleManager.isManiac(player))) {
            player.sendMessage(Component.text("Only the Maniac may swap places.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            killerSignListener.sendSwapTargets(player);
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || target.getGameMode() == GameMode.SPECTATOR || target.isDead()) {
            player.sendMessage(Component.text("Invalid target.", NamedTextColor.RED));
            return true;
        }
        if (!target.getWorld().equals(player.getWorld())) {
            player.sendMessage(Component.text("Target is in another world.", NamedTextColor.RED));
            return true;
        }
        if (roleManager.isManiac(player) && roundManager.isTeleportOnCooldown(player)) {
            player.sendMessage(Component.text("Teleport ability is on cooldown.", NamedTextColor.RED));
            return true;
        }
        Location playerLoc = player.getLocation().clone();
        Location targetLoc = target.getLocation().clone();
        playerLoc.setY(playerLoc.getY() + 0.1);
        targetLoc.setY(targetLoc.getY() + 0.1);
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.teleport(targetLoc);
            target.teleport(playerLoc);
            player.setFallDistance(0f);
            target.setFallDistance(0f);
        });
        if (roleManager.isManiac(player)) {
            roundManager.markTeleportUsed(player);
        }
        return true;
    }

    private boolean handleEntityId(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("In-game only.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /maniac entityId <entity name>", NamedTextColor.DARK_AQUA));
            return true;
        }

        Player player = (Player) sender;
        String targetName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        World world = player.getWorld();

        Entity matched = world.getEntities().stream()
                .filter(entity -> {
                    String customName = entity.getCustomName();
                    String visibleName = (customName == null || customName.isEmpty())
                            ? entity.getName()
                            : customName;
                    return visibleName.equalsIgnoreCase(targetName);
                })
                .findFirst()
                .orElse(null);

        if (matched == null) {
            sender.sendMessage(Component.text("No entity with that visible name found.", NamedTextColor.RED));
            return true;
        }

        int entityId = matched.getEntityId();
        Component message = Component.text("Entity '" + targetName + "' has id " + entityId + ".", NamedTextColor.GREEN);
        sender.sendMessage(message);
        plugin.getLogger().info("/maniac entityId resolved '" + targetName + "' to id " + entityId + " in world " + world.getName());
        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("watchbox.maniac.debug")) {
            sender.sendMessage(Component.text("Operator permission required.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /maniac debug <players|kill|revive|setrole> ...", NamedTextColor.DARK_AQUA));
            return true;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "players" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
                    return true;
                }
                if (debugBookFactory != null) {
                    debugBookFactory.sendPlayersDebugInfo(player);
                }
                return true;
            }
            case "kill" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /maniac debug kill <player>", NamedTextColor.DARK_AQUA));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                roundManager.eliminatePlayer(target, Component.text(target.getName() + " was eliminated by an admin.", NamedTextColor.RED));
                sender.sendMessage(Component.text("Eliminated " + target.getName() + ".", NamedTextColor.GREEN));
                return true;
            }
            case "revive" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /maniac debug revive <player>", NamedTextColor.DARK_AQUA));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                roundManager.revivePlayer(target);
                sender.sendMessage(Component.text("Revived " + target.getName() + ".", NamedTextColor.GREEN));
                target.sendMessage(Component.text("You have been revived by an admin.", NamedTextColor.AQUA));
                return true;
            }
            case "setrole" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /maniac debug setrole <maniac|civilian> <player>", NamedTextColor.DARK_AQUA));
                    return true;
                }
                Role role = parseDebugRole(args[2]);
                if (role == null) {
                    sender.sendMessage(Component.text("Unknown role. Use maniac or civilian.", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                roleManager.assignRole(target, role);
                sender.sendMessage(Component.text("Set role of " + target.getName() + " to " + role + ".", NamedTextColor.GREEN));
                target.sendMessage(Component.text("Your role was set to " + role + " by an admin.", NamedTextColor.YELLOW));
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown debug action.", NamedTextColor.RED));
                return true;
            }
        }
    }

    private Role parseDebugRole(String input) {
        if (input.equalsIgnoreCase("maniac")) {
            return Role.MANIAC;
        }
        if (input.equalsIgnoreCase("civilian")) {
            return Role.CIVILIAN;
        }
        return null;
    }
}

package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command entrypoint for Maniac utilities (voting books, killer sign, abilities).
 */
public class ManiacCommand implements CommandExecutor {
    private final RoleManager roleManager;
    private final SilenceManager silenceManager;
    private final ManiacAbilityManager abilityManager;
    private final VoteManager voteManager;
    private final KillerSignItem killerSignItem;
    private final KillerSignListener killerSignListener;
    private final long silenceDuration;

    public ManiacCommand(RoleManager roleManager, SilenceManager silenceManager,
                         ManiacAbilityManager abilityManager, VoteManager voteManager,
                         KillerSignItem killerSignItem, KillerSignListener killerSignListener, long silenceDuration) {
        this.roleManager = roleManager;
        this.silenceManager = silenceManager;
        this.abilityManager = abilityManager;
        this.voteManager = voteManager;
        this.killerSignItem = killerSignItem;
        this.killerSignListener = killerSignListener;
        this.silenceDuration = silenceDuration;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: votebook|killersign|silence|swap|showmarks", NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "votebook":
                return handleVoteBook(sender, args);
            case "killersign":
                return handleKillerSign(sender, args);
            case "silence":
                return handleSilence(sender, args);
            case "showmarks":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
                    return true;
                }
                Player marksViewer = (Player) sender;
                if (!(marksViewer.isOp() || roleManager.isManiac(marksViewer))) {
                    marksViewer.sendMessage(Component.text("You cannot view marks.", NamedTextColor.RED));
                    return true;
                }
                killerSignListener.sendMarked(marksViewer);
                return true;
            case "swap":
                return handleSwap(sender, args);
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
            sender.sendMessage(Component.text("Usage: /maniac votebook <player>", NamedTextColor.YELLOW));
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
            sender.sendMessage(Component.text("Usage: /maniac killersign <player>", NamedTextColor.YELLOW));
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
        if (target == null || target.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(Component.text("Invalid target.", NamedTextColor.RED));
            return true;
        }
        if (!target.getWorld().equals(player.getWorld())) {
            player.sendMessage(Component.text("Target is in another world.", NamedTextColor.RED));
            return true;
        }
        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();
        player.teleport(targetLoc);
        target.teleport(playerLoc);
        player.sendMessage(Component.text("You swapped places with " + target.getName() + "!", NamedTextColor.AQUA));
        target.sendMessage(Component.text("The Maniac swapped places with you!", NamedTextColor.RED));
        return true;
    }
}

package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /vote command from voting books.
 */
public class VoteCommand implements CommandExecutor {
    private final VoteManager voteManager;

    public VoteCommand(VoteManager voteManager) {
        this.voteManager = voteManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players may vote.", NamedTextColor.RED));
            return true;
        }
        Player voter = (Player) sender;
        if (!voteManager.isVotingPhase()) {
            voter.sendMessage(Component.text("You can only vote during the voting phase.", NamedTextColor.RED));
            return true;
        }
        if (voteManager.hasVoted(voter)) {
            voter.sendMessage(Component.text("You have already voted this round.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            voter.sendMessage(Component.text("Usage: /vote <player>", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || target.getGameMode() == GameMode.SPECTATOR) {
            voter.sendMessage(Component.text("You must vote for a living player.", NamedTextColor.RED));
            return true;
        }
        boolean success = voteManager.castVote(voter, target);
        if (success) {
            voteManager.removeVotingBook(voter);
            voter.closeInventory();
        }
        return true;
    }
}

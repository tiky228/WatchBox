package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tracks votes during the voting phase and distributes voting books.
 */
public class VoteManager {
    private final RoundManager roundManager;
    private final RoleManager roleManager;
    private final VotingBookFactory votingBookFactory;
    private final Map<UUID, UUID> votes = new HashMap<>();

    public VoteManager(JavaPlugin plugin, RoleManager roleManager, RoundManager roundManager) {
        this.roundManager = roundManager;
        this.roleManager = roleManager;
        this.votingBookFactory = new VotingBookFactory(plugin);
    }

    public void startVoting() {
        votes.clear();
        Bukkit.broadcast(Component.text("Voting phase has begun!", NamedTextColor.LIGHT_PURPLE));
        distributeVotingBooks();
    }

    public void concludeVoting() {
        Map<UUID, Long> counts = votes.values().stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        Map<UUID, Long> tallied = new LinkedHashMap<>();
        getAlivePlayers().forEach(player -> tallied.put(player.getUniqueId(), 0L));
        counts.forEach((uuid, count) -> tallied.merge(uuid, count, Long::sum));

        Bukkit.broadcast(Component.text("Voting results:", NamedTextColor.DARK_AQUA));
        tallied.entrySet().stream()
                .sorted(Comparator.comparing(entry -> getNameFor(entry.getKey())))
                .forEach(entry -> Bukkit.broadcast(Component.text(
                        getNameFor(entry.getKey()) + " - " + entry.getValue() + " vote" + (entry.getValue() == 1 ? "" : "s"),
                        NamedTextColor.GRAY)));

        clearVotingBooks();
        Optional<UUID> eliminationTarget = determineEliminationTarget(tallied);
        votes.clear();

        if (eliminationTarget.isEmpty()) {
            Bukkit.broadcast(Component.text("Voting ended with no elimination.", NamedTextColor.GRAY));
            return;
        }

        Player target = Bukkit.getPlayer(eliminationTarget.get());
        if (target == null) {
            Bukkit.broadcast(Component.text("Top voted player is no longer online.", NamedTextColor.GRAY));
            return;
        }
        target.setGameMode(GameMode.SPECTATOR);
        Bukkit.broadcast(Component.text(target.getName() + " was eliminated by vote!", NamedTextColor.RED));
        roundManager.checkWinConditions();
    }

    public void endVoting() {
        votes.clear();
        clearVotingBooks();
    }

    public void distributeVotingBooks() {
        Collection<Player> targets = getAlivePlayers();
        ItemStack book = votingBookFactory.createVotingBook(targets);
        for (Player player : targets) {
            player.getInventory().addItem(book.clone());
        }
    }

    public void giveVotingBook(Player player) {
        ItemStack book = votingBookFactory.createVotingBook(getAlivePlayers());
        player.getInventory().addItem(book);
    }

    public void clearVotingBooks() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeVotingBook(player);
        }
    }

    public boolean hasVoted(Player player) {
        return votes.containsKey(player.getUniqueId());
    }

    public boolean castVote(Player voter, Player target) {
        if (hasVoted(voter)) {
            return false;
        }
        votes.put(voter.getUniqueId(), target.getUniqueId());
        voter.sendMessage(Component.text("You voted for " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private Optional<UUID> determineEliminationTarget(Map<UUID, Long> tallied) {
        if (tallied.isEmpty()) {
            return Optional.empty();
        }
        long maxVotes = -1;
        UUID top = null;
        boolean tie = false;

        for (Map.Entry<UUID, Long> entry : tallied.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                top = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxVotes) {
                tie = true; // Tie means no elimination.
            }
        }

        if (top == null || tie || maxVotes <= 0) {
            return Optional.empty();
        }
        return Optional.of(top);
    }

    public Optional<Player> getTopVotedPlayer() {
        if (votes.isEmpty()) {
            return Optional.empty();
        }
        Map<UUID, Long> counts = votes.values().stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        Optional<Map.Entry<UUID, Long>> top = counts.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue));
        if (top.isEmpty()) {
            return Optional.empty();
        }
        long max = top.get().getValue();
        Set<UUID> ties = counts.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (ties.size() != 1) {
            return Optional.empty();
        }
        Player player = Bukkit.getPlayer(top.get().getKey());
        return Optional.ofNullable(player);
    }

    public void removeVotingBook(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (votingBookFactory.isVotingBook(item)) {
                player.getInventory().remove(item);
            }
        }
    }

    public boolean isVotingBook(ItemStack itemStack) {
        return votingBookFactory.isVotingBook(itemStack);
    }

    private String getNameFor(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : uuid.toString();
    }

    private Collection<Player> getAlivePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .filter(p -> !p.isDead())
                .collect(Collectors.toList());
    }

    public boolean isVotingPhase() {
        return roundManager.getCurrentPhase() == RoundPhase.VOTING;
    }
}

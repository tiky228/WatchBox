package com.watchbox.maniac;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoundManager {
    private static final int DEFAULT_ACTION_SECONDS = 600;
    private static final int DEFAULT_DISCUSSION_SECONDS = 60;
    private static final int VOTING_DURATION_SECONDS = 30;

    private final JavaPlugin plugin;
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final TaskManager taskManager;
    private VoteManager voteManager;

    private RoundPhase currentPhase = RoundPhase.PRE_ROUND;
    private int remainingSeconds = 0;
    private BukkitTask timerTask;

    private int actionPhaseDuration;
    private int discussionPhaseDuration;
    private int maxMarksBeforeDeath;

    private final Map<UUID, PlayerDisguise> activeDisguises = new HashMap<>();
    private Scoreboard hiddenNameScoreboard;
    private Team hiddenTeam;

    public RoundManager(JavaPlugin plugin, RoleManager roleManager, MarkManager markManager, TaskManager taskManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.taskManager = taskManager;
        reloadDurations();
    }

    public void setVoteManager(VoteManager voteManager) {
        this.voteManager = voteManager;
    }

    public void reloadDurations() {
        actionPhaseDuration = plugin.getConfig().getInt("actionPhaseDuration", DEFAULT_ACTION_SECONDS);
        discussionPhaseDuration = plugin.getConfig().getInt("discussionPhaseDuration", DEFAULT_DISCUSSION_SECONDS);
        maxMarksBeforeDeath = plugin.getConfig().getInt("maxMarksBeforeDeath", 3);
    }

    public void startRound() {
        cancelTimer();
        cancelDisguises();
        clearHiddenNameTeam();
        if (voteManager != null) {
            voteManager.endVoting();
        }

        roleManager.clear();
        markManager.clearAll();
        taskManager.resetLanterns();

        prepareParticipants();
        if (assignRoles()) {
            giveStartingSigns();
            applyRoundStartEffects();
            applyDisguises();
            setupHiddenNameTeam();

            setPhase(RoundPhase.ROUND_START, 3);
        }
    }

    public void endRound() {
        cancelTimer();
        if (voteManager != null) {
            voteManager.endVoting();
        }
        cancelDisguises();
        clearHiddenNameTeam();
        roleManager.clear();
        markManager.clearAll();
        currentPhase = RoundPhase.PRE_ROUND;
    }

    public void advancePhase() {
        switch (currentPhase) {
            case ROUND_START -> enterActionPhase();
            case ACTION -> enterDiscussionPhase();
            case DISCUSSION -> enterVotingPhase();
            case VOTING -> enterRoundEndPhase();
            case ROUND_END -> endRound();
            case PRE_ROUND -> {
            }
        }
    }

    public void forcePhase(RoundPhase phase, int seconds) {
        setPhase(phase, seconds);
    }

    public void onPlayerDeathOrLeave() {
        checkWinConditions();
    }

    public void checkWinConditions() {
        List<Player> alivePlayers = getAlivePlayers();
        long maniacsAlive = alivePlayers.stream().filter(roleManager::isManiac).count();
        long nonManiacsAlive = alivePlayers.stream().filter(player -> !roleManager.isManiac(player)).count();

        if (maniacsAlive <= 0) {
            Bukkit.broadcast(Component.text("Innocents win!", NamedTextColor.AQUA));
            endRound();
            return;
        }

        if (maniacsAlive == 1 && nonManiacsAlive == 1) {
            Bukkit.broadcast(Component.text("The Maniac wins!", NamedTextColor.RED));
            endRound();
        }
    }

    public RoundPhase getCurrentPhase() {
        return currentPhase;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(int seconds) {
        remainingSeconds = Math.max(0, seconds);
    }

    private void setPhase(RoundPhase phase, int durationSeconds) {
        cancelTimer();
        currentPhase = phase;
        remainingSeconds = durationSeconds;
        Bukkit.broadcast(Component.text("Phase: " + phase.name(), NamedTextColor.DARK_AQUA));

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                remainingSeconds--;
                if (remainingSeconds > 0) {
                    return;
                }
                cancelTimer();
                advancePhase();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void enterActionPhase() {
        setPhase(RoundPhase.ACTION, actionPhaseDuration);
    }

    private void enterDiscussionPhase() {
        resolveMarks();
        setPhase(RoundPhase.DISCUSSION, discussionPhaseDuration);
    }

    private void enterVotingPhase() {
        if (voteManager != null) {
            voteManager.startVoting();
        }
        setPhase(RoundPhase.VOTING, VOTING_DURATION_SECONDS);
    }

    private void enterRoundEndPhase() {
        if (voteManager != null) {
            voteManager.endVoting();
        }
        checkWinConditions();
        setPhase(RoundPhase.ROUND_END, 5);
    }

    private void resolveMarks() {
        for (Player player : getAlivePlayers()) {
            int total = markManager.getTotalMarks(player);
            if (total >= maxMarksBeforeDeath) {
                player.setHealth(0.0);
            }
        }
        checkWinConditions();
    }

    private void prepareParticipants() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.ADVENTURE);
            }
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
        }
    }

    private boolean assignRoles() {
        List<Player> players = new ArrayList<>(getAlivePlayers());
        if (players.isEmpty()) {
            plugin.getLogger().warning("No players available to start a round.");
            return false;
        }

        Collections.shuffle(players);
        Player maniac = players.get(0);
        roleManager.assignRole(maniac, Role.MANIAC);
        for (int i = 1; i < players.size(); i++) {
            roleManager.assignRole(players.get(i), Role.CIVILIAN);
        }
        return true;
    }

    private void giveStartingSigns() {
        ItemStack signs = new ItemStack(Material.OAK_SIGN, 6);
        for (Player player : getAlivePlayers()) {
            PlayerInventory inventory = player.getInventory();
            if (inventory.firstEmpty() == -1 && !inventory.containsAtLeast(signs, 6)) {
                continue;
            }
            inventory.addItem(signs.clone());
        }
    }

    private void applyRoundStartEffects() {
        for (Player player : getAlivePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false, false));
            player.sendTitle("Round Start", "Trust no one.", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }
    }

    private void applyDisguises() {
        cancelDisguises();
        for (Player player : getAlivePlayers()) {
            PlayerDisguise disguise = new PlayerDisguise(player);
            disguise.setNameVisible(false);
            DisguiseAPI.disguiseToAll(player, disguise);
            activeDisguises.put(player.getUniqueId(), disguise);
        }
    }

    private void cancelDisguises() {
        activeDisguises.forEach((uuid, disguise) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                DisguiseAPI.undisguiseToAll(player);
            }
        });
        activeDisguises.clear();
    }

    private void setupHiddenNameTeam() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        hiddenNameScoreboard = manager.getMainScoreboard();
        hiddenTeam = hiddenNameScoreboard.getTeam("hidden_names");
        if (hiddenTeam == null) {
            hiddenTeam = hiddenNameScoreboard.registerNewTeam("hidden_names");
        }
        hiddenTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);

        for (Player player : getAlivePlayers()) {
            hiddenTeam.addEntry(player.getName());
        }
    }

    private void clearHiddenNameTeam() {
        if (hiddenTeam != null) {
            for (String entry : new ArrayList<>(hiddenTeam.getEntries())) {
                hiddenTeam.removeEntry(entry);
            }
        }
        hiddenTeam = null;
        hiddenNameScoreboard = null;
    }

    public List<Player> getAlivePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getGameMode() != GameMode.SPECTATOR)
                .filter(player -> !player.isDead())
                .collect(Collectors.toList());
    }

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}

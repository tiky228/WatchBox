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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controls the round phases and timers.
 */
public class RoundManager {
    private static final int DEFAULT_ACTION_SECONDS = 600;
    private static final int DEFAULT_DISCUSSION_SECONDS = 60;
    private static final int VOTING_DURATION_SECONDS = 30;
    private static final int ROUND_START_BLINDNESS_TICKS = 60;

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
    private Scoreboard hiddenScoreboard;
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
        cleanupDisguises();
        if (voteManager != null) {
            voteManager.endVoting();
        }
        roleManager.clear();
        markManager.clearAll();
        taskManager.resetLanterns();
        prepareParticipants();
        assignRoles();
        giveStartingSigns();
        applyRoundStartEffects();
        setPhase(RoundPhase.ROUND_START, 3);
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

    private void assignRoles() {
        List<Player> players = new ArrayList<>(getAlivePlayers());
        if (players.isEmpty()) {
            plugin.getLogger().warning("No players online to assign roles.");
            return;
        }
        Collections.shuffle(players);
        Player maniac = players.get(0);
        roleManager.assignRole(maniac, Role.MANIAC);
        for (int i = 1; i < players.size(); i++) {
            roleManager.assignRole(players.get(i), Role.CIVILIAN);
        }
        plugin.getLogger().info("Assigned roles: " + maniac.getName() + " is the Maniac.");
    }

    private void giveStartingSigns() {
        ItemStack signs = new ItemStack(Material.OAK_SIGN, 6);
        for (Player player : getAlivePlayers()) {
            player.getInventory().addItem(signs.clone());
        }
    }

    public void forcePhase(RoundPhase phase, int durationSeconds) {
        cancelTimer();
        startPhase(phase, durationSeconds);
    }

    private void setPhase(RoundPhase phase, int durationSeconds) {
        cancelTimer();
        startPhase(phase, durationSeconds);
    }

    private void startPhase(RoundPhase phase, int durationSeconds) {
        if (currentPhase == RoundPhase.VOTING && phase != RoundPhase.VOTING && voteManager != null) {
            voteManager.concludeVoting();
        }
        currentPhase = phase;
        remainingSeconds = durationSeconds;
        Bukkit.broadcast(Component.text("Phase: " + phase, NamedTextColor.DARK_AQUA));

        switch (phase) {
            case ROUND_START:
                startTimer(() -> enterActionPhase());
                break;
            case ACTION:
                startTimer(this::enterDiscussionPhase);
                break;
            case DISCUSSION:
                startTimer(this::enterVotingPhase);
                break;
            case VOTING:
                if (voteManager != null) {
                    voteManager.startVoting();
                }
                startTimer(this::endRoundPhase);
                break;
            case ROUND_END:
                endRoundPhase();
                break;
            default:
                break;
        }
    }

    private void enterActionPhase() {
        setPhase(RoundPhase.ACTION, actionPhaseDuration);
    }

    private void enterDiscussionPhase() {
        resolveMarks();
        setPhase(RoundPhase.DISCUSSION, discussionPhaseDuration);
    }

    private void enterVotingPhase() {
        setPhase(RoundPhase.VOTING, VOTING_DURATION_SECONDS);
    }

    public void advancePhase() {
        switch (currentPhase) {
            case ROUND_START:
                enterActionPhase();
                break;
            case ACTION:
                enterDiscussionPhase();
                break;
            case DISCUSSION:
                enterVotingPhase();
                break;
            case VOTING:
                setPhase(RoundPhase.ROUND_END, 0);
                break;
            default:
                break;
        }
    }

    private void resolveMarks() {
        plugin.getLogger().info("Resolving marks for end of action phase.");
        for (Player player : getAlivePlayers()) {
            int total = markManager.getTotalMarks(player);
            if (total >= maxMarksBeforeDeath) {
                player.setHealth(0.0);
                plugin.getLogger().info(player.getName() + " was slain by accumulated marks (" + total + ").");
            }
        }
        checkWinConditions();
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

    public void stopRound() {
        cancelTimer();
        currentPhase = RoundPhase.ROUND_END;
        remainingSeconds = 0;
        if (voteManager != null) {
            voteManager.endVoting();
        }
        cleanupDisguises();
        restoreNameTags();
        Bukkit.broadcast(Component.text("Round ended.", NamedTextColor.GRAY));
    }

    private void startTimer(Runnable onComplete) {
        cancelTimer();
        if (remainingSeconds <= 0) {
            onComplete.run();
            return;
        }
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                cancelTimer();
                onComplete.run();
            }
        }, 20L, 20L);
    }

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private Collection<Player> getAlivePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .filter(p -> !p.isDead())
                .collect(Collectors.toList());
    }

    private void applyRoundStartEffects() {
        markManager.clearAll();
        disguisePlayers();
        hideNameTags();
        for (Player player : getAlivePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ROUND_START_BLINDNESS_TICKS, 1, false, false, false));
            player.showTitle(org.bukkit.Title.title(
                    Component.text("Round Start", NamedTextColor.RED),
                    Component.text("Trust no one…", NamedTextColor.DARK_GRAY)
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }
    }

    private void disguisePlayers() {
        cleanupDisguises();
        for (Player player : getAlivePlayers()) {
            PlayerDisguise disguise = new PlayerDisguise("Steve");
            disguise.setNameVisible(false);
            DisguiseAPI.disguiseToAll(player, disguise);
            activeDisguises.put(player.getUniqueId(), disguise);
        }
    }

    private void cleanupDisguises() {
        activeDisguises.keySet().forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                DisguiseAPI.undisguiseToAll(player);
            }
        });
        activeDisguises.clear();
    }

    private void hideNameTags() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        hiddenScoreboard = manager.getNewScoreboard();
        hiddenTeam = hiddenScoreboard.registerNewTeam("maniac_hidden");
        hiddenTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        for (Player player : Bukkit.getOnlinePlayers()) {
            hiddenTeam.addEntry(player.getName());
            player.setScoreboard(hiddenScoreboard);
        }
    }

    private void restoreNameTags() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        Scoreboard main = manager.getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(main);
        }
        if (hiddenTeam != null) {
            hiddenTeam.unregister();
        }
        hiddenScoreboard = null;
        hiddenTeam = null;
    }

    private void endRoundPhase() {
        if (voteManager != null && currentPhase == RoundPhase.VOTING) {
            voteManager.concludeVoting();
        }
        checkWinConditions();
        cleanupDisguises();
        restoreNameTags();
        currentPhase = RoundPhase.ROUND_END;
    }

    public void checkWinConditions() {
        long maniacsAlive = getAlivePlayers().stream().filter(roleManager::isManiac).count();
        long civiliansAlive = getAlivePlayers().stream().filter(p -> roleManager.getRole(p) == Role.CIVILIAN).count();

        if (maniacsAlive <= 0) {
            announceWin("Innocents Win", NamedTextColor.AQUA);
            stopRound();
            return;
        }
        if (maniacsAlive == 1 && civiliansAlive == 1) {
            announceWin("Maniac Triumphs", NamedTextColor.RED);
            stopRound();
        }
    }

    private void announceWin(String title, NamedTextColor color) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(org.bukkit.Title.title(Component.text(title, color), Component.empty()));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }
}

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controls the round phases and timers.
 */
public class RoundManager {
    private final JavaPlugin plugin;
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final TaskManager taskManager;

    private RoundPhase currentPhase = RoundPhase.LOBBY;
    private int remainingSeconds = 0;
    private BukkitTask timerTask;

    private int actionPhaseDuration;
    private int discussionPhaseDuration;
    private int maxMarksBeforeDeath;

    public RoundManager(JavaPlugin plugin, RoleManager roleManager, MarkManager markManager, TaskManager taskManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.taskManager = taskManager;
        reloadDurations();
    }

    public void reloadDurations() {
        actionPhaseDuration = plugin.getConfig().getInt("actionPhaseDuration", 120);
        discussionPhaseDuration = plugin.getConfig().getInt("discussionPhaseDuration", 60);
        maxMarksBeforeDeath = plugin.getConfig().getInt("maxMarksBeforeDeath", 3);
    }

    public void startRound() {
        cancelTimer();
        roleManager.clear();
        markManager.clearAll();
        taskManager.resetLanterns();
        assignRoles();
        giveStartingSigns();
        setPhase(RoundPhase.ACTION, actionPhaseDuration);
    }

    private void assignRoles() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            taskManager.giveStartingSigns(player);
        }
    }

    public void forcePhase(RoundPhase phase, int durationSeconds) {
        cancelTimer();
        setPhase(phase, durationSeconds);
    }

    private void setPhase(RoundPhase phase, int durationSeconds) {
        currentPhase = phase;
        remainingSeconds = durationSeconds;
        plugin.getLogger().info("Phase set to " + phase + " for " + remainingSeconds + "s.");
        Bukkit.broadcast(Component.text("Phase: " + phase + " (" + remainingSeconds + "s)", NamedTextColor.LIGHT_PURPLE));
        if (phase == RoundPhase.ACTION || phase == RoundPhase.DISCUSSION) {
            startTimer();
        }
    }

    private void startTimer() {
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                advancePhase();
            }
        }, 20L, 20L);
    }

    private void advancePhase() {
        cancelTimer();
        if (currentPhase == RoundPhase.ACTION) {
            resolveMarks();
            setPhase(RoundPhase.DISCUSSION, discussionPhaseDuration);
            return;
        }
        if (currentPhase == RoundPhase.DISCUSSION) {
            setPhase(RoundPhase.ENDED, 0);
            return;
        }
        if (currentPhase == RoundPhase.LOBBY) {
            setPhase(RoundPhase.ACTION, actionPhaseDuration);
        }
    }

    private void resolveMarks() {
        plugin.getLogger().info("Resolving marks for end of action phase.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            int total = markManager.getTotalMarks(player);
            if (total >= maxMarksBeforeDeath) {
                player.setHealth(0.0);
                plugin.getLogger().info(player.getName() + " was slain by accumulated marks (" + total + ").");
            }
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

    public void stopRound() {
        cancelTimer();
        currentPhase = RoundPhase.ENDED;
        remainingSeconds = 0;
        Bukkit.broadcast(Component.text("Round ended.", NamedTextColor.GRAY));
    }

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}

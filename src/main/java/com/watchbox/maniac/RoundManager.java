package com.watchbox.maniac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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

public class RoundManager {
    private static final int DEFAULT_ACTION_SECONDS = 600;
    private static final int DEFAULT_DISCUSSION_SECONDS = 60;
    private static final int VOTING_DURATION_SECONDS = 30;
    private static final String DISGUISE_SKIN = "Steve";

    private final JavaPlugin plugin;
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final TaskManager taskManager;
    private final MarkTokenManager markTokenManager;
    private KillerSignItem killerSignItem;
    private VoteManager voteManager;

    private final Map<UUID, RoundMarkState> roundMarkStates = new HashMap<>();

    private RoundPhase currentPhase = RoundPhase.PRE_ROUND;
    private int remainingSeconds = 0;
    private BukkitTask timerTask;

    private int actionPhaseDuration;
    private int discussionPhaseDuration;
    private int maxMarksBeforeDeath;

    private final Map<UUID, PlayerDisguise> activeDisguises = new HashMap<>();
    private Scoreboard hiddenNameScoreboard;
    private Team hiddenTeam;
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();

    private static class RoundMarkState {
        private boolean usedMarkToken;
        private UUID markedPlayer;
    }

    public enum WinnerType {
        MANIAC,
        INNOCENTS
    }

    public RoundManager(JavaPlugin plugin, RoleManager roleManager, MarkManager markManager, TaskManager taskManager, MarkTokenManager markTokenManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.taskManager = taskManager;
        this.markTokenManager = markTokenManager;
        reloadDurations();
    }

    public void setVoteManager(VoteManager voteManager) {
        this.voteManager = voteManager;
    }

    public void setKillerSignItem(KillerSignItem killerSignItem) {
        this.killerSignItem = killerSignItem;
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
        clearSavedInventories();
        markTokenManager.removeTokensFromAll();
        if (voteManager != null) {
            voteManager.endVoting();
        }

        roleManager.clear();
        markManager.clearAll();
        taskManager.resetLanterns();
        resetRoundMarkState();

        prepareParticipants();
        if (assignRoles()) {
            announceRoles();
            giveStartingSigns();
            giveKillerSigns();
            giveMarkTokens();
            applyRoundStartEffects();

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
        restoreInventories();
        clearSavedInventories();
        markTokenManager.removeTokensFromAll();
        roleManager.clear();
        markManager.clearAll();
        resetRoundMarkState();
        currentPhase = RoundPhase.PRE_ROUND;
    }

    public void advancePhase() {
        switch (currentPhase) {
            case ROUND_START -> enterActionPhase();
            case ACTION -> enterDiscussionPhase();
            case DISCUSSION -> enterVotingPhase();
            case VOTING -> enterRoundEndPhase();
            case ROUND_END -> enterActionPhase();
            case PRE_ROUND -> {
            }
        }
    }

    public void forcePhase(RoundPhase phase, int seconds) {
        setPhase(phase, seconds);
    }

    public void onPlayerDeathOrLeave() {
        cleanupDisguisesForInactivePlayers();
        cleanupMarkTargets();
        checkGameEnd();
    }

    public WinnerType checkGameEnd() {
        List<Player> alivePlayers = getAlivePlayers();
        long maniacsAlive = alivePlayers.stream().filter(roleManager::isManiac).count();
        long innocentsAlive = alivePlayers.stream().filter(player -> !roleManager.isManiac(player)).count();

        if (maniacsAlive <= 0) {
            endGameAndReturnToPregame(WinnerType.INNOCENTS);
            return WinnerType.INNOCENTS;
        }

        if (innocentsAlive <= maniacsAlive) {
            endGameAndReturnToPregame(WinnerType.MANIAC);
            return WinnerType.MANIAC;
        }
        return null;
    }

    public void checkWinConditions() {
        checkGameEnd();
    }

    public void endGameAndReturnToPregame(WinnerType winner) {
        if (winner == null || currentPhase == RoundPhase.PRE_ROUND) {
            return;
        }
        Component announcement = winner == WinnerType.MANIAC
                ? Component.text("The Maniac wins!", NamedTextColor.RED)
                : Component.text("Innocents win!", NamedTextColor.AQUA);
        Bukkit.broadcast(announcement);
        endRound();
    }

    public RoundPhase getCurrentPhase() {
        return currentPhase;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public long countAliveManiacs() {
        return getAlivePlayers().stream().filter(roleManager::isManiac).count();
    }

    public long countAliveCivilians() {
        return getAlivePlayers().stream().filter(roleManager::isCivilian).count();
    }

    public long countSpectators() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getGameMode() == GameMode.SPECTATOR || player.isDead())
                .count();
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
        resetRoundMarkState();
        restoreInventories();
        giveMarkTokens();
        applyDisguises();
        setupHiddenNameTeam();
        setPhase(RoundPhase.ACTION, actionPhaseDuration);
    }

    private void enterDiscussionPhase() {
        cancelDisguises();
        clearHiddenNameTeam();
        saveAndClearInventories();
        setPhase(RoundPhase.DISCUSSION, discussionPhaseDuration);
        executeMarkedPlayers();
        if (currentPhase == RoundPhase.PRE_ROUND) {
            return;
        }
        resolveMarks();
    }

    private void enterVotingPhase() {
        cancelDisguises();
        clearHiddenNameTeam();
        clearInventoriesForAlivePlayers();
        if (voteManager != null) {
            voteManager.startVoting();
        }
        setPhase(RoundPhase.VOTING, VOTING_DURATION_SECONDS);
    }

    private void enterRoundEndPhase() {
        if (voteManager != null) {
            voteManager.concludeVoting();
        }
        checkWinConditions();
        if (currentPhase == RoundPhase.PRE_ROUND) {
            return;
        }
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

    public boolean handleMarkTokenUse(Player maniac, Player target) {
        if (maniac == null || target == null) {
            return false;
        }
        if (!roleManager.isManiac(maniac)) {
            return false;
        }

        RoundMarkState state = roundMarkStates.computeIfAbsent(maniac.getUniqueId(), id -> new RoundMarkState());
        if (state.usedMarkToken) {
            return false;
        }

        state.usedMarkToken = true;
        state.markedPlayer = target.getUniqueId();

        markManager.addNormalMark(target, 1);
        markManager.addMarkedEntity(target);
        markTokenManager.removeTokens(maniac);
        return true;
    }

    private void prepareParticipants() {
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
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

    private void announceRoles() {
        for (Player player : getAlivePlayers()) {
            Role role = roleManager.getRole(player);
            if (role == Role.MANIAC) {
                player.sendMessage(Component.text("You are the Maniac. You can place one maniac mark each round and carry a killer sign to eliminate others.", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("You are an innocent. Talk with other players and complete tasks to survive.", NamedTextColor.GREEN));
            }
        }
    }

    private void giveMarkTokens() {
        for (Player player : getAlivePlayers()) {
            if (roleManager.isManiac(player)) {
                markTokenManager.giveToken(player);
            } else {
                markTokenManager.removeTokens(player);
            }
        }
    }

    private void giveKillerSigns() {
        if (killerSignItem == null) {
            return;
        }
        for (Player player : getAlivePlayers()) {
            if (!roleManager.isManiac(player)) {
                continue;
            }
            boolean hasSign = false;
            for (ItemStack stack : player.getInventory().getContents()) {
                if (killerSignItem.isKillerSign(stack)) {
                    hasSign = true;
                    break;
                }
            }
            if (!hasSign) {
                player.getInventory().addItem(killerSignItem.createItem());
            }
        }
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
            PlayerDisguise disguise = new PlayerDisguise(DISGUISE_SKIN);
            disguise.setSkin(DISGUISE_SKIN);
            disguise.setName(DISGUISE_SKIN);
            disguise.setNameVisible(false);
            DisguiseAPI.disguiseToAll(player, disguise);
            activeDisguises.put(player.getUniqueId(), disguise);
        }
    }

    public void removeDisguise(Player player) {
        if (player == null) {
            return;
        }
        if (activeDisguises.remove(player.getUniqueId()) != null) {
            DisguiseAPI.undisguiseToAll(player);
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
        cleanupDisguisesForInactivePlayers();
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getGameMode() != GameMode.SPECTATOR)
                .filter(player -> !player.isDead())
                .collect(Collectors.toList());
    }

    private void cleanupDisguisesForInactivePlayers() {
        for (UUID uuid : new ArrayList<>(activeDisguises.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                if (player != null) {
                    DisguiseAPI.undisguiseToAll(player);
                }
                activeDisguises.remove(uuid);
            }
        }
    }

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void resetRoundMarkState() {
        roundMarkStates.clear();
    }

    private void executeMarkedPlayers() {
        List<Player> targets = getMarkedPlayersThisRound();
        if (targets.isEmpty()) {
            return;
        }

        for (Player target : targets) {
            if (target.getGameMode() == GameMode.SPECTATOR || target.isDead()) {
                continue;
            }
            eliminateMarkedPlayer(target);
        }
        clearMarkedTargets();
        onPlayerDeathOrLeave();
    }

    private void eliminateMarkedPlayer(Player target) {
        removeDisguise(target);
        target.setHealth(0.0);
        target.setGameMode(GameMode.SPECTATOR);
        markManager.clearAllMarks(target);
        markManager.removeMarkedEntity(target);
        Bukkit.broadcast(Component.text(target.getName() + " succumbed to the Maniac's mark!", NamedTextColor.RED));
    }

    public List<Player> getMarkedPlayersThisRound() {
        return roundMarkStates.values().stream()
                .map(state -> state.markedPlayer)
                .filter(Objects::nonNull)
                .distinct()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(player -> player.getGameMode() != GameMode.SPECTATOR)
                .filter(player -> !player.isDead())
                .collect(Collectors.toList());
    }

    private void clearMarkedTargets() {
        roundMarkStates.values().forEach(state -> state.markedPlayer = null);
    }

    private void cleanupMarkTargets() {
        for (UUID maniacId : new ArrayList<>(roundMarkStates.keySet())) {
            RoundMarkState state = roundMarkStates.get(maniacId);
            Player maniac = Bukkit.getPlayer(maniacId);
            if (maniac == null || maniac.isDead() || maniac.getGameMode() == GameMode.SPECTATOR) {
                roundMarkStates.remove(maniacId);
                continue;
            }
            if (state != null && state.markedPlayer != null) {
                Player marked = Bukkit.getPlayer(state.markedPlayer);
                if (marked == null || marked.isDead() || marked.getGameMode() == GameMode.SPECTATOR) {
                    if (marked != null) {
                        markManager.removeMarkedEntity(marked);
                    }
                    state.markedPlayer = null;
                }
            }
        }
    }

    private void saveAndClearInventories() {
        for (Player player : getAlivePlayers()) {
            PlayerInventory inventory = player.getInventory();
            savedInventories.put(player.getUniqueId(), cloneItems(inventory.getContents()));
            savedArmor.put(player.getUniqueId(), cloneItems(inventory.getArmorContents()));
            inventory.clear();
            inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
        }
    }

    private void clearInventoriesForAlivePlayers() {
        for (Player player : getAlivePlayers()) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[player.getInventory().getArmorContents().length]);
        }
    }

    private void restoreInventories() {
        for (UUID uuid : new ArrayList<>(savedInventories.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                continue;
            }
            PlayerInventory inventory = player.getInventory();
            ItemStack[] contents = savedInventories.get(uuid);
            ItemStack[] armor = savedArmor.getOrDefault(uuid, new ItemStack[0]);
            if (contents != null) {
                inventory.setContents(cloneItems(contents));
            }
            if (armor.length > 0) {
                inventory.setArmorContents(cloneItems(armor));
            }
        }
        clearSavedInventories();
    }

    private void clearSavedInventories() {
        savedInventories.clear();
        savedArmor.clear();
    }

    private ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] copy = Arrays.copyOf(source, source.length);
        for (int i = 0; i < copy.length; i++) {
            if (copy[i] != null) {
                copy[i] = copy[i].clone();
            }
        }
        return copy;
    }
}

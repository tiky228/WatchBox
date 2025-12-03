package com.watchbox.maniac;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin entry for Maniac test mode.
 */
public class TestPlugin extends JavaPlugin {
    private RoleManager roleManager;
    private MarkManager markManager;
    private SilenceManager silenceManager;
    private TaskManager taskManager;
    private RoundManager roundManager;
    private ManiacAbilityManager abilityManager;
    private MurdererWeapon murdererWeapon;
    private DebugBookFactory debugBookFactory;
    private VoteManager voteManager;
    private KillerSignItem killerSignItem;
    private KillerSignListener killerSignListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        roleManager = new RoleManager();
        markManager = new MarkManager();
        silenceManager = new SilenceManager();
        abilityManager = new ManiacAbilityManager();
        murdererWeapon = new MurdererWeapon();
        taskManager = new TaskManager(this, roleManager, markManager);
        taskManager.loadFromConfig(getConfig());
        roundManager = new RoundManager(this, roleManager, markManager, taskManager);
        voteManager = new VoteManager(this, roleManager, roundManager);
        roundManager.setVoteManager(voteManager);
        debugBookFactory = new DebugBookFactory(this, roleManager, roundManager, markManager);
        killerSignItem = new KillerSignItem(this);
        killerSignListener = new KillerSignListener(this, roleManager, markManager);

        long silenceDuration = getConfig().getLong("signSilenceDurationTicks", 200L);
        boolean logSigns = getConfig().getBoolean("logSignsToChat", true);
        long normalCooldown = getConfig().getLong("normalMarkCooldown", 60L);
        long empoweredCooldown = getConfig().getLong("empoweredMarkCooldown", 120L);
        boolean empoweredEnabled = getConfig().getBoolean("maniacEmpoweredMarkEnabled", true);

        // Commands
        if (getCommand("maniacdebug") != null) {
            getCommand("maniacdebug").setExecutor(new ManiacDebugCommand(roleManager, markManager, silenceManager, taskManager, roundManager, abilityManager, debugBookFactory, voteManager, silenceDuration, normalCooldown, empoweredCooldown, empoweredEnabled));
        }
        if (getCommand("role") != null) {
            getCommand("role").setExecutor(new RoleCommand(roleManager));
        }
        if (getCommand("round") != null) {
            getCommand("round").setExecutor(new RoundCommand(roundManager));
        }
        if (getCommand("maniac") != null) {
            getCommand("maniac").setExecutor(new ManiacCommand(this, roleManager, silenceManager, abilityManager, voteManager, killerSignItem, killerSignListener, silenceDuration));
        }
        if (getCommand("vote") != null) {
            getCommand("vote").setExecutor(new VoteCommand(voteManager));
        }
        if (getCommand("dbghighlight") != null) {
            getCommand("dbghighlight").setExecutor(new DebugHighlightCommand(this, markManager, roleManager));
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new SignListener(silenceManager, logSigns), this);
        getServer().getPluginManager().registerEvents(new TaskListener(taskManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(taskManager), this);
        getServer().getPluginManager().registerEvents(new MurdererWeaponListener(this, roleManager, markManager, murdererWeapon), this);
        getServer().getPluginManager().registerEvents(killerSignListener, this);
        getServer().getPluginManager().registerEvents(new DebugBookListener(debugBookFactory), this);

        getLogger().info("Watchbox Maniac test plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Watchbox Maniac test plugin disabled.");
    }
}

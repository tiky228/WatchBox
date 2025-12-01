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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        roleManager = new RoleManager();
        markManager = new MarkManager();
        silenceManager = new SilenceManager();
        abilityManager = new ManiacAbilityManager();
        taskManager = new TaskManager(this, roleManager, markManager);
        taskManager.loadFromConfig(getConfig());
        roundManager = new RoundManager(this, roleManager, markManager, taskManager);

        long silenceDuration = getConfig().getLong("signSilenceDurationTicks", 200L);
        boolean logSigns = getConfig().getBoolean("logSignsToChat", true);
        long normalCooldown = getConfig().getLong("normalMarkCooldown", 60L);
        long empoweredCooldown = getConfig().getLong("empoweredMarkCooldown", 120L);
        boolean empoweredEnabled = getConfig().getBoolean("maniacEmpoweredMarkEnabled", true);

        // Commands
        if (getCommand("maniacdebug") != null) {
            getCommand("maniacdebug").setExecutor(new ManiacDebugCommand(roleManager, markManager, silenceManager, taskManager, roundManager, abilityManager, silenceDuration, normalCooldown, empoweredCooldown, empoweredEnabled));
        }
        if (getCommand("role") != null) {
            getCommand("role").setExecutor(new RoleCommand(roleManager));
        }
        if (getCommand("round") != null) {
            getCommand("round").setExecutor(new RoundCommand(roundManager));
        }
        if (getCommand("dbghighlight") != null) {
            getCommand("dbghighlight").setExecutor(new DebugHighlightCommand(this, markManager, roleManager));
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new SignListener(silenceManager, logSigns), this);
        getServer().getPluginManager().registerEvents(new TaskListener(taskManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(taskManager), this);

        getLogger().info("Watchbox Maniac test plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Watchbox Maniac test plugin disabled.");
    }
}

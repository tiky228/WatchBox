import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin entry. Registers managers, commands, and listeners for the role/mark demo.
 */
public class TestPlugin extends JavaPlugin {
    private RoleManager roleManager;
    private MarkManager markManager;
    private MurdererWeapon weapon;
    private BlackFlashManager blackFlashManager;

    @Override
    public void onEnable() {
        roleManager = new RoleManager();
        markManager = new MarkManager();
        weapon = new MurdererWeapon();
        blackFlashManager = new BlackFlashManager(this);

        // Register command executors for debug tools.
        if (getCommand("dbgrole") != null) {
            getCommand("dbgrole").setExecutor(new DebugRoleCommand(roleManager));
        }
        if (getCommand("dbgweapon") != null) {
            getCommand("dbgweapon").setExecutor(new DebugWeaponCommand(roleManager, weapon));
        }
        if (getCommand("dbghighlight") != null) {
            getCommand("dbghighlight").setExecutor(new DebugHighlightCommand(roleManager, markManager, this));
        }
        if (getCommand("blackflash") != null) {
            getCommand("blackflash").setExecutor(new BlackFlashCommand(blackFlashManager));
        }

        // Listen for weapon interactions and cleanup.
        getServer().getPluginManager().registerEvents(new MurdererWeaponListener(roleManager, markManager, weapon), this);
        getServer().getPluginManager().registerEvents(new BlackFlashListener(blackFlashManager), this);

        getLogger().info("Role/Mark test plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Role/Mark test plugin disabled.");
    }
}

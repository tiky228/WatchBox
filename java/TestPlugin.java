import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.Duration;

/**
 * Main plugin class that handles lifecycle and registers command and events for the test plugin.
 */
public class TestPlugin extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        // Register the event listener and command executor.
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("testplugin") != null) {
            getCommand("testplugin").setExecutor(this);
        }

        // Log to console and broadcast to players that the plugin is enabled.
        getLogger().info("[TestPlugin] Plugin enabled successfully!");
        Bukkit.getOnlinePlayers().forEach(player ->
                player.sendMessage(Component.text("TestPlugin is now enabled!", NamedTextColor.AQUA))
        );
    }

    @Override
    public void onDisable() {
        // Log to console when the plugin is disabled.
        getLogger().info("[TestPlugin] Plugin disabled.");
    }

    /**
     * Listener for player join events to welcome players with messages and titles.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Send chat message
        player.sendMessage(Component.text("Welcome to the server, " + player.getName() + "! (from TestPlugin)", NamedTextColor.GREEN));

        // Show title and subtitle
        Title title = Title.title(
                Component.text("Welcome!", NamedTextColor.GOLD),
                Component.text("This is a TestPlugin on Paper 1.20.4", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        );
        player.showTitle(title);

        // Show action bar message
        player.sendActionBar(Component.text("Have fun on the server!", NamedTextColor.LIGHT_PURPLE));
    }

    /**
     * Command executor for /testplugin to demonstrate plugin functionality with messages and a firework.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Send confirmation messages
        player.sendMessage(Component.text("TestPlugin command executed!", NamedTextColor.GREEN));
        player.sendActionBar(Component.text("Effect triggered!", NamedTextColor.GOLD));
        player.showTitle(Title.title(
                Component.text("Effect triggered!", NamedTextColor.AQUA),
                Component.text("Enjoy the firework!", NamedTextColor.WHITE),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofMillis(200))
        ));

        // Launch a small firework without damaging the player
        launchFirework(player.getLocation());

        return true;
    }

    private void launchFirework(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class, fw -> {
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL)
                    .withColor(Color.AQUA)
                    .withFade(Color.WHITE)
                    .trail(true)
                    .build());
            meta.setPower(0);
            fw.setFireworkMeta(meta);
            fw.setShotAtAngle(false);
            fw.setTicksToDetonate(20);
        });

        // Detonate shortly after spawn to ensure it is visible but harmless.
        new BukkitRunnable() {
            @Override
            public void run() {
                firework.detonate();
            }
        }.runTaskLater(this, 20L); // 1 second later
    }
}

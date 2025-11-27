package com.example.testplugin;

import com.example.testplugin.commands.TestPluginCommand;
import com.example.testplugin.listeners.PlayerJoinListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class responsible for lifecycle and registrations.
 */
public class TestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Log to console
        getLogger().info("[TestPlugin] Plugin enabled successfully!");

        // Broadcast to online players
        Bukkit.getServer().broadcast(Component.text("TestPlugin is now enabled!"));

        // Register event listeners and command executors
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        if (getCommand("testplugin") != null) {
            getCommand("testplugin").setExecutor(new TestPluginCommand());
        } else {
            getLogger().warning("Command 'testplugin' is not defined in plugin.yml");
        }
    }

    @Override
    public void onDisable() {
        // Log to console when plugin shuts down
        getLogger().info("[TestPlugin] Plugin disabled.");
    }
}

package com.example.testplugin.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;

/**
 * Handles player join events to greet players using multiple channels.
 */
public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        // Send a welcome chat message
        player.sendMessage(Component.text("Welcome to the server, " + player.getName() + "! (from TestPlugin)"));

        // Display title and subtitle with fade timings
        Title title = Title.title(
                Component.text("Welcome!"),
                Component.text("This is a TestPlugin"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        player.showTitle(title);

        // Send an action bar message
        player.sendActionBar(Component.text("Have fun on the server!"));
    }
}

package com.watchbox.maniac;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Supplies players with basic items when they join during a round.
 */
public class PlayerJoinListener implements Listener {
    private final TaskManager taskManager;

    public PlayerJoinListener(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        taskManager.giveStartingSigns(event.getPlayer());
    }
}

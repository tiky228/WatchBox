package com.watchbox.maniac;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles player interactions with task blocks and items.
 */
public class TaskListener implements Listener {
    private final TaskManager taskManager;

    public TaskListener(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (taskManager.tryConsumeLivingWater(player, item)) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Location loc = event.getClickedBlock().getLocation();
        Material type = event.getClickedBlock().getType();
        if (type == Material.LANTERN) {
            if (taskManager.handleLanternInteract(player, loc)) {
                event.setCancelled(true);
            }
            return;
        }
        if (type == Material.BELL) {
            if (taskManager.handleBellInteract(player, loc)) {
                event.setCancelled(true);
            }
            return;
        }
        if (taskManager.handleWellInteract(player, loc)) {
            event.setCancelled(true);
        }
    }
}

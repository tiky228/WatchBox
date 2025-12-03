package com.watchbox.maniac;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Refreshes debug books before they are opened.
 */
public class DebugBookListener implements Listener {
    private final DebugBookFactory debugBookFactory;

    public DebugBookListener(DebugBookFactory debugBookFactory) {
        this.debugBookFactory = debugBookFactory;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!debugBookFactory.isDebugBook(item)) {
            return;
        }
        BookMeta meta = (BookMeta) debugBookFactory.createDebugBook().getItemMeta();
        if (meta != null) {
            item.setItemMeta(meta);
        }
    }
}

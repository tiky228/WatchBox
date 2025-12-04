package com.watchbox.maniac;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;

public class ChatPhaseListener implements Listener {
    private final RoundManager roundManager;

    public ChatPhaseListener(RoundManager roundManager) {
        this.roundManager = roundManager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("watchbox.maniac.chat.bypass")) {
            return;
        }

        RoundPhase phase = roundManager.getCurrentPhase();
        if (phase == RoundPhase.ACTION || phase == RoundPhase.ROUND_START) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Chat is disabled during the action phase.", NamedTextColor.RED));
        }
    }
}

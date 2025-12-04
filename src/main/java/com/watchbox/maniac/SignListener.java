package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Logs sign usage and enforces silence.
 */
public class SignListener implements Listener {
    private final SilenceManager silenceManager;
    private final boolean logSigns;
    private final RoleManager roleManager;
    private final RoundManager roundManager;

    public SignListener(SilenceManager silenceManager, boolean logSigns, RoleManager roleManager, RoundManager roundManager) {
        this.silenceManager = silenceManager;
        this.logSigns = logSigns;
        this.roleManager = roleManager;
        this.roundManager = roundManager;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (silenceManager.isSilenced(event.getPlayer())) {
            for (int i = 0; i < event.lines().size(); i++) {
                event.line(i, Component.text(""));
            }
        }
        if (!logSigns) {
            return;
        }
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        StringBuilder builder = new StringBuilder();
        for (Component line : event.lines()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(plain.serialize(line));
        }
        Component message = Component.text("[BigBrother] " + event.getPlayer().getName() + " wrote: " + builder);
        event.getPlayer().getServer().getConsoleSender().sendMessage(message);
        event.getPlayer().getServer().getOnlinePlayers().stream()
                .filter(Player::isOp)
                .forEach(player -> player.sendMessage(message));

        maybeNotifyManiac(event.getPlayer(), event.lines(), event.getPlayer().getName());
    }

    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }

        maybeNotifyManiac(event.getPlayer(), sign.getSide(Side.FRONT).lines(), null);
    }

    private void maybeNotifyManiac(Player player, Iterable<Component> lines, String authorName) {
        if (!roleManager.isManiac(player)) {
            return;
        }
        if (roundManager.getCurrentPhase() == RoundPhase.PRE_ROUND) {
            return;
        }

        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        StringBuilder builder = new StringBuilder();
        for (Component line : lines) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(plain.serialize(line));
        }
        String author = (authorName != null && !authorName.isEmpty()) ? authorName : "<Unknown>";
        player.sendMessage(Component.text("[BigBrother] " + author + ": " + builder));
    }
}

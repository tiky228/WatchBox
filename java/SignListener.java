package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Logs sign usage and enforces silence.
 */
public class SignListener implements Listener {
    private final SilenceManager silenceManager;
    private final boolean logSigns;

    public SignListener(SilenceManager silenceManager, boolean logSigns) {
        this.silenceManager = silenceManager;
        this.logSigns = logSigns;
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
        event.getPlayer().getServer().broadcast(Component.text("[SignLog] " + event.getPlayer().getName() + " wrote: " + builder));
    }
}

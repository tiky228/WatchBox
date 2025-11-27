package com.example.testplugin.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Simple command to demonstrate messaging and a firework effect.
 */
public class TestPluginCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        // Send confirmation in chat
        player.sendMessage(Component.text("/testplugin command executed successfully!"));

        // Show an action bar message
        player.sendActionBar(Component.text("TestPlugin command received."));

        // Display a quick title
        player.showTitle(net.kyori.adventure.title.Title.title(
                Component.text("TestPlugin"),
                Component.text("Command executed!"))
        );

        // Spawn and detonate a small firework at the player's location
        launchFirework(player.getLocation());
        return true;
    }

    private void launchFirework(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class, fw -> {
            FireworkMeta meta = fw.getFireworkMeta();
            FireworkEffect effect = FireworkEffect.builder()
                    .withColor(Color.AQUA)
                    .withFade(Color.WHITE)
                    .with(FireworkEffect.Type.BALL)
                    .trail(true)
                    .flicker(true)
                    .build();
            meta.addEffect(effect);
            meta.setPower(0); // Small and quick effect
            fw.setFireworkMeta(meta);
        });

        // Detonate shortly after spawn for immediate feedback
        firework.detonate();
    }
}

package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles right-click interactions with the Killer Sign and provides the Maniac ability UI.
 */
public class KillerSignListener implements Listener {
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final ManiacGlowHelper glowHelper;
    private final NamespacedKey killerSignKey;

    public KillerSignListener(JavaPlugin plugin, RoleManager roleManager, MarkManager markManager, ManiacGlowHelper glowHelper) {
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.glowHelper = glowHelper;
        this.killerSignKey = new NamespacedKey(plugin, "killer_sign");
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

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isKillerSign(item)) {
            return;
        }

        event.setCancelled(true);

        if (!(player.isOp() || roleManager.isManiac(player))) {
            player.sendMessage(Component.text("Only the Maniac can use this sign.", NamedTextColor.RED));
            return;
        }

        sendMainMenu(player);
    }

    private boolean isKillerSign(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(killerSignKey, PersistentDataType.BYTE);
    }

    private void sendMainMenu(Player player) {
        player.sendMessage(Component.text("Killer Abilities", NamedTextColor.DARK_RED));
        player.sendMessage(Component.text("[Silence player]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/maniac silence")));
        player.sendMessage(Component.text("[Show marked players]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/maniac showmarks")));
        player.sendMessage(Component.text("[Swap places with player]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/maniac swap")));
    }

    public void sendSilenceTargets(Player player) {
        List<Player> targets = getAlivePlayers(player.getUniqueId());
        if (targets.isEmpty()) {
            player.sendMessage(Component.text("No available targets.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Select a target to silence:", NamedTextColor.DARK_AQUA));
        for (Player target : targets) {
            player.sendMessage(Component.text(target.getName() + " ", NamedTextColor.GRAY)
                    .append(Component.text("[SILENCE]", NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/maniac silence " + target.getName()))));
        }
    }

    public void sendSwapTargets(Player player) {
        List<Player> targets = getAlivePlayers(player.getUniqueId());
        if (targets.isEmpty()) {
            player.sendMessage(Component.text("No available targets.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Swap with:", NamedTextColor.DARK_AQUA));
        for (Player target : targets) {
            player.sendMessage(Component.text(target.getName() + " ", NamedTextColor.GRAY)
                    .append(Component.text("[SWAP]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/maniac swap " + target.getName()))));
        }
    }

    public void sendMarked(Player player) {
        Map<UUID, MarkManager.MarkCounts> marks = markManager.getAllMarks();
        if (marks.isEmpty()) {
            player.sendMessage(Component.text("No marked players.", NamedTextColor.GRAY));
            return;
        }

        List<Player> onlineMarked = marks.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (onlineMarked.isEmpty()) {
            player.sendMessage(Component.text("No marked players online.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Highlighting marked players...", NamedTextColor.AQUA));
        glowHelper.showMarkedGlow(player, onlineMarked, 60);
    }

    private List<Player> getAlivePlayers(UUID exclude) {
        List<Player> alive = new ArrayList<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(exclude)) {
                continue;
            }
            if (target.getGameMode() == GameMode.SPECTATOR || target.isDead()) {
                continue;
            }
            alive.add(target);
        }
        return alive;
    }
}

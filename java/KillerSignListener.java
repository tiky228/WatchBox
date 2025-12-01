package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
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
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles right-click interactions with the Killer Sign and provides the Maniac ability UI.
 */
public class KillerSignListener implements Listener {
    private final JavaPlugin plugin;
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final NamespacedKey killerSignKey;

    public KillerSignListener(JavaPlugin plugin, RoleManager roleManager, MarkManager markManager, KillerSignItem killerSignItem) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.killerSignKey = new NamespacedKey(plugin, "killer_sign");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isKillerSign(item)) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (!(player.isOp() || roleManager.isManiac(player))) {
            player.sendMessage(Component.text("Only the Maniac can use this sign.", NamedTextColor.RED));
            return;
        }

        sendMainMenu(player);
    }

    private boolean isKillerSign(ItemStack itemStack) {
        if (itemStack == null) {
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
        player.sendMessage(Component.text("[Show marked players]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand("/maniac showmarks")));
        player.sendMessage(Component.text("[Swap places with player]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/maniac swap")));
    }

    public void sendSilenceTargets(Player player) {
        List<Player> targets = getAlivePlayers(player.getUniqueId());
        if (targets.isEmpty()) {
            player.sendMessage(Component.text("No available targets.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Select a target to silence:", NamedTextColor.YELLOW));
        for (Player target : targets) {
            player.sendMessage(Component.text(target.getName() + " ", NamedTextColor.WHITE)
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

        player.sendMessage(Component.text("Swap with:", NamedTextColor.YELLOW));
        for (Player target : targets) {
            player.sendMessage(Component.text(target.getName() + " ", NamedTextColor.WHITE)
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

        player.sendMessage(Component.text("Highlighting marked players...", NamedTextColor.GOLD));
        showMarkedGlowToPlayer(player, onlineMarked);
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

    private void showMarkedGlowToPlayer(Player viewer, Collection<Player> markedPlayers) {
        if (markedPlayers.isEmpty()) {
            return;
        }

        for (Player target : markedPlayers) {
            sendGlowPacket(viewer, target, true);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!viewer.isOnline()) {
                return;
            }
            for (Player target : markedPlayers) {
                sendGlowPacket(viewer, target, false);
            }
        }, 100L);
    }

    private void sendGlowPacket(Player viewer, Player target, boolean glowing) {
        CraftPlayer craftViewer = (CraftPlayer) viewer;
        net.minecraft.world.entity.player.Player targetHandle = ((CraftPlayer) target).getHandle();
        SynchedEntityData data = targetHandle.getEntityData();
        byte flags = data.get(Entity.DATA_SHARED_FLAGS_ID);

        if (glowing) {
            flags |= 0x40;
        } else {
            flags &= ~0x40;
        }

        SynchedEntityData.DataValue<Byte> dataValue = SynchedEntityData.DataValue.create(Entity.DATA_SHARED_FLAGS_ID, flags);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(target.getEntityId(), List.of(dataValue));
        craftViewer.getHandle().connection.send(packet);
    }
}

package com.watchbox.maniac;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MarkTokenManager implements Listener {
    private final JavaPlugin plugin;
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final NamespacedKey tokenKey;
    private final ProtocolManager protocolManager;
    private RoundManager roundManager;

    public MarkTokenManager(JavaPlugin plugin, RoleManager roleManager, MarkManager markManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.tokenKey = new NamespacedKey(plugin, "mark_token");
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerEquipmentListener();
    }

    public void setRoundManager(RoundManager roundManager) {
        this.roundManager = roundManager;
    }

    public ItemStack createToken() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Mark Token", NamedTextColor.AQUA));
        meta.lore(List.of(
                Component.text("Use on a player to add a mark.", NamedTextColor.GRAY),
                Component.text("Hidden from everyone but you.", NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(tokenKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public void giveToken(Player player) {
        if (player == null || !player.isOnline() || !roleManager.isManiac(player)) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (isMarkToken(item)) {
                return;
            }
        }
        inventory.addItem(createToken());
    }

    public void removeTokens(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isMarkToken(contents[i])) {
                inventory.setItem(i, null);
            }
        }
    }

    public void removeTokensFromAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeTokens(player);
        }
    }

    public boolean isMarkToken(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(tokenKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!roleManager.isManiac(player)) {
            return;
        }
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (!isMarkToken(inHand)) {
            return;
        }
        if (player.getLocation().distanceSquared(target.getLocation()) > 9) {
            return;
        }
        if (target.getGameMode() == GameMode.SPECTATOR || target.isDead()) {
            return;
        }
        if (roundManager != null) {
            if (!roundManager.handleMarkTokenUse(player, target)) {
                player.sendMessage(Component.text("You cannot use another mark this round.", NamedTextColor.RED));
            }
            return;
        }

        // Fallback if the round manager is unavailable; behaves like the old implementation.
        markManager.addNormalMark(target, 1);
        player.sendMessage(Component.text("Marked " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void registerEquipmentListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                hideTokenFromViewer(event);
            }
        });
    }

    private void hideTokenFromViewer(PacketEvent event) {
        Entity entity = event.getPacket()
                .getEntityModifier(event.getPlayer().getWorld())
                .readSafely(0);
        if (!(entity instanceof Player holder)) {
            return;
        }
        List<Pair<ItemSlot, ItemStack>> equipment = event.getPacket().getSlotStackPairLists().read(0);
        if (equipment == null || equipment.isEmpty()) {
            return;
        }

        boolean modified = false;
        List<Pair<ItemSlot, ItemStack>> updated = new ArrayList<>(equipment.size());
        for (Pair<ItemSlot, ItemStack> pair : equipment) {
            ItemSlot slot = pair.getFirst();
            ItemStack stack = pair.getSecond();
            if (slot == ItemSlot.MAINHAND && isMarkToken(stack) && !event.getPlayer().getUniqueId().equals(holder.getUniqueId())) {
                updated.add(new Pair<>(slot, new ItemStack(Material.AIR)));
                modified = true;
            } else {
                updated.add(pair);
            }
        }

        if (modified) {
            event.getPacket().getSlotStackPairLists().write(0, updated);
        }
    }
}

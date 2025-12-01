package com.watchbox.maniac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility for creating and identifying Killer Sign items.
 */
public class KillerSignItem {
    private final NamespacedKey key;

    public KillerSignItem(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "killer_sign");
    }

    public ItemStack createItem() {
        ItemStack sign = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = sign.getItemMeta();
        meta.displayName(Component.text("Killer Sign", NamedTextColor.RED));
        meta.lore(java.util.List.of(Component.text("Right-click to use abilities", NamedTextColor.GRAY)));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        sign.setItemMeta(meta);
        return sign;
    }

    public boolean isKillerSign(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}

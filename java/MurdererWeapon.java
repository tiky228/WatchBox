import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Creates and validates the murderer "marking" weapon.
 * The item can be made invisible in hand by applying a custom model in a resource pack.
 */
public class MurdererWeapon {
    private final ItemStack weapon;

    public MurdererWeapon() {
        this.weapon = createWeapon();
    }

    private ItemStack createWeapon() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Murderer's Marker", NamedTextColor.DARK_RED));
        meta.lore(List.of(
                Component.text("Used by murderers to mark targets.", NamedTextColor.GRAY),
                Component.text("Can be made invisible via resource pack (custom model data).", NamedTextColor.DARK_GRAY)
        ));
        // CustomModelData can be pointed to a transparent model in a resource pack.
        meta.setCustomModelData(12345);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getWeapon() {
        return weapon.clone();
    }

    public boolean isWeapon(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getType() != weapon.getType()) return false;
        if (!stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta.hasCustomModelData() && meta.getCustomModelData() == weapon.getItemMeta().getCustomModelData();
    }
}

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * Handles the murderer weapon marking logic.
 *
 * If a murderer hits another player with the special weapon, normal damage is cancelled
 * and the target receives a mark instead.
 */
public class MurdererWeaponListener implements Listener {
    private final Plugin plugin;
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final MurdererWeapon weapon;

    public MurdererWeaponListener(Plugin plugin, RoleManager roleManager, MarkManager markManager, MurdererWeapon weapon) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.weapon = weapon;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return; // Only care about player attackers
        }

        if (!(event.getEntity() instanceof Player victim)) {
            return; // Only mark player victims
        }

        if (roleManager.getRole(attacker.getUniqueId()) != Role.MURDERER) {
            return; // Only murderers can mark with this weapon
        }

        ItemStack inHand = attacker.getInventory().getItemInMainHand();
        if (!isMurdererWeapon(inHand)) {
            return; // Ignore other items
        }

        // Prevent normal damage and apply a mark instead.
        event.setCancelled(true);
        applyMark(victim);

        attacker.sendMessage(Component.text("You marked " + victim.getName() + ".", NamedTextColor.GREEN));
        victim.sendMessage(Component.text("You have been marked!", NamedTextColor.RED));
    }

    private void applyMark(Player victim) {
        markManager.addNormalMark(victim, 1);
        markManager.addMarkedEntity(victim);
    }

    private boolean isMurdererWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        // Prefer the dedicated weapon matcher if available.
        if (weapon != null && weapon.isWeapon(item)) {
            return true;
        }

        ItemStack template = weapon != null ? weapon.getWeapon() : null;
        if (template == null) {
            return false;
        }

        if (item.getType() != template.getType()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        ItemMeta templateMeta = template.getItemMeta();
        if (meta == null || templateMeta == null) {
            return false;
        }

        return meta.hasDisplayName() && templateMeta.hasDisplayName() && meta.displayName().equals(templateMeta.displayName());
    }
}

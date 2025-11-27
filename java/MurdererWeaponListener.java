import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

/**
 * Listens for murderer weapon usage and applies marks to the targeted living entity.
 */
public class MurdererWeaponListener implements Listener {
    private final RoleManager roleManager;
    private final MarkManager markManager;
    private final MurdererWeapon weapon;

    public MurdererWeaponListener(RoleManager roleManager, MarkManager markManager, MurdererWeapon weapon) {
        this.roleManager = roleManager;
        this.markManager = markManager;
        this.weapon = weapon;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!weapon.isWeapon(event.getItem())) {
            return;
        }

        if (roleManager.getRole(player.getUniqueId()) != Role.MURDERER) {
            player.sendMessage(Component.text("Only murderers can use this item to mark targets.", NamedTextColor.RED));
            return;
        }

        Entity target = player.getTargetEntity(5);
        if (!(target instanceof LivingEntity living) || target.equals(player)) {
            player.sendMessage(Component.text("No valid living target found in front of you.", NamedTextColor.GRAY));
            return;
        }

        markManager.mark(living);
        player.sendMessage(Component.text("Marked " + living.getName() + "!", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Clean up marks when entities die so the debug set stays relevant.
        if (markManager.isMarked(event.getEntity())) {
            markManager.unmark(event.getEntity());
        }
    }
}

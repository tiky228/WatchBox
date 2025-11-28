import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Listens for combat hits and attempts to trigger Black Flash when the custom axe is used.
 */
public class BlackFlashListener implements Listener {
    private final BlackFlashManager manager;

    public BlackFlashListener(BlackFlashManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        // Only consider main-hand attacks with the marked axe.
        if (attacker.getInventory().getItemInMainHand() == null || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!manager.isBlackFlashAxe(attacker.getInventory().getItemInMainHand())) {
            return;
        }

        // Attempt the 20% chance to trigger the effect.
        boolean triggered = manager.tryTriggerBlackFlash(attacker, target);
        if (!triggered) {
            return;
        }

        // Optional: add a bit of bonus damage on successful Black Flash.
        event.setDamage(event.getDamage() + 4.0); // small boost to emphasize power.
    }
}

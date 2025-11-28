import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Utility for creating and identifying the Black Flash axe and applying its effects.
 */
public class BlackFlashManager {
    private final NamespacedKey axeKey;
    private final Random random = new Random();

    public BlackFlashManager(JavaPlugin plugin) {
        this.axeKey = new NamespacedKey(plugin, "blackflash-axe");
    }

    /**
     * Create the custom golden axe that can trigger Black Flash.
     */
    public ItemStack createBlackFlashAxe() {
        ItemStack axe = new ItemStack(org.bukkit.Material.GOLDEN_AXE);
        ItemMeta meta = axe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED + "Black Flash");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Unleash red-black cursed energy.");
            lore.add(ChatColor.DARK_RED + "20% chance" + ChatColor.GRAY + " to trigger a devastating strike.");
            lore.add(ChatColor.DARK_GRAY + "(Optional CustomModelData for resource pack)");
            meta.setLore(lore);
            // Mark the item in the persistent data container to identify it later.
            meta.getPersistentDataContainer().set(axeKey, PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            axe.setItemMeta(meta);
        }
        return axe;
    }

    /**
     * Check whether the provided item stack is our Black Flash axe.
     */
    public boolean isBlackFlashAxe(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(axeKey, PersistentDataType.BYTE);
    }

    /**
     * Attempt to trigger Black Flash based on a 1/5 (20%) random chance.
     */
    public boolean tryTriggerBlackFlash(Player attacker, LivingEntity target) {
        // 1 out of 5 hits succeeds when using the marked axe.
        boolean triggered = random.nextInt(5) == 0;
        if (!triggered) {
            return false;
        }

        applyBlackFlashEffects(attacker, target);
        return true;
    }

    /**
     * Apply potion effects, movement penalties, particles, and sounds when Black Flash activates.
     */
    private void applyBlackFlashEffects(Player attacker, LivingEntity target) {
        // Debuffs to the target.
        target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 100, 0)); // nausea for 5s
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1)); // weakness for 5s
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 10)); // heavy slowness ~3s
        target.setFreezeTicks(Math.max(target.getFreezeTicks(), 40)); // brief freeze for ~2s

        // Buffs to the attacker.
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0)); // speed I for 10s
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0)); // regen I for 5s

        // Visual feedback: red and black dust particles around the target.
        spawnParticles(target.getLocation());

        // Message feedback.
        attacker.sendMessage(ChatColor.RED + "§lBLACK FLASH! " + ChatColor.GRAY + "You struck with overwhelming power!");
        if (target instanceof Player playerTarget) {
            playerTarget.sendMessage(ChatColor.DARK_RED + "You were hit by Black Flash!");
        }

        // Audio feedback.
        Location targetLoc = target.getLocation();
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.2f, 1.2f);
        attacker.playSound(attacker.getLocation(), Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1.0f, 1.3f);
    }

    /**
     * Spawn colored particle bursts to emphasize the hit.
     */
    private void spawnParticles(Location center) {
        var world = center.getWorld();
        if (world == null) {
            return;
        }
        // Red and black dust swirls.
        Particle.DustOptions redDust = new Particle.DustOptions(Color.fromRGB(255, 40, 40), 1.2f);
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(15, 15, 15), 1.0f);
        for (int i = 0; i < 30; i++) {
            world.spawnParticle(Particle.DUST, center, 1, 0.5, 0.8, 0.5, 0.01, redDust, true);
            world.spawnParticle(Particle.DUST, center, 1, 0.5, 0.8, 0.5, 0.01, blackDust, true);
        }
        // Add a burst of redstone particles for extra flair.
        world.spawnParticle(Particle.CRIT_MAGIC, center, 40, 0.6, 0.6, 0.6, 0.3);
    }
}

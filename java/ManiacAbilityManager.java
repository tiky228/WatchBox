import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles cooldown tracking for Maniac abilities.
 */
public class ManiacAbilityManager {
    public enum Ability {
        NORMAL_MARK,
        EMPOWERED_MARK,
        SILENCE
    }

    private final Map<UUID, EnumMap<Ability, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(Player player, Ability ability, long cooldownTicks) {
        EnumMap<Ability, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return false;
        }
        Long until = map.get(ability);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            map.remove(ability);
            return false;
        }
        return true;
    }

    public long remainingMs(Player player, Ability ability) {
        EnumMap<Ability, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return 0L;
        }
        Long until = map.get(ability);
        if (until == null) {
            return 0L;
        }
        return Math.max(0L, until - System.currentTimeMillis());
    }

    public void triggerCooldown(Player player, Ability ability, long cooldownTicks) {
        EnumMap<Ability, Long> map = cooldowns.computeIfAbsent(player.getUniqueId(), id -> new EnumMap<>(Ability.class));
        long expires = System.currentTimeMillis() + cooldownTicks * 50L;
        map.put(ability, expires);
    }

    public void clear() {
        cooldowns.clear();
    }
}

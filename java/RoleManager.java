import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores player roles in memory keyed by UUID.
 */
public class RoleManager {
    private final Map<UUID, Role> roles = new HashMap<>();

    public void assignRole(Player player, Role role) {
        roles.put(player.getUniqueId(), role);
    }

    public Role getRole(Player player) {
        return roles.getOrDefault(player.getUniqueId(), Role.CIVILIAN);
    }

    public Role getRole(UUID uuid) {
        return roles.getOrDefault(uuid, Role.CIVILIAN);
    }

    public boolean isManiac(Player player) {
        return getRole(player) == Role.MANIAC;
    }

    public boolean isCivilian(Player player) {
        return getRole(player) == Role.CIVILIAN;
    }

    public void clear() {
        roles.clear();
    }
}

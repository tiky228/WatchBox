import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores player roles in memory keyed by UUID. Defaults to INNOCENT when unknown.
 */
public class RoleManager {
    private final Map<UUID, Role> roles = new HashMap<>();

    public Role getRole(UUID uuid) {
        return roles.getOrDefault(uuid, Role.INNOCENT);
    }

    public void setRole(UUID uuid, Role role) {
        roles.put(uuid, role);
    }
}

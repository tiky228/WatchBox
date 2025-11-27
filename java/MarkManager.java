import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Keeps track of marked entities in memory using their UUIDs.
 */
public class MarkManager {
    private final Set<UUID> marked = new HashSet<>();

    /**
     * Apply a mark to the given entity.
     */
    public void mark(LivingEntity entity) {
        marked.add(entity.getUniqueId());
    }

    /**
     * Remove a mark from the given entity.
     */
    public void unmark(LivingEntity entity) {
        marked.remove(entity.getUniqueId());
    }

    public boolean isMarked(LivingEntity entity) {
        return marked.contains(entity.getUniqueId());
    }

    /**
     * Return all currently loaded living entities that are marked.
     */
    public List<LivingEntity> getMarkedEntities() {
        return Bukkit.getWorlds().stream()
                .flatMap(world -> world.getLivingEntities().stream())
                .filter(entity -> marked.contains(entity.getUniqueId()))
                .collect(Collectors.toList());
    }

    /**
     * Drop marks for entities that are no longer valid.
     */
    public void cleanupMissingEntities() {
        Set<UUID> stillPresent = Bukkit.getWorlds().stream()
                .flatMap(world -> world.getLivingEntities().stream())
                .map(LivingEntity::getUniqueId)
                .collect(Collectors.toSet());
        marked.retainAll(stillPresent);
    }
}

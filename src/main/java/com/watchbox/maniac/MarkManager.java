package com.watchbox.maniac;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks normal and empowered marks on players.
 */
public class MarkManager {
    public static class MarkCounts {
        public int normal;
        public int empowered;
    }

    private final Map<UUID, MarkCounts> marks = new HashMap<>();
    private final List<LivingEntity> markedEntities = new ArrayList<>();

    private MarkCounts getCounts(UUID uuid) {
        return marks.computeIfAbsent(uuid, id -> new MarkCounts());
    }

    public void addNormalMark(Player player, int amount) {
        MarkCounts counts = getCounts(player.getUniqueId());
        counts.normal += amount;
    }

    public void addEmpoweredMark(Player player, int amount) {
        MarkCounts counts = getCounts(player.getUniqueId());
        counts.empowered += amount;
    }

    public void clearNormalMarks(Player player) {
        MarkCounts counts = getCounts(player.getUniqueId());
        counts.normal = 0;
    }

    public void clearAllMarks(Player player) {
        marks.remove(player.getUniqueId());
    }

    public int getNormalMarks(Player player) {
        return getCounts(player.getUniqueId()).normal;
    }

    public int getEmpoweredMarks(Player player) {
        return getCounts(player.getUniqueId()).empowered;
    }

    public int getTotalMarks(Player player) {
        MarkCounts counts = getCounts(player.getUniqueId());
        return counts.normal + counts.empowered;
    }

    public Map<UUID, MarkCounts> getAllMarks() {
        return marks;
    }

    public void clearAll() {
        marks.clear();
    }

    public void addMarkedEntity(LivingEntity entity) {
        if (!markedEntities.contains(entity)) {
            markedEntities.add(entity);
        }
    }

    public void removeMarkedEntity(LivingEntity entity) {
        markedEntities.remove(entity);
    }

    public Collection<LivingEntity> getMarkedEntities() {
        return new ArrayList<>(markedEntities);
    }
}

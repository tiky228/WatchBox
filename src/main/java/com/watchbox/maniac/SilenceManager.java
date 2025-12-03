package com.watchbox.maniac;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks silence durations for players.
 */
public class SilenceManager {
    private final Map<UUID, Long> silencedUntil = new HashMap<>();

    public void silence(Player player, long durationTicks) {
        long expire = System.currentTimeMillis() + durationTicks * 50L;
        silencedUntil.put(player.getUniqueId(), expire);
    }

    public boolean isSilenced(Player player) {
        Long expire = silencedUntil.get(player.getUniqueId());
        if (expire == null) {
            return false;
        }
        if (System.currentTimeMillis() > expire) {
            silencedUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void clear() {
        silencedUntil.clear();
    }
}

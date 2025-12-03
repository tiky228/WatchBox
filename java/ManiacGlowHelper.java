package com.watchbox.maniac;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles per-viewer glowing highlights for marked players.
 */
public class ManiacGlowHelper {
    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, Set<Integer>> viewerTargets = new HashMap<>();
    private final Map<Integer, Integer> glowCounts = new HashMap<>();
    private final Set<Integer> highlightedEntities = new HashSet<>();

    public ManiacGlowHelper(JavaPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerListener();
    }

    public void showMarkedGlow(Player maniac, List<Player> targets, int durationTicks) {
        if (maniac == null || targets.isEmpty()) {
            return;
        }

        Set<Integer> entityIds = new HashSet<>();
        for (Player target : targets) {
            int id = target.getEntityId();
            entityIds.add(id);
            highlightedEntities.add(id);
            glowCounts.merge(id, 1, Integer::sum);
            target.setGlowing(true);
        }
        viewerTargets.put(maniac.getUniqueId(), entityIds);

        Bukkit.getScheduler().runTaskLater(plugin, () -> clearGlow(maniac.getUniqueId(), entityIds), durationTicks);
    }

    private void clearGlow(UUID viewerId, Set<Integer> entityIds) {
        viewerTargets.remove(viewerId);
        for (Integer id : entityIds) {
            glowCounts.computeIfPresent(id, (key, count) -> count > 1 ? count - 1 : null);
            if (!glowCounts.containsKey(id)) {
                highlightedEntities.remove(id);
            }
        }
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (entityIds.contains(player.getEntityId()) && !glowCounts.containsKey(player.getEntityId())) {
                player.setGlowing(false);
            }
        });
    }

    private void registerListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleMetadata(event);
            }
        });
    }

    private void handleMetadata(PacketEvent event) {
        int entityId = event.getPacket().getIntegers().read(0);
        if (!highlightedEntities.contains(entityId)) {
            return;
        }

        Player viewer = event.getPlayer();
        Set<Integer> allowed = viewerTargets.get(viewer.getUniqueId());
        boolean viewerAllowed = allowed != null && allowed.contains(entityId);

        List<WrappedWatchableObject> originalWatchables = event.getPacket().getWatchableCollectionModifier().read(0);
        if (originalWatchables == null) {
            return;
        }
        List<WrappedWatchableObject> watchables = new ArrayList<>(originalWatchables);
        for (WrappedWatchableObject watchable : watchables) {
            if (watchable.getIndex() == 0 && watchable.getValue() instanceof Byte) {
                byte original = (Byte) watchable.getValue();
                byte modified = viewerAllowed ? (byte) (original | 0x40) : (byte) (original & ~0x40);
                watchable.setValue(modified);
            }
        }
        event.getPacket().getWatchableCollectionModifier().write(0, watchables);
    }
}

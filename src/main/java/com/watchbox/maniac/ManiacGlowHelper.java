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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles per-viewer glowing highlights for marked players using ProtocolLib.
 */
public class ManiacGlowHelper {
    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, Set<Integer>> viewerTargets = new HashMap<>();
    private final Set<Integer> glowingEntities = new HashSet<>();

    public ManiacGlowHelper(JavaPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerListener();
    }

    public void showMarkedGlow(Player maniac, Collection<Player> marked, int durationTicks) {
        if (maniac == null || marked.isEmpty()) {
            return;
        }
        Set<Integer> ids = new HashSet<>();
        for (Player target : marked) {
            ids.add(target.getEntityId());
            glowingEntities.add(target.getEntityId());
        }
        viewerTargets.put(maniac.getUniqueId(), ids);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            viewerTargets.remove(maniac.getUniqueId());
            ids.forEach(glowingEntities::remove);
        }, durationTicks);
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
        try {
            int entityId = event.getPacket().getIntegers().read(0);
            if (!glowingEntities.contains(entityId)) {
                return;
            }
            Player viewer = event.getPlayer();
            Set<Integer> allowed = viewerTargets.get(viewer.getUniqueId());
            boolean shouldSeeGlow = allowed != null && allowed.contains(entityId);

            List<WrappedWatchableObject> original = event.getPacket().getWatchableCollectionModifier().read(0);
            if (original == null) {
                return;
            }
            List<WrappedWatchableObject> modified = new ArrayList<>(original.size());
            for (WrappedWatchableObject watchable : original) {
                if (watchable.getIndex() == 0 && watchable.getValue() instanceof Byte current) {
                    byte updated = shouldSeeGlow ? (byte) (current | 0x40) : (byte) (current & ~0x40);
                    modified.add(new WrappedWatchableObject(watchable.getIndex(), updated));
                } else {
                    modified.add(watchable);
                }
            }
            event.getPacket().getWatchableCollectionModifier().write(0, modified);
        } catch (IllegalArgumentException ignored) {
            // ProtocolLib may not understand some metadata types on newer versions; skip safely.
        } catch (Exception ex) {
            plugin.getLogger().fine("Skipped unexpected metadata entry: " + ex.getMessage());
        }
    }
}

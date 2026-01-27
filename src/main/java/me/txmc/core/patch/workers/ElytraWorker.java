package me.txmc.core.patch.workers;

import lombok.RequiredArgsConstructor;
import me.txmc.core.patch.PatchSection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.*;

/**
 * @author MindComplexity
 * @since 2026/01/26
 * This file was created as a part of 8b8tCore
*/
@RequiredArgsConstructor
public class ElytraWorker implements Runnable {
    private final PatchSection main;
    private final ConcurrentHashMap<UUID, Location> positions = new ConcurrentHashMap<>();

    @Override
    public void run() {
        final double MAX_SPEED_DEFAULT = main.config().getDouble("DefaultElytraMaxSpeedBlocksPerSecond", 128.0);
        final double MAX_SPEED_WITH_ENTITIES = main.config().getDouble("TileEntitiesElytraMaxSpeedBlocksPerSecond", 64.0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(main.plugin(), (task) -> {
                if (!player.isOnline()) return;

                UUID uuid = player.getUniqueId();
                if (!player.isGliding()) {
                    positions.remove(uuid);
                    return;
                }

                Location currentLoc = player.getLocation();
                Location from = positions.get(uuid);

                if (from == null || !from.getWorld().equals(currentLoc.getWorld())) {
                    positions.put(uuid, currentLoc.clone());
                    return;
                }

                double speed = calcSpeed(from, currentLoc);
                positions.put(uuid, currentLoc.clone());

                if (speed > MAX_SPEED_WITH_ENTITIES) {
                    if (currentLoc.getChunk().getTileEntities().length > 128) {
                        handleViolation(player, "elytra_tile_entities_too_fast", MAX_SPEED_WITH_ENTITIES);
                        return;
                    }
                }

                if (speed > MAX_SPEED_DEFAULT) {
                    handleViolation(player, "elytra_too_fast", MAX_SPEED_DEFAULT);
                }
            }, null);
        }
    }

    private void handleViolation(Player player, String messageKey, double speed) {
        removeElytra(player); 
        sendPrefixedLocalizedMessage(player, messageKey, speed);
    }

    private double calcSpeed(Location from, Location to) {
        // Pythagoras without the square root is faster, but hypot is fine if called rarely
        return Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
    }
}

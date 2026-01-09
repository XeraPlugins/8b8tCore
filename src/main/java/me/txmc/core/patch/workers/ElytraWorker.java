package me.txmc.core.patch.workers;

import lombok.RequiredArgsConstructor;
import me.txmc.core.patch.PatchSection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.*;

/**
 * @author 254n_m
 * @since 2023/12/26 12:57 AM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class ElytraWorker implements Runnable {
    private final PatchSection main;

    @Override
    public void run() {

        final double MAX_SPEED_DEFAULT = main.config().getDouble("DefaultElytraMaxSpeedBlocksPerSecond", 128.0);
        final double MAX_SPEED_WITH_ENTITIES = main.config().getDouble("TileEntitiesElytraMaxSpeedBlocksPerSecond", 64.0);

        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Bukkit.getRegionScheduler().run(main.plugin(), player.getLocation(), (task) -> {
                    try {
                        if (!player.isGliding()) {
                            main.positions().remove(player.getUniqueId());
                            return;
                        }

                        Location from = main.positions().getOrDefault(player.getUniqueId(), null);
                        if (from == null) {
                            main.positions().put(player.getUniqueId(), player.getLocation());
                            return;
                        }

                        double speed = calcSpeed(from, player.getLocation());
                        main.positions().replace(player.getUniqueId(), player.getLocation());

                        Chunk playerChunk = player.getLocation().getChunk();
                        if (speed > MAX_SPEED_WITH_ENTITIES &&
                                playerChunk.getTileEntities().length > 128) {
                            removeElytra(player);
                            sendPrefixedLocalizedMessage(player, "elytra_tile_entities_too_fast", MAX_SPEED_WITH_ENTITIES);
                        } else if (speed > MAX_SPEED_DEFAULT) {
                            removeElytra(player);
                            sendPrefixedLocalizedMessage(player, "elytra_too_fast", MAX_SPEED_DEFAULT);
                        }
                    } catch (Exception e) {
                        log(Level.WARNING, "Error processing elytra check for " + player.getName() + ": " + e.getMessage());
                    }
                });
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "An error occurred in ElytraWorker: %s", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private double calcSpeed(Location from, Location to) {
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        return Math.hypot(deltaX, deltaZ);
    }
}


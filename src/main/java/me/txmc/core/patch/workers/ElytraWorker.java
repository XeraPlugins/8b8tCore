package me.txmc.core.patch.workers;

import lombok.RequiredArgsConstructor;
import me.txmc.core.patch.PatchSection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Bukkit.getRegionScheduler().execute(main.plugin(), player.getLocation(), () -> {
                    if (!player.isGliding()) {
                        main.positions().remove(player);
                        return;
                    }

                    Location from = main.positions().getOrDefault(player, null);
                    if (from == null) {
                        main.positions().put(player, player.getLocation());
                        return;
                    }

                    double speed = calcSpeed(from, player.getLocation());
                    main.positions().replace(player, player.getLocation());

                    Chunk playerChunk = player.getLocation().getChunk();
                    if (speed > main.config().getDouble("TileEntityElytraMaxSpeed") &&
                            playerChunk.getTileEntities().length > 16) {
                        removeElytra(player);
                        sendPrefixedLocalizedMessage(player, "elytra_school_zone");
                    } else if (speed > main.config().getDouble("ElytraMaxSpeed")) {
                        removeElytra(player);
                        sendPrefixedLocalizedMessage(player, "elytra_too_fast");
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


package me.txmc.core.patch.workers;

import lombok.RequiredArgsConstructor;
import me.txmc.core.patch.PatchSection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import static me.txmc.core.util.GlobalUtils.removeElytra;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

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
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isGliding()) {
                main.positions().remove(player);
                continue;
            }
            Location from = main.positions().getOrDefault(player, null);
            if (from == null) {
                main.positions().put(player, player.getLocation());
                continue;
            }
            double speed = calcSpeed(from, player.getLocation());
            main.positions().replace(player, player.getLocation());
            if (speed > main.config().getDouble("TileEntityElytraMaxSpeed") && player.getLocation().getChunk().getTileEntities().length > 5) {
                removeElytra(player);
                sendPrefixedLocalizedMessage(player, "elytra_school_zone");
            } else if (speed > main.config().getDouble("ElytraMaxSpeed")) {
                removeElytra(player);
                sendPrefixedLocalizedMessage(player, "elytra_too_fast");
            }
        }
    }

    private double calcSpeed(Location from, Location to) {
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        return Math.hypot(deltaX, deltaZ);
    }
}

package me.txmc.core.patch.listeners;

import me.txmc.core.Main;
import me.txmc.core.ViolationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import static me.txmc.core.util.GlobalUtils.removeElytra;

/**
 * @author 254n_m
 * @since 2024/01/26 4:41 PM
 * This file was created as a part of 8b8tCore
 */
public class FallFlyListener extends ViolationManager implements Listener {
    public FallFlyListener(Main main) {
        super(1, main);
    }
    @EventHandler
    public void onGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player) {
            int vls = getVLS(player);
            increment(player);
            if (vls > 10) {
                removeElytra(player);
                remove(player);
            }
        }

    }
}

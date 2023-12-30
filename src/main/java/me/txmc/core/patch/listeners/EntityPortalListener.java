package me.txmc.core.patch.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

/**
 * @author 254n_m
 * @since 2023/12/30 3:55 AM
 * This file was created as a part of 8b8tCore
 */
public class EntityPortalListener implements Listener {
    @EventHandler
    public void onWorldChange(EntityPortalEnterEvent event) {
        System.out.println(event);
    }
}

package me.txmc.core.patch.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author 254n_m
 * @since 2024/02/21 9:33 PM
 * This file was created as a part of 8b8tCore
 */
public class EntitySwitchWorldListener implements Listener {
    private final HashSet<EntityType> blacklist = new HashSet<>() {{
        add(EntityType.BOAT);
        addAll(Arrays.stream(EntityType.values()).filter(e -> e.name().contains("MINECART")).toList());
    }};
    @EventHandler
    public void onEntitySwitchWorld(EntityPortalEvent event) {
        event.setCancelled(blacklist.contains(event.getEntityType()));
    }
}

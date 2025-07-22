package me.txmc.core.patch.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.tablist.util.Utils;
import me.txmc.core.util.GlobalUtils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityPortalEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;

import org.bukkit.Bukkit;

/**
 * @author 254n_m
 * @since 2024/02/21 9:33 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class EntitySwitchWorldListener implements Listener {
    private final Main main;

    private final HashSet<EntityType> blacklist = new HashSet<>() {{
        add(EntityType.ACACIA_BOAT);
        add(EntityType.ACACIA_CHEST_BOAT);
        add(EntityType.BIRCH_BOAT);
        add(EntityType.BIRCH_CHEST_BOAT);
        add(EntityType.CHERRY_BOAT);
        add(EntityType.CHERRY_CHEST_BOAT);
        add(EntityType.DARK_OAK_BOAT);
        add(EntityType.DARK_OAK_CHEST_BOAT);
        add(EntityType.JUNGLE_BOAT);
        add(EntityType.JUNGLE_CHEST_BOAT);
        add(EntityType.MANGROVE_BOAT);
        add(EntityType.MANGROVE_CHEST_BOAT);
        add(EntityType.OAK_BOAT);
        add(EntityType.OAK_CHEST_BOAT);
        add(EntityType.PALE_OAK_BOAT);
        add(EntityType.PALE_OAK_CHEST_BOAT);
        add(EntityType.SPRUCE_BOAT);
        add(EntityType.SPRUCE_CHEST_BOAT);

        add(EntityType.ITEM);
        addAll(Arrays.stream(EntityType.values()).filter(e -> e.name().contains("MINECART")).toList());
    }};
    @EventHandler
    public void onEntitySwitchWorld(EntityPortalEnterEvent event) {
        Entity entity = event.getEntity();
        if (blacklist.contains(entity.getType())) {
            GlobalUtils.log(Level.INFO, "Prevented a %s from going through a portal at %s", event.getEntityType(), GlobalUtils.formatLocation(event.getEntity().getLocation()));
            entity.getScheduler().run(main, (tsk) -> entity.remove(), () ->{});
        }
    }
}

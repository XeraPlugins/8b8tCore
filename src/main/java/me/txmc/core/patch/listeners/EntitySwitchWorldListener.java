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

/**
 * @author 254n_m
 * @since 2024/02/21 9:33 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class EntitySwitchWorldListener implements Listener {
    private final Main main;

    private final HashSet<EntityType> blacklist = new HashSet<>() {{
        // Updated 8/21/25 to support all boat types for 1.21.6 Folia API the previous symbol has been removed. 
        addAll(Arrays.stream(EntityType.values()).filter(e -> e.name().contains("BOAT")).toList());
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

package me.txmc.core.patch.epc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;

public class EntityPortalListener implements Listener {

    @EventHandler
    public void onEntityPortal(EntityPortalEvent e) {
        if (!(e.getEntity() instanceof Player)) e.setCancelled(true);
    }
}

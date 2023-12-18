package me.txmc.core.antiillegal.listeners;

import io.papermc.paper.event.block.BlockPreDispenseEvent;
import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.AntiIllegalMain;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import static me.txmc.core.antiillegal.util.Utils.checkStand;


/**
 * @author 254n_m
 * @since 2023/09/18 10:55 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
@RequiredArgsConstructor
public class MiscListeners implements Listener {
    private final AntiIllegalMain main;

    @EventHandler
    public void onDispenser(BlockPreDispenseEvent event) {
        main.checkFixItem(event.getItemStack(), event);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Entity[] entities = event.getChunk().getEntities();
        for (Entity entity : entities) {
            if (entity instanceof ItemFrame frame) {
                main.checkFixItem(frame.getItem(), null);
            } else if (entity instanceof ArmorStand stand) {
                checkStand(stand, main);
            }
        }
    }
}

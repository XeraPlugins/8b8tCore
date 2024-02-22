package me.txmc.core.patch.listeners;

import me.txmc.core.ViolationManager;
import me.txmc.core.patch.PatchSection;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author 254n_m
 * @since 2024/02/21 10:16 PM
 * This file was created as a part of 8b8tCore
 */
public class Redstone extends ViolationManager implements Listener {
    private final PatchSection main;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public Redstone(PatchSection main) {
        super(1, 300, main.plugin());
        this.main = main;
    }

    @EventHandler
    public void onRedstoneEvent(BlockRedstoneEvent event) {
        process(event);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        process(event);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        process(event);
    }


    private void process(BlockEvent event) {
        ConfigurationSection config = main.config().getConfigurationSection("Redstone");
        Block block = event.getBlock();
        int vls = getVLS(block.getChunk().hashCode());

        increment(block.getChunk().hashCode());
        GlobalUtils.getRegionTps(block.getLocation()).thenAccept(tps -> {
            if (tps < config.getInt("StrictTPS") && vls > config.getInt("StrictMaxVLS")) {
                cancelEvent(event);
                if (shouldBreakBlock()) block.breakNaturally();
            } else {
                if (vls > config.getInt("RegularMaxVLS")) {
                    if (shouldBreakBlock()) event.getBlock().breakNaturally();
                    cancelEvent(event);
                }
            }
        });
    }
    private void cancelEvent(BlockEvent event) {
        if (event instanceof BlockRedstoneEvent) {
            ((BlockRedstoneEvent)event).setNewCurrent(0);
        } else ((Cancellable)event).setCancelled(true);
    }

    private boolean shouldBreakBlock() {
        return random.nextInt(0, 10) == 1;
    }
}

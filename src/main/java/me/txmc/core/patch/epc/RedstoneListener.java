package me.txmc.core.patch.epc;

import me.txmc.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class RedstoneListener implements Listener {
    private final HashMap<Chunk, Integer> chunkUpdateCount = new HashMap<>();
    private final int maxUpdates;

    public RedstoneListener() {
        maxUpdates = Main.getInstance().getConfig().getInt("MaxRedstoneUpdatesPerTick");
        runTask();
    }

    @EventHandler
    public void onRedstoneBlockUpdate(BlockRedstoneEvent e) {
        if (maxUpdates == -1) return;
        Chunk chunk = e.getBlock().getChunk();
        if (chunkUpdateCount.containsKey(chunk)) {
            if (chunkUpdateCount.get(chunk) >= maxUpdates-1) {
                e.setNewCurrent(0);
                e.getBlock().setType(Material.AIR);
            }

            chunkUpdateCount.put(chunk, chunkUpdateCount.get(chunk) + 1);
        } else {
            chunkUpdateCount.put(chunk, 1);
        }

    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (maxUpdates == -1) return;
        Chunk chunk = e.getBlock().getChunk();
        if (chunkUpdateCount.containsKey(chunk)) {
            if (chunkUpdateCount.get(chunk) >= maxUpdates-1) {
                e.setCancelled(true);
                e.getBlock().setType(Material.AIR);
            }

            chunkUpdateCount.put(chunk, chunkUpdateCount.get(chunk) + 1);
        } else {
            chunkUpdateCount.put(chunk, 1);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (maxUpdates == -1) return;
        Chunk chunk = e.getBlock().getChunk();
        if (chunkUpdateCount.containsKey(chunk)) {
            if (chunkUpdateCount.get(chunk) >= maxUpdates-1) {
                e.setCancelled(true);
                e.getBlock().setType(Material.AIR);
            }

            chunkUpdateCount.put(chunk, chunkUpdateCount.get(chunk) + 1);
        } else {
            chunkUpdateCount.put(chunk, 1);
        }
    }

    private void runTask() {
        if (maxUpdates == -1) return;
        Main.getInstance().getExecutorService().scheduleAtFixedRate(() -> {
            for (Chunk chunk : chunkUpdateCount.keySet()) {
                if (chunkUpdateCount.get(chunk) > maxUpdates) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        chunk.unload();
                        chunk.load();
                    });
                }
                chunkUpdateCount.put(chunk, 0);
            }
        }, 0L, 50L, TimeUnit.MILLISECONDS); // CHECK UPDATE COUNT EVERY TICK
    }
}

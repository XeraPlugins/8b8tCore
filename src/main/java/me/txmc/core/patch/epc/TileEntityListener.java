package me.txmc.core.patch.epc;

import me.txmc.core.Main;
import me.txmc.core.util.Cache;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static me.txmc.core.patch.PatchSection.*;

public class TileEntityListener implements Listener {
    private static final Cache<Chunk, Entity[]> entcache = new Cache<>(30);
    private static final Cache<Chunk, BlockState[]> tilecache = new Cache<>(100);
    private static int moblimit = -1;
    private static int tilelimit = -1;
    private static long checkInterval = 0;
    private static final FileConfiguration config = Main.getInstance().getConfig();
    private static final Main plugin = Main.getInstance();

    public TileEntityListener() {
        moblimit = config.getInt("Patch.EntityPerChunk.TotalEntitiesPerChunk");
        tilelimit = config.getInt("Patch.EntityPerChunk.TotalTileEntitiesPerChunk");
        checkInterval = (long) config.getInt("Patch.EntityPerChunk.CheckInterval") * 20*60;
        runTask();
    }

    static int i = 0;

    private static void runTask() {
        plugin.getExecutorService().scheduleAtFixedRate(() -> {
            i++;

            if (i % checkInterval == 0) {
                try {
                    for (World w : Bukkit.getWorlds()) {
                        for (Chunk chk : w.getLoadedChunks()) {
                            Map<EntityType, Integer> counts = new HashMap<>();
                            for (Entity ent : chk.getEntities()) {
                                EntityType type = ent.getType();

                                int count = counts.getOrDefault(ent.getType(), 0);
                                int allowed = getMobMaxHard(type);

                                if (count < allowed) {
                                    counts.put(type, count + 1);
                                    continue;
                                }

                                ent.remove();
                            }
                        }
                    }
                } catch (Exception ignored) {

                }
            }

            tilecache.tick();
            entcache.tick();
        }, 50L, 50L, TimeUnit.MILLISECONDS);
    }

    private static int getMobCount(Chunk chk, EntityType type) {
        int ents = 0;

        if (!entcache.isCached(chk)) {
            entcache.putCached(chk, chk.getEntities());
        }

        for (Entity ent : entcache.getCached(chk)) {
            if (ent == null) continue;
            if (ent.getType() != type) continue;

            ents++;
        }
        return ents;
    }

    private static int getMobCount(Chunk chk) {
        return chk.getEntities().length;
    }

    private static int getTileCount(Chunk chk, Class<?> type) {
        int tls = 0;

        if (!tilecache.isCached(chk)) {
            tilecache.putCached(chk, chk.getTileEntities());
        }

        for (BlockState tle : tilecache.getCached(chk)) {
            if (tle == null) {
                continue;
            }

            if (!tle.getClass().equals(type)) {
                continue;
            }
            tls++;
        }
        return tls;
    }

    private static int getTileCount(Chunk chk) {
        return chk.getTileEntities().length;
    }

    private static boolean isDeniedMob(Entity ent) {
        EntityType etype = ent.getType();

        // Fix issues with destroying player objects.
        if (etype == EntityType.PLAYER) return false;

        Chunk chk = ent.getLocation().getChunk();

        int current = getMobCount(chk, etype);
        int maximum = getMobMaxSoft(ent.getType());

        if (maximum < 0) {
            current = getMobCount(chk);
            maximum = moblimit;
        }

        if (maximum < 0) return false;


        return current >= maximum;

    }

    private static boolean isDeniedTile(Block b) {
        Chunk chk = b.getLocation().getChunk();
        Class<?> type = b.getState().getClass();

        String typename = type.getSimpleName().toLowerCase();

        if (typename.equals("craftblockstate")) return false;

        int current = getTileCount(chk, type);
        int maximum = getTileMax(b.getType());

        if (maximum < 0) {
            current = getTileCount(chk);
            maximum = tilelimit;
        }

        if (maximum < 0) return false;

        return current >= maximum;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleSpawn(VehicleCreateEvent e) {
        Vehicle ent = e.getVehicle();

        e.setCancelled(isDeniedMob(ent));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawn(EntitySpawnEvent e) {
        if (e.isCancelled()) return;

        Entity ent = e.getEntity();

        e.setCancelled(isDeniedMob(ent));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        if (e.isCancelled()) return;

        Block b = e.getBlock();
        e.setCancelled(isDeniedTile(b));
    }
}

package me.txmc.core.patch.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 *        This file was created as a part of 8b8tCore
 */

@RequiredArgsConstructor
public class PhantomPatch implements Listener {
    private final Main plugin;
    private final NamespacedKey aggroKey;

    public PhantomPatch(Main plugin) {
        this.plugin = plugin;
        this.aggroKey = new NamespacedKey(plugin, "phantom_aggroed");
        startSpawner();
    }

    private void startSpawner() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            boolean enabled = plugin.getConfig().getBoolean("Patch.PhantomFixes.Enabled", true);
            boolean blockAll = plugin.getConfig().getBoolean("Patch.PhantomFixes.BlockAll", false);
            if (!enabled || blockAll)
                return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (GeneralDatabase.getInstance().getPreventPhantomSpawn(player.getName()))
                    continue;
                World world = player.getWorld();
                if (world.getEnvironment() != World.Environment.THE_END)
                    continue;

                if (Math.random() > 0.10)
                    continue;

                Location playerLoc = player.getLocation();

                Bukkit.getRegionScheduler().run(plugin, playerLoc, (regionTask) -> {
                    if (!player.isOnline())
                        return;

                    List<org.bukkit.entity.Entity> existing = player.getNearbyEntities(64, 64, 64);
                    long phantomCount = existing.stream().filter(e -> e.getType() == EntityType.PHANTOM).count();
                    if (phantomCount >= 8)
                        return;

                    List<String> validNamed = plugin.getConfig().getStringList("Patch.PhantomFixes.SpawnAboveBlocks");
                    boolean disableScreech = plugin.getConfig()
                            .getBoolean("Patch.PhantomFixes.DisableScreechUntilAttacked", true);

                    Block ground = null;
                    for (int y = 0; y < 64; y++) {
                        int checkY = playerLoc.getBlockY() - y;
                        if (checkY < world.getMinHeight())
                            break;
                        Block b = world.getBlockAt(playerLoc.getBlockX(), checkY, playerLoc.getBlockZ());
                        if (!b.getType().isAir()) {
                            ground = b;
                            break;
                        }
                    }

                    if (ground == null)
                        return;

                    if (!validNamed.isEmpty()) {
                        boolean valid = false;
                        String typeName = ground.getType().name();
                        for (String name : validNamed) {
                            if (typeName.equalsIgnoreCase(name)) {
                                valid = true;
                                break;
                            }
                        }
                        if (!valid)
                            return;
                    }

                    Location spawnLoc = playerLoc.clone().add((Math.random() - 0.5) * 20, 20,
                            (Math.random() - 0.5) * 20);
                    int count = 2 + (int) (Math.random() * 3);
                    for (int i = 0; i < count; i++) {
                        Phantom phantom = (Phantom) world.spawnEntity(spawnLoc, EntityType.PHANTOM,
                                CreatureSpawnEvent.SpawnReason.CUSTOM);
                        if (disableScreech) {
                            phantom.setSilent(true);
                        }
                    }
                });
            }
        }, 600L, 600L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPhantomPreSpawn(PhantomPreSpawnEvent event) {
        if (GeneralDatabase.getInstance().getPreventPhantomSpawn(event.getSpawningEntity().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPhantomSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.PHANTOM)
            return;

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            Player closest = null;
            double distSq = Double.MAX_VALUE;
            for (Player p : event.getLocation().getWorld().getPlayers()) {
                double d = p.getLocation().distanceSquared(event.getLocation());
                if (d < distSq) {
                    distSq = d;
                    closest = p;
                }
            }
            if (closest != null && GeneralDatabase.getInstance().getPreventPhantomSpawn(closest.getName())) {
                event.setCancelled(true);
                return;
            }
        }

        if (!plugin.getConfig().getBoolean("Patch.PhantomFixes.Enabled", true))
            return;
        if (plugin.getConfig().getBoolean("Patch.PhantomFixes.BlockAll", false)) {
            event.setCancelled(true);
            return;
        }

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM)
            return;

        World world = event.getLocation().getWorld();
        if (plugin.getConfig().getBoolean("Patch.PhantomFixes.OnlyInEnd", true)) {
            if (world.getEnvironment() != World.Environment.THE_END) {
                event.setCancelled(true);
                return;
            }
        }

        List<String> blockNames = plugin.getConfig().getStringList("Patch.PhantomFixes.SpawnAboveBlocks");
        if (!blockNames.isEmpty()) {
            Set<Material> validBlocks = blockNames.stream()
                    .map(name -> {
                        try {
                            return Material.valueOf(name.toUpperCase());
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(m -> m != null)
                    .collect(Collectors.toSet());

            if (!validBlocks.isEmpty()) {
                boolean found = false;
                Block base = event.getLocation().getBlock();

                for (int i = 1; i <= 64; i++) {
                    Block below = base.getRelative(0, -i, 0);
                    if (below.getType().isAir())
                        continue;
                    if (validBlocks.contains(below.getType())) {
                        found = true;
                    }
                    break;
                }
                if (!found) {
                    event.setCancelled(true);
                } else {
                    if (plugin.getConfig().getBoolean("Patch.PhantomFixes.DisableScreechUntilAttacked", true)) {
                        event.getEntity().setSilent(true);
                    }
                }
            }
        } else if (plugin.getConfig().getBoolean("Patch.PhantomFixes.DisableScreechUntilAttacked", true)) {
            event.getEntity().setSilent(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntityType() != EntityType.PHANTOM)
            return;
        if (!plugin.getConfig().getBoolean("Patch.PhantomFixes.Enabled", true))
            return;
        if (!plugin.getConfig().getBoolean("Patch.PhantomFixes.OnlyHostileIfAttacked", true))
            return;

        Phantom phantom = (Phantom) event.getEntity();
        if (!phantom.getPersistentDataContainer().has(aggroKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhantomDamage(EntityDamageByEntityEvent event) {
        if (event.getEntityType() != EntityType.PHANTOM)
            return;
        if (!plugin.getConfig().getBoolean("Patch.PhantomFixes.Enabled", true))
            return;

        Player player = null;
        if (event.getDamager() instanceof Player p) {
            player = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj
                && proj.getShooter() instanceof Player p) {
            player = p;
        }

        if (player != null) {
            final Player target = player;
            Phantom damagedPhantom = (Phantom) event.getEntity();

            Bukkit.getRegionScheduler().run(plugin, damagedPhantom.getLocation(), (task) -> {
                List<org.bukkit.entity.Entity> nearby = damagedPhantom.getNearbyEntities(32, 32, 32);
                nearby.add(damagedPhantom);

                boolean disableScreech = plugin.getConfig().getBoolean("Patch.PhantomFixes.DisableScreechUntilAttacked",
                        true);

                for (org.bukkit.entity.Entity entity : nearby) {
                    if (entity instanceof Phantom phantom) {
                        phantom.getPersistentDataContainer().set(aggroKey, PersistentDataType.BYTE, (byte) 1);
                        phantom.setTarget(target);
                        if (disableScreech) {
                            phantom.setSilent(false);
                        }
                    }
                }
            });
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        resetPhantomAggro(event.getEntity());
    }

    @EventHandler
    public void onPlayerLeave(org.bukkit.event.player.PlayerQuitEvent event) {
        resetPhantomAggro(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        resetPhantomAggro(event.getPlayer());
    }

    private void resetPhantomAggro(Player player) {
        Location loc = player.getLocation().clone();
        Bukkit.getRegionScheduler().run(plugin, loc, (task) -> {
            if (!player.isOnline())
                return;

            boolean disableScreech = plugin.getConfig().getBoolean("Patch.PhantomFixes.DisableScreechUntilAttacked",
                    true);


            for (org.bukkit.entity.Entity entity : loc.getWorld().getNearbyEntities(loc, 128, 128, 128)) {
                if (entity instanceof Phantom phantom) {
                    if (player.equals(phantom.getTarget())) {
                        phantom.getPersistentDataContainer().remove(aggroKey);
                        phantom.setTarget(null);
                        if (disableScreech) {
                            phantom.setSilent(true);
                        }
                    }
                }
            }
        });
    }

}

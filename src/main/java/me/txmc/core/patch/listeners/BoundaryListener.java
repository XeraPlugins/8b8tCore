package me.txmc.core.patch.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BoundaryListener - Prevents players from falling below world boundaries and going above nether roof.
 * @author MindComplexity (aka Libalpm)
 * @since 10/2/2025
 */
@RequiredArgsConstructor
public class BoundaryListener implements Listener {
    private final Main plugin;
    private final Map<UUID, Long> lastNetherRoofDamage = new HashMap<>();
    private final Map<UUID, Long> lastBottomTeleport = new HashMap<>();
    private static final long NETHER_ROOF_DAMAGE_COOLDOWN = 1000;
    private static final long BOTTOM_TELEPORT_COOLDOWN = 3000;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("Patch.BoundaryProtection.Enabled", true)) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && 
            event.getFrom().getBlockY() == event.getTo().getBlockY() && 
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        org.bukkit.entity.Player player = event.getPlayer();
        Location to = event.getTo();
        
        if (to == null) return;
        
        World world = to.getWorld();
        if (world == null) return;
        
        double y = to.getY();
        
        if (world.getEnvironment() == World.Environment.NORMAL && (y < -64 || event.getFrom().getY() < -64)) {
            handleBottomBoundary(player, event, world, -59);
        } else if (world.getEnvironment() == World.Environment.NETHER && (y < 0 || event.getFrom().getY() < 0)) {
            handleBottomBoundary(player, event, world, 5);
        }
        
        if (world.getEnvironment() == World.Environment.NETHER && 
            plugin.getConfig().getBoolean("Patch.BoundaryProtection.NetherRoofProtection.Enabled", true) &&
            world.getName().toLowerCase().contains("nether")) {
            
            int netherYLevel = plugin.getConfig().getInt("Patch.BoundaryProtection.NetherRoofProtection.NetherYLevel", 128);
            
            if (event.getFrom().getY() < netherYLevel && event.getTo().getY() >= netherYLevel) {
                handleNetherRoofAttempt(player, event);
            } else if (y >= netherYLevel) {
                handleNetherRoof(player, event);
            }
        }
    }
    
    private void handleBottomBoundary(org.bukkit.entity.Player player, PlayerMoveEvent event, World world, double targetY) {
        if (player.isOp()) return;
        
        long currentTime = System.currentTimeMillis();
        long lastTeleport = lastBottomTeleport.getOrDefault(player.getUniqueId(), 0L);
        
        boolean forceTeleport = (world.getEnvironment() == World.Environment.NORMAL && event.getFrom().getY() < -64) ||
                               (world.getEnvironment() == World.Environment.NETHER && event.getFrom().getY() < 0);
        
        if (currentTime - lastTeleport < BOTTOM_TELEPORT_COOLDOWN && !forceTeleport) {
            event.setCancelled(true);
            return;
        }
        
        if (event.getFrom().getY() >= targetY && !forceTeleport) return;
        
        lastBottomTeleport.put(player.getUniqueId(), currentTime);
        
        Location safeLocation = findSafeLocationAboveBottom(world, event.getTo().getX(), event.getTo().getZ(), targetY);
        safeLocation.add(0, 0.1, 0);
        safeLocation.setYaw(event.getTo().getYaw());
        safeLocation.setPitch(event.getTo().getPitch());
        
        event.setCancelled(true);
        
        player.teleportAsync(safeLocation);
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.setFallDistance(0);
        
        player.getWorld().getChunkAt(safeLocation).load();
        player.teleportAsync(safeLocation.clone().add(0, 0.1, 0));
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.teleportAsync(safeLocation);
                player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                player.setFallDistance(0);
                
                if (player.getLocation().getY() < player.getWorld().getMinHeight()) {
                    player.teleportAsync(safeLocation.clone().add(0, 1, 0));
                    player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                }
            }
        }, 1L);
        
        player.setInvulnerable(true);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) player.setInvulnerable(false);
        }, 20L);
        
        lastBottomTeleport.put(player.getUniqueId(), System.currentTimeMillis() + 1000);
        
        player.sendMessage("§eYou were teleported to safety above the bottom boundary!");
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            lastBottomTeleport.remove(player.getUniqueId());
        }, 100L);
    }
    
    private void handleNetherRoofAttempt(org.bukkit.entity.Player player, PlayerMoveEvent event) {
        if (player.isOp()) return;
        
        event.setCancelled(true);
        player.setVelocity(player.getVelocity().setY(0));
        
        Location safeLocation = null;
        if (plugin.getConfig().getBoolean("Patch.BoundaryProtection.NetherRoofProtection.EnsureSafeTeleport", true)) {
            safeLocation = findSafeLocationBelowBedrock(event.getFrom());
        }
        
        if (safeLocation != null) {
            teleportPlayer(player, safeLocation);
        } else {
            Location fallbackLocation = event.getFrom().clone().subtract(0, 3, 0);
            teleportPlayer(player, fallbackLocation);
        }
        
        player.sendMessage("§cYou cannot go above the nether roof!");
    }
    
    private void handleNetherRoof(org.bukkit.entity.Player player, PlayerMoveEvent event) {
        if (player.isOp()) return;
        
        Location from = event.getFrom();
        int netherYLevel = plugin.getConfig().getInt("Patch.BoundaryProtection.NetherRoofProtection.NetherYLevel", 128);
        
        if (from.getBlockY() >= netherYLevel) {
            if (plugin.getConfig().getBoolean("Patch.BoundaryProtection.NetherRoofProtection.DamagePlayers", true)) {
                long currentTime = System.currentTimeMillis();
                long lastDamage = lastNetherRoofDamage.getOrDefault(player.getUniqueId(), 0L);
                
                if (currentTime - lastDamage < NETHER_ROOF_DAMAGE_COOLDOWN) return;
                
                lastNetherRoofDamage.put(player.getUniqueId(), currentTime);
                
                player.setFlying(false);
                if (player.isGliding()) player.setGliding(false);
                
                if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL) {
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                }
                
                if (player.isInvulnerable()) player.setInvulnerable(false);
                
                event.setCancelled(true);
                
                if (player.isOnline() && !player.isDead()) {
                    player.setHealth(0);
                }
                
                player.sendMessage("§cYou cannot be above the Nether roof!");
                
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    try {
                        lastNetherRoofDamage.remove(player.getUniqueId());
                    } catch (Exception ignored) {}
                }, 200L);
            } else {
                teleportPlayerDownFromNetherRoof(player, event, from);
            }
            return;
        }
        
        if (!plugin.getConfig().getBoolean("Patch.BoundaryProtection.NetherRoofProtection.BlockPlayers", true)) return;
        
        teleportPlayerDownFromNetherRoof(player, event, from);
    }
    
    private void teleportPlayerDownFromNetherRoof(org.bukkit.entity.Player player, PlayerMoveEvent event, Location from) {
        event.setCancelled(true);
        player.setVelocity(player.getVelocity().setY(0));
        
        Location safeLocation = null;
        if (plugin.getConfig().getBoolean("Patch.BoundaryProtection.NetherRoofProtection.EnsureSafeTeleport", true)) {
            safeLocation = findSafeLocationBelowBedrock(from);
        }
        
        if (safeLocation != null) {
            teleportPlayer(player, safeLocation);
        } else {
            Location fallbackLocation = from.clone().subtract(0, 3, 0);
            teleportPlayer(player, fallbackLocation);
        }
        
        player.sendMessage("§cYou cannot go above the nether roof!");
    }
    
    private void teleportPlayer(org.bukkit.entity.Player player, Location location) {
         player.teleportAsync(location);
         
         final Location finalLocation = location;
         plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
             if (player.isOnline() && player.getLocation().getY() >= plugin.getConfig().getInt("Patch.BoundaryProtection.NetherRoofProtection.NetherYLevel", 128)) {
                 player.teleportAsync(finalLocation);
                 player.setVelocity(player.getVelocity().setY(0));
             }
         }, 5L);
     }
    
    /**
     * Finds a safe location above the bottom boundary
     */
    private Location findSafeLocationAboveBottom(World world, double x, double z, double startY) {
        int maxY = world.getMaxHeight();
        int minY = world.getMinHeight();
        
        // Start from the configured Y level and search upward
        for (int y = (int)startY; y < maxY && y < startY + 50; y++) {
            Block block = world.getBlockAt((int)x, y, (int)z);
            Block above = world.getBlockAt((int)x, y + 1, (int)z);
            Block twoAbove = world.getBlockAt((int)x, y + 2, (int)z);
            
            if (isSafeBlock(block) && isSafeBlock(above) && isSafeBlock(twoAbove)) {
                Location location = new Location(world, x + 0.5, y + 1, z + 0.5);
                
                if (plugin.getConfig().getBoolean("Patch.BoundaryProtection.CreateBottomPlatform", true)) {
                    createBottomPlatform(world, (int)x, y - 1, (int)z);
                }
                
                return location;
            }
        }
        
        // Fallback: create a platform at startY
        int y = (int)startY;
        if (y >= minY && y < maxY) {
            createBottomPlatform(world, (int)x, y - 1, (int)z);
            return new Location(world, x + 0.5, y, z + 0.5);
        }
        
        return new Location(world, x, startY, z);
    }
    
    /**
     * Finds a safe location below the bedrock in the nether
     */
    private Location findSafeLocationBelowBedrock(Location from) {
        World world = from.getWorld();
        int netherYLevel = plugin.getConfig().getInt("Patch.BoundaryProtection.NetherRoofProtection.NetherYLevel", 128);
        
        // Find the actual bedrock ceiling
        int bedrockCeiling = netherYLevel;
        for (int y = netherYLevel; y < world.getMaxHeight() && y < netherYLevel + 20; y++) {
            Block block = world.getBlockAt(from.getBlockX(), y, from.getBlockZ());
            if (block.getType() == Material.BEDROCK) {
                bedrockCeiling = y;
                break;
            }
        }
        
        // Search for a safe spot at least 2 blocks below the bedrock ceiling
        for (int y = bedrockCeiling - 2; y > 0 && y > bedrockCeiling - 30; y--) {
            Block block = world.getBlockAt(from.getBlockX(), y, from.getBlockZ());
            Block above = world.getBlockAt(from.getBlockX(), y + 1, from.getBlockZ());
            Block twoAbove = world.getBlockAt(from.getBlockX(), y + 2, from.getBlockZ());
            
            if (isSafeBlock(block) && isSafeBlock(above) && isSafeBlock(twoAbove)) {
                Location location = new Location(world, from.getX(), y + 1, from.getZ());
                
                if (plugin.getConfig().getBoolean("Patch.BoundaryProtection.NetherRoofProtection.CreateNetherPlatform", true)) {
                    createNetherPlatform(world, from.getBlockX(), y, from.getBlockZ());
                }
                
                return location;
            }
        }
        
        return null;
    }
    
    /**
     * Creates a safe platform at the specified location
     */
    private void createBottomPlatform(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) return;
        
        Block center = world.getBlockAt(x, y, z);
        if (center.getType() == Material.AIR || center.getType() == Material.VOID_AIR) {
            center.setType(Material.STONE);
        }
        
        // Create a 3x3 platform
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if (block.getType() == Material.AIR || block.getType() == Material.VOID_AIR) {
                    block.setType(Material.STONE);
                }
            }
        }
    }
    
    /**
     * Creates a safe platform below the nether roof
     */
    private void createNetherPlatform(World world, int x, int y, int z) {
        if (y < 0 || y >= world.getMaxHeight()) return;
        
        Block center = world.getBlockAt(x, y, z);
        if (center.getType() == Material.AIR || center.getType() == Material.VOID_AIR || center.getType() == Material.LAVA) {
            center.setType(Material.NETHERRACK);
        }
        
        // Create a 3x3 platform, protecting end portals
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if ((block.getType() == Material.AIR || block.getType() == Material.VOID_AIR || block.getType() == Material.LAVA) &&
                    block.getType() != Material.END_PORTAL && block.getType() != Material.END_PORTAL_FRAME) {
                    block.setType(Material.NETHERRACK);
                }
            }
        }
    }
    
    /**
     * Checks if a block is safe for player placement
     */
    private boolean isSafeBlock(Block block) {
        Material type = block.getType();
        return type != Material.LAVA && type != Material.WATER && type != Material.FIRE &&
               !type.isSolid() && type != Material.MAGMA_BLOCK;
    }
}

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
        
        if (isPlayerMounted(player)) {
            dismountPlayer(player);            
            player.getScheduler().runDelayed(plugin, (task) -> {
                if (player.isOnline()) {
                    performBottomBoundaryTeleport(player, safeLocation);
                }
            }, null, 3L);
        } else {
            performBottomBoundaryTeleport(player, safeLocation);
        }
        
        player.setInvulnerable(true);
        player.getScheduler().runDelayed(plugin, (task) -> {
            if (player.isOnline()) player.setInvulnerable(false);
        }, null, 20L);
        
        lastBottomTeleport.put(player.getUniqueId(), System.currentTimeMillis() + 1000);
        
        player.sendMessage("§eYou were teleported to safety above the bottom boundary!");
        
        player.getScheduler().runDelayed(plugin, (task) -> {
            lastBottomTeleport.remove(player.getUniqueId());
        }, null, 100L);
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
            Location fallbackLocation = event.getFrom().clone().subtract(0, 10, 0);
            if (fallbackLocation.getY() < 1) {
                fallbackLocation.setY(5);
            }
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
                
                player.getScheduler().runDelayed(plugin, (task) -> {
                    try {
                        lastNetherRoofDamage.remove(player.getUniqueId());
                    } catch (Exception ignored) {}
                }, null, 200L);
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
    
    /**
     * Checks if a player is currently mounted on any entity (boat, horse, etc.)
     */
    private boolean isPlayerMounted(org.bukkit.entity.Player player) {
        return player.getVehicle() != null;
    }
    
    /**
     * Safely dismounts a player from any vehicle they might be riding
     */
    private void dismountPlayer(org.bukkit.entity.Player player) {
        if (isPlayerMounted(player)) {
            org.bukkit.entity.Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                vehicle.eject();
                player.getScheduler().runDelayed(plugin, (task) -> {
                    if (player.isOnline() && isPlayerMounted(player)) {
                        player.leaveVehicle();
                    }
                }, null, 1L);
            }
        }
    }
    
    private void teleportPlayer(org.bukkit.entity.Player player, Location location) {
         if (isPlayerMounted(player)) {
             dismountPlayer(player);
             
             player.getScheduler().runDelayed(plugin, (task) -> {
                 if (player.isOnline()) {
                     performTeleport(player, location);
                 }
             }, null, 3L);
         } else {
             performTeleport(player, location);
         }
     }
     
     /**
      * Performs the actual teleportation after ensuring player is dismounted
      */
     private void performTeleport(org.bukkit.entity.Player player, Location location) {
         player.teleportAsync(location);
         
         final Location finalLocation = location;
         player.getScheduler().runDelayed(plugin, (task) -> {
             if (player.isOnline() && player.getLocation().getY() >= plugin.getConfig().getInt("Patch.BoundaryProtection.NetherRoofProtection.NetherYLevel", 128)) {
                 player.teleportAsync(finalLocation);
                 player.setVelocity(player.getVelocity().setY(0));
             }
         }, null, 5L);
     }
     
     /**
      * Performs the specific teleportation logic for bottom boundary violations
      */
     private void performBottomBoundaryTeleport(org.bukkit.entity.Player player, Location safeLocation) {
         player.teleportAsync(safeLocation);
         player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
         player.setFallDistance(0);
         
         player.getWorld().getChunkAt(safeLocation).load();
         player.teleportAsync(safeLocation.clone().add(0, 0.1, 0));
         
         player.getScheduler().runDelayed(plugin, (task) -> {
             if (player.isOnline()) {
                 player.teleportAsync(safeLocation);
                 player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                 player.setFallDistance(0);
                 
                 if (player.getLocation().getY() < player.getWorld().getMinHeight()) {
                     player.teleportAsync(safeLocation.clone().add(0, 1, 0));
                     player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                 }
             }
         }, null, 1L);
     }
    
    /**
     * Finds a safe location above the bottom boundary
     */
    private Location findSafeLocationAboveBottom(World world, double x, double z, double startY) {
        int maxY = world.getMaxHeight();
        int minY = world.getMinHeight();
        
        for (int y = (int)startY; y < maxY && y < startY + 50; y++) {
            Block ground = world.getBlockAt((int)x, y - 1, (int)z);
            Block body = world.getBlockAt((int)x, y, (int)z);
            Block head = world.getBlockAt((int)x, y + 1, (int)z);
            
            if (isSolidBlock(ground) && isAirSpace(body) && isAirSpace(head)) {
                Location location = new Location(world, x, y, z);                
                if (!isAirSpace(body)) body.setType(Material.AIR);
                if (!isAirSpace(head)) head.setType(Material.AIR);
                
                return location;
            }
        }
        
        // If no safe spot found, create one at startY
        int y = (int)startY;
        if (y >= minY && y < maxY) {
            Block ground = world.getBlockAt((int)x, y - 1, (int)z);
            Block body = world.getBlockAt((int)x, y, (int)z);
            Block head = world.getBlockAt((int)x, y + 1, (int)z);
            
            if (!isSolidBlock(ground)) {
                ground.setType(Material.STONE);
            }
            
            body.setType(Material.AIR);
            head.setType(Material.AIR);
            
            return new Location(world, x, y, z);
        }
        
        return new Location(world, x, startY, z);
    }
    
    /**
     * Finds a safe location below the bedrock in the nether
     */
    private Location findSafeLocationBelowBedrock(Location from) {
        World world = from.getWorld();
        int netherYLevel = plugin.getConfig().getInt("Patch.BoundaryProtection.NetherRoofProtection.NetherYLevel", 128);
        
        int bedrockCeiling = netherYLevel;
        for (int y = netherYLevel; y < world.getMaxHeight() && y < netherYLevel + 20; y++) {
            Block block = world.getBlockAt(from.getBlockX(), y, from.getBlockZ());
            if (block.getType() == Material.BEDROCK) {
                bedrockCeiling = y;
                break;
            }
        }
        
        for (int y = bedrockCeiling - 8; y > 0 && y > bedrockCeiling - 40; y--) {
            Block ground = world.getBlockAt(from.getBlockX(), y - 1, from.getBlockZ());
            Block body = world.getBlockAt(from.getBlockX(), y, from.getBlockZ());
            Block head = world.getBlockAt(from.getBlockX(), y + 1, from.getBlockZ());
            
            if (isSolidBlock(ground) && isAirSpace(body) && isAirSpace(head)) {
                Location location = new Location(world, from.getX(), y, from.getZ());
                
                if (!isAirSpace(body)) body.setType(Material.AIR);
                if (!isAirSpace(head)) head.setType(Material.AIR);

                for (int clearY = y + 2; clearY <= y + 4 && clearY < bedrockCeiling; clearY++) {
                    Block aboveBlock = world.getBlockAt(from.getBlockX(), clearY, from.getBlockZ());
                    if (!isAirSpace(aboveBlock) && aboveBlock.getType() != Material.BEDROCK) {
                        aboveBlock.setType(Material.AIR);
                    }
                }
                
                if (plugin.getConfig().getBoolean("Patch.BoundaryProtection.NetherRoofProtection.CreateNetherPlatform", true)) {
                    createNetherPlatform(world, from.getBlockX(), y - 1, from.getBlockZ());
                }
                
                return location;
            }
        }
        
        // Exception to create safe space 10 blocks below bedrock if no safe spot found
        int forceY = Math.max(5, bedrockCeiling - 10);
        Block ground = world.getBlockAt(from.getBlockX(), forceY - 1, from.getBlockZ());
        Block body = world.getBlockAt(from.getBlockX(), forceY, from.getBlockZ());
        Block head = world.getBlockAt(from.getBlockX(), forceY + 1, from.getBlockZ());
        
        if (!isSolidBlock(ground) && ground.getType() != Material.BEDROCK) {
            ground.setType(Material.NETHERRACK);
        }
        body.setType(Material.AIR);
        head.setType(Material.AIR);
        
        for (int clearY = forceY + 2; clearY <= forceY + 5 && clearY < bedrockCeiling; clearY++) {
            Block aboveBlock = world.getBlockAt(from.getBlockX(), clearY, from.getBlockZ());
            if (aboveBlock.getType() != Material.BEDROCK) {
                aboveBlock.setType(Material.AIR);
            }
        }
        
        if (plugin.getConfig().getBoolean("Patch.BoundaryProtection.NetherRoofProtection.CreateNetherPlatform", true)) {
            createNetherPlatform(world, from.getBlockX(), forceY - 1, from.getBlockZ());
        }
        
        return new Location(world, from.getX(), forceY, from.getZ());
    }
    
    /**
     * Creates a safe platform at the specified location
     */
    private void createBottomPlatform(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) return;
        
        // Create a 3x3 platform
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if (block.getType() == Material.AIR || block.getType() == Material.VOID_AIR || block.getType() == Material.CAVE_AIR) {
                    block.setType(Material.STONE);
                }
            }
        }
        
        // Ensure air space above the platform (2 blocks high)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= 2; dy++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }
    
    /**
     * Creates a safe platform below the nether roof
     */
    private void createNetherPlatform(World world, int x, int y, int z) {
        if (y < 0 || y >= world.getMaxHeight()) return;
        
        // Create a 3x3 platform (excluding end portals)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if ((block.getType() == Material.AIR || block.getType() == Material.VOID_AIR || block.getType() == Material.CAVE_AIR || block.getType() == Material.LAVA) &&
                    block.getType() != Material.END_PORTAL && block.getType() != Material.END_PORTAL_FRAME) {
                    block.setType(Material.NETHERRACK);
                }
            }
        }
        
        // Ensure air space above the platform
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= 4; dy++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }
    
    /**
     * Checks if a block is safe for player placement (solid ground)
     */
    private boolean isSolidBlock(Block block) {
        Material type = block.getType();
        return type.isSolid() && 
               type != Material.LAVA && 
               type != Material.WATER && 
               type != Material.FIRE && 
               type != Material.MAGMA_BLOCK &&
               !type.name().contains("LEAVES");
    }
    
    /**
     * Checks if a block is air or safe for player to occupy
     */
    private boolean isAirSpace(Block block) {
        Material type = block.getType();
        return type == Material.AIR || 
               type == Material.VOID_AIR || 
               type == Material.CAVE_AIR ||
               (!type.isSolid() && 
                type != Material.LAVA && 
                type != Material.WATER && 
                type != Material.FIRE &&
                type != Material.MAGMA_BLOCK);
    }
}

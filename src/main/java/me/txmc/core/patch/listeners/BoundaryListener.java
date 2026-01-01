package me.txmc.core.patch.listeners;

import io.papermc.paper.entity.TeleportFlag;
import me.txmc.core.Main;
import me.txmc.core.Reloadable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BoundaryListener - Prevents players from falling below world boundaries and
 * going above nether roof.
 * 
 * @author MindComplexity (aka Libalpm)
 * @since 10/2/2025
 */
public class BoundaryListener implements Listener, Reloadable {
    private final Main plugin;
    private final Map<UUID, Long> lastNetherRoofDamage = new HashMap<>();
    private final Map<UUID, Long> lastBottomTeleport = new HashMap<>();
    private final Map<UUID, Long> lastBorderMessage = new HashMap<>();
    private static final long NETHER_ROOF_DAMAGE_COOLDOWN = 1000;
    private static final long BOTTOM_TELEPORT_COOLDOWN = 3000;
    private static final long BORDER_MESSAGE_COOLDOWN = 3000;

    private boolean enabled;
    private boolean netherRoofEnabled;
    private int netherYLevel;
    private boolean damagePlayers;
    private boolean blockPlayers;
    private boolean ensureSafeTeleport;
    private boolean createNetherPlatform;
    private boolean worldBorderEnabled;
    private double worldBorderBuffer;

    public BoundaryListener(Main plugin) {
        this.plugin = plugin;
        reloadConfig();
        startNetherRoofMonitor();
        startWorldBorderMonitor();
    }

    @Override
    public void reloadConfig() {
        this.enabled = plugin.getConfig().getBoolean("Patch.BoundaryProtection.Enabled", true);
        this.netherRoofEnabled = plugin.getConfig().getBoolean("Patch.BoundaryProtection.NetherRoofProtection.Enabled",
                true);
        this.netherYLevel = plugin.getConfig().getInt("Patch.BoundaryProtection.NetherRoofProtection.NetherYLevel",
                128);
        this.damagePlayers = plugin.getConfig()
                .getBoolean("Patch.BoundaryProtection.NetherRoofProtection.DamagePlayers", true);
        this.blockPlayers = plugin.getConfig().getBoolean("Patch.BoundaryProtection.NetherRoofProtection.BlockPlayers",
                true);
        this.ensureSafeTeleport = plugin.getConfig()
                .getBoolean("Patch.BoundaryProtection.NetherRoofProtection.EnsureSafeTeleport", true);
        this.createNetherPlatform = plugin.getConfig()
                .getBoolean("Patch.BoundaryProtection.NetherRoofProtection.CreateNetherPlatform", true);
        this.worldBorderEnabled = plugin.getConfig().getBoolean(
                "Patch.BoundaryProtection.WorldBorderProtection.Enabled", true);
        this.worldBorderBuffer = 0.1;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enabled)
            return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null)
            return;

        World world = to.getWorld();
        if (world == null)
            return;

        double y = to.getY();
        World.Environment env = world.getEnvironment();

        if (env == World.Environment.NORMAL && (y < -64 || event.getFrom().getY() < -64)) {
            handleBottomBoundary(player, event, world, -59);
        } else if (env == World.Environment.NETHER && (y < 0 || event.getFrom().getY() < 0)) {
            handleBottomBoundary(player, event, world, 5);
        }

        if (env == World.Environment.NETHER && netherRoofEnabled) {
            if (event.getFrom().getY() < netherYLevel && y >= netherYLevel) {
                handleNetherRoofAttempt(player, event);
            } else if (y >= netherYLevel) {
                handleNetherRoof(player, event);
            }
        }

        if (worldBorderEnabled && !player.isOp()) {
            if (isOutsideWorldBorder(to) || getDistanceOutsideBorder(to) > -worldBorderBuffer) {
                handleWorldBorderViolation(player, event);
                return;
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!enabled || !worldBorderEnabled)
            return;

        Vehicle vehicle = event.getVehicle();
        Location to = event.getTo();

        if (to == null)
            return;

        if (isOutsideWorldBorder(to) || getDistanceOutsideBorder(to) > -worldBorderBuffer) {
            vehicle.setVelocity(new Vector(0, 0, 0));

            for (Entity passenger : vehicle.getPassengers()) {
                if (passenger instanceof Player player) {
                    if (player.isOp())
                        continue;

                    vehicle.eject();

                    Location safeLocation = findSafeLocationInsideBorder(event.getFrom());

                    player.teleportAsync(safeLocation);
                    sendBorderMessage(player, "§cYou cannot go beyond the world border!");
                }
            }

            Location vehicleSafe = findSafeLocationInsideBorder(event.getFrom());
            vehicle.teleportAsync(vehicleSafe);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBoatEnter(VehicleEnterEvent event) {
        if (!enabled || !worldBorderEnabled)
            return;

        if (!(event.getEntered() instanceof Player player))
            return;
        if (player.isOp())
            return;

        if (isOutsideWorldBorder(event.getVehicle().getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§cThis vehicle is outside the world border!");
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnderPearlLaunch(ProjectileLaunchEvent event) {
        if (!enabled || !netherRoofEnabled)
            return;

        if (!(event.getEntity() instanceof EnderPearl pearl))
            return;

        if (!(pearl.getShooter() instanceof Player player))
            return;

        if (player.isOp())
            return;

        pearl.getScheduler().runAtFixedRate(plugin, (task) -> {
            if (pearl.isDead() || !pearl.isValid()) {
                task.cancel();
                return;
            }

            Location pearlLoc = pearl.getLocation();
            World pearlWorld = pearlLoc.getWorld();
            if (pearlWorld == null)
                return;

            // Nether Roof Protection
            if (netherRoofEnabled && pearlWorld.getEnvironment() == World.Environment.NETHER
                    && pearlLoc.getY() >= netherYLevel - 5) {
                pearl.remove();
                player.sendMessage("§cEnder pearls cannot take you above the nether roof!");
                task.cancel();
                return;
            }

            if (worldBorderEnabled) {
                double px = pearlLoc.getX();
                double pz = pearlLoc.getZ();

                if (px > -29000000 && px < 29000000 && pz > -29000000 && pz < 29000000)
                    return;

                Vector velocity = pearl.getVelocity();
                double vx = velocity.getX();
                double vz = velocity.getZ();

                double predX = px + vx;
                double predZ = pz + vz;

                WorldBorder border = pearlWorld.getWorldBorder();
                Location center = border.getCenter();
                double radius = border.getSize() / 2.0;
                double precisionLimit = 29999984.0;
                double effectiveRadius = Math.min(radius, precisionLimit) - worldBorderBuffer;

                double xDistFromCenter = predX - center.getX();
                double zDistFromCenter = predZ - center.getZ();

                boolean bounced = false;

                if (Math.abs(xDistFromCenter) >= effectiveRadius) {
                    double reflectX = -Math.max(Math.abs(vx), 0.5) * Math.signum(xDistFromCenter);
                    velocity.setX(reflectX);
                    pearlLoc.setX(center.getX() + (Math.signum(xDistFromCenter) * (effectiveRadius - 1.0)));
                    bounced = true;
                }

                if (Math.abs(zDistFromCenter) >= effectiveRadius) {
                    double reflectZ = -Math.max(Math.abs(vz), 0.5) * Math.signum(zDistFromCenter);
                    velocity.setZ(reflectZ);
                    pearlLoc.setZ(center.getZ() + (Math.signum(zDistFromCenter) * (effectiveRadius - 1.0)));
                    bounced = true;
                }

                if (bounced) {
                    pearl.teleportAsync(pearlLoc);
                    pearl.setVelocity(velocity);
                    sendBorderMessage(player, "§cAn Ender pearl collided with the world border!");
                }
            }
        }, null, 1L, 1L);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {
        if (!enabled)
            return;

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL)
            return;

        Location to = event.getTo();
        if (to == null || to.getWorld() == null)
            return;

        Player player = event.getPlayer();
        if (player.isOp())
            return;

        if (netherRoofEnabled && to.getWorld().getEnvironment() == World.Environment.NETHER
                && to.getY() >= netherYLevel) {
            event.setCancelled(true);
            player.sendMessage("§cEnder pearls cannot take you above the nether roof!");
            return;
        }

        if (worldBorderEnabled && (isOutsideWorldBorder(to) || getDistanceOutsideBorder(to) > -worldBorderBuffer)) {
            event.setCancelled(true);
            player.sendMessage("§cEnder pearls cannot take you beyond the world border!");
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!enabled)
            return;

        Location to = event.getTo();
        if (to == null || to.getWorld() == null)
            return;

        Player player = event.getPlayer();

        if (worldBorderEnabled && !player.isOp()) {
            if (isOutsideWorldBorder(to) || getDistanceOutsideBorder(to) > -worldBorderBuffer) {
                event.setCancelled(true);
                Location safeLocation = findSafeLocationInsideBorder(event.getFrom());
                player.teleportAsync(safeLocation);
                sendBorderMessage(player, "§cYou cannot teleport beyond the world border!");
                return;
            }
        }

        if (netherRoofEnabled && to.getWorld().getEnvironment() == World.Environment.NETHER
                && to.getY() >= netherYLevel && !player.isOp()) {

            event.setCancelled(true);

            final Location originalLocation = player.getLocation().clone();

            Location safeLocation = findSafeLocationBelowBedrock(to);
            final Location finalSafe = safeLocation != null ? safeLocation : originalLocation;

            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, (task) -> {
                if (!player.isOnline())
                    return;

                player.getScheduler().run(plugin, (playerTask) -> {
                    player.teleportAsync(finalSafe,
                            PlayerTeleportEvent.TeleportCause.PLUGIN,
                            TeleportFlag.EntityState.RETAIN_PASSENGERS,
                            TeleportFlag.EntityState.RETAIN_VEHICLE).thenAccept(success -> {
                                // Verify teleport worked
                                player.getScheduler().runDelayed(plugin, (verifyTask) -> {
                                    if (player.isOnline() && player.getLocation().getY() >= netherYLevel) {
                                        // Force again if still above
                                        player.teleportAsync(finalSafe);
                                        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                                    }
                                }, null, 3L);
                            });
                }, null);
            }, 1L);

            player.sendMessage("§cYou cannot teleport above the nether roof!");
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onPlayerTeleportMonitor(PlayerTeleportEvent event) {
        if (!enabled || !netherRoofEnabled || event.isCancelled())
            return;

        Location to = event.getTo();
        if (to == null || to.getWorld() == null)
            return;

        org.bukkit.entity.Player player = event.getPlayer();

        if (to.getWorld().getEnvironment() == World.Environment.NETHER
                && to.getY() >= netherYLevel && !player.isOp()) {
            player.getScheduler().runDelayed(plugin, (task) -> {
                if (player.isOnline() && player.getLocation().getY() >= netherYLevel) {
                    Location safe = findSafeLocationBelowBedrock(player.getLocation());
                    if (safe != null) {
                        player.teleportAsync(safe);
                    }
                }
            }, null, 2L);
        }
    }

    private void handleBottomBoundary(Player player, PlayerMoveEvent event, World world,
            double targetY) {
        if (player.isOp())
            return;

        long currentTime = System.currentTimeMillis();
        long lastTeleport = lastBottomTeleport.getOrDefault(player.getUniqueId(), 0L);

        boolean forceTeleport = (world.getEnvironment() == World.Environment.NORMAL && event.getFrom().getY() < -64) ||
                (world.getEnvironment() == World.Environment.NETHER && event.getFrom().getY() < 0);

        if (currentTime - lastTeleport < BOTTOM_TELEPORT_COOLDOWN && !forceTeleport) {
            event.setCancelled(true);
            return;
        }

        if (event.getFrom().getY() >= targetY && !forceTeleport)
            return;

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
            if (player.isOnline())
                player.setInvulnerable(false);
        }, null, 20L);

        lastBottomTeleport.put(player.getUniqueId(), System.currentTimeMillis() + 1000);

        player.sendMessage("§eYou were teleported to safety above the bottom boundary!");

        player.getScheduler().runDelayed(plugin, (task) -> {
            lastBottomTeleport.remove(player.getUniqueId());
        }, null, 100L);
    }

    private void handleNetherRoofAttempt(Player player, PlayerMoveEvent event) {
        if (player.isOp())
            return;

        event.setCancelled(true);
        player.setVelocity(player.getVelocity().setY(0));

        Location safeLocation = null;
        if (ensureSafeTeleport) {
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

    private void handleNetherRoof(Player player, PlayerMoveEvent event) {
        if (player.isOp())
            return;

        Location from = event.getFrom();
        if (from.getBlockY() >= netherYLevel) {
            if (damagePlayers) {
                long currentTime = System.currentTimeMillis();
                long lastDamage = lastNetherRoofDamage.getOrDefault(player.getUniqueId(), 0L);

                if (currentTime - lastDamage < NETHER_ROOF_DAMAGE_COOLDOWN)
                    return;

                lastNetherRoofDamage.put(player.getUniqueId(), currentTime);

                player.setFlying(false);
                if (player.isGliding())
                    player.setGliding(false);

                if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL) {
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                }

                if (player.isInvulnerable())
                    player.setInvulnerable(false);

                event.setCancelled(true);

                if (player.isOnline() && !player.isDead()) {
                    player.setHealth(0);
                }

                player.sendMessage("§cYou cannot be above the Nether roof!");

                player.getScheduler().runDelayed(plugin, (task) -> {
                    try {
                        lastNetherRoofDamage.remove(player.getUniqueId());
                    } catch (Exception ignored) {
                    }
                }, null, 200L);
            } else {
                teleportPlayerDownFromNetherRoof(player, event, from);
            }
            return;
        }

        if (!blockPlayers)
            return;
        teleportPlayerDownFromNetherRoof(player, event, from);
    }

    private void teleportPlayerDownFromNetherRoof(Player player, PlayerMoveEvent event,
            Location from) {
        event.setCancelled(true);
        player.setVelocity(player.getVelocity().setY(0));

        Location safeLocation = null;
        if (ensureSafeTeleport) {
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
    private boolean isPlayerMounted(Player player) {
        return player.getVehicle() != null;
    }

    /**
     * Safely dismounts a player from any vehicle they might be riding
     */
    private void dismountPlayer(Player player) {
        if (isPlayerMounted(player)) {
            Entity vehicle = player.getVehicle();
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

    private void teleportPlayer(Player player, Location location) {
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
    private void performTeleport(Player player, Location location) {
        player.teleportAsync(location);

        final Location finalLocation = location;
        player.getScheduler().runDelayed(plugin, (task) -> {
            if (player.isOnline() && player.getLocation().getY() >= netherYLevel) {
                player.teleportAsync(finalLocation);
                player.setVelocity(player.getVelocity().setY(0));
            }
        }, null, 5L);
    }

    /**
     * Performs the specific teleportation logic for bottom boundary violations
     */
    private void performBottomBoundaryTeleport(Player player, Location safeLocation) {
        player.teleportAsync(safeLocation);
        player.setVelocity(new Vector(0, 0, 0));
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
                    player.setVelocity(new Vector(0, 0, 0));
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

        for (int y = (int) startY; y < maxY && y < startY + 50; y++) {
            Block ground = world.getBlockAt((int) x, y - 1, (int) z);
            Block body = world.getBlockAt((int) x, y, (int) z);
            Block head = world.getBlockAt((int) x, y + 1, (int) z);

            if (isSolidBlock(ground) && isAirSpace(body) && isAirSpace(head)) {
                Location location = new Location(world, x, y, z);
                if (!isAirSpace(body))
                    body.setType(Material.AIR);
                if (!isAirSpace(head))
                    head.setType(Material.AIR);

                return location;
            }
        }

        // If no safe spot found, create one at startY
        int y = (int) startY;
        if (y >= minY && y < maxY) {
            Block ground = world.getBlockAt((int) x, y - 1, (int) z);
            Block body = world.getBlockAt((int) x, y, (int) z);
            Block head = world.getBlockAt((int) x, y + 1, (int) z);

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
        int bedrockCeiling = netherYLevel;
        for (int y = netherYLevel + 5; y >= netherYLevel - 10; y--) {
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

                if (!isAirSpace(body))
                    body.setType(Material.AIR);
                if (!isAirSpace(head))
                    head.setType(Material.AIR);

                for (int clearY = y + 2; clearY <= y + 4 && clearY < bedrockCeiling; clearY++) {
                    Block aboveBlock = world.getBlockAt(from.getBlockX(), clearY, from.getBlockZ());
                    if (!isAirSpace(aboveBlock) && aboveBlock.getType() != Material.BEDROCK) {
                        aboveBlock.setType(Material.AIR);
                    }
                }

                if (createNetherPlatform) {
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

        if (createNetherPlatform) {
            createNetherPlatform(world, from.getBlockX(), forceY - 1, from.getBlockZ());
        }

        return new Location(world, from.getX(), forceY, from.getZ());
    }

    /**
     * Creates a safe platform at the specified location
     */
    private void createBottomPlatform(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight())
            return;

        // Create a 3x3 platform
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if (block.getType() == Material.AIR || block.getType() == Material.VOID_AIR
                        || block.getType() == Material.CAVE_AIR) {
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
        if (y < 0 || y >= world.getMaxHeight())
            return;

        // Create a 3x3 platform (excluding end portals)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if ((block.getType() == Material.AIR || block.getType() == Material.VOID_AIR
                        || block.getType() == Material.CAVE_AIR || block.getType() == Material.LAVA) &&
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

    private void handleWorldBorderViolation(Player player, PlayerMoveEvent event) {
        event.setCancelled(true);
        player.setVelocity(new Vector(0, 0, 0));

        if (isPlayerMounted(player)) {
            Entity vehicle = player.getVehicle();
            dismountPlayer(player);
            if (vehicle != null) {
                vehicle.remove();
            }
        }

        Location safeLocation = findSafeLocationInsideBorder(event.getFrom());

        player.teleportAsync(safeLocation);
        player.setVelocity(new Vector(0, 0, 0));

        sendBorderMessage(player, "§cYou cannot go beyond the world border!");
    }

    private Location findSafeLocationInsideBorder(Location from) {
        World world = from.getWorld();
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();

        double maxDist = (border.getSize() / 2.0) - worldBorderBuffer;
        double precisionLimit = 29999984.0 - worldBorderBuffer;
        maxDist = Math.min(maxDist, precisionLimit);

        double x = from.getX();
        double z = from.getZ();
        double relX = x - center.getX();
        double relZ = z - center.getZ();

        relX = Math.max(-maxDist, Math.min(maxDist, relX));
        relZ = Math.max(-maxDist, Math.min(maxDist, relZ));

        x = center.getX() + relX;
        z = center.getZ() + relZ;

        double y = from.getY();
        if (world.getEnvironment() == World.Environment.NETHER) {
            y = Math.min(y, netherYLevel - 2); // Stay at least 2 blocks below roof
            Location checkLoc = new Location(world, x, y, z);
            Location safe = findSafeLocationBelowBedrock(checkLoc);
            if (safe != null) {
                safe.setYaw(from.getYaw());
                safe.setPitch(from.getPitch());
                return safe;
            }
            y = 100;
        } else {
            int safeY = world.getHighestBlockYAt((int) x, (int) z) + 1;
            y = Math.max(safeY, world.getMinHeight() + 5);
        }

        Location safe = new Location(world, x, y, z);
        safe.setYaw(from.getYaw());
        safe.setPitch(from.getPitch());

        return safe;
    }

    private boolean isOutsideWorldBorder(Location location) {
        if (location == null || location.getWorld() == null)
            return false;

        double x = Math.abs(location.getX());
        double z = Math.abs(location.getZ());

        if (x < 29000000 && z < 29000000)
            return false;

        World world = location.getWorld();
        WorldBorder border = world.getWorldBorder();

        if (!border.isInside(location)) {
            return true;
        }

        double precisionLimit = 29999984.0;

        return x >= precisionLimit || z >= precisionLimit;
    }

    private double getDistanceOutsideBorder(Location location) {
        if (location == null || location.getWorld() == null)
            return 0;

        WorldBorder border = location.getWorld().getWorldBorder();
        Location center = border.getCenter();
        double radius = border.getSize() / 2.0;

        double dx = Math.abs(location.getX() - center.getX()) - radius;
        double dz = Math.abs(location.getZ() - center.getZ()) - radius;

        return Math.max(dx, dz);
    }

    public void startWorldBorderMonitor() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.isOp())
                    continue;

                if (isOutsideWorldBorder(player.getLocation())) {
                    player.getScheduler().run(plugin, (playerTask) -> {
                        if (!player.isOnline())
                            return;

                        if (isPlayerMounted(player)) {
                            Entity vehicle = player.getVehicle();
                            player.leaveVehicle();
                            if (vehicle != null)
                                vehicle.remove();
                        }

                        Location safe = findSafeLocationInsideBorder(player.getLocation());
                        player.teleportAsync(safe);
                        player.sendMessage("§cYou were teleported inside the world border!");
                    }, null);
                }
            }
        }, 20L, 10L);
    }

    public void startNetherRoofMonitor() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.isOp())
                    continue;

                Location loc = player.getLocation();
                if (loc.getWorld() == null)
                    continue;

                if (loc.getWorld().getEnvironment() == World.Environment.NETHER
                        && loc.getY() >= netherYLevel) {

                    player.getScheduler().run(plugin, (playerTask) -> {
                        if (!player.isOnline())
                            return;

                        Location safe = findSafeLocationBelowBedrock(player.getLocation());
                        if (safe != null) {
                            player.teleportAsync(safe);
                            player.sendMessage("§cYou cannot be above the nether roof!");
                        }
                    }, null);
                }
            }
        }, 10L, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastNetherRoofDamage.remove(uuid);
        lastBottomTeleport.remove(uuid);
        lastBorderMessage.remove(uuid);
    }

    private void sendBorderMessage(Player player, String message) {
        long now = System.currentTimeMillis();
        long last = lastBorderMessage.getOrDefault(player.getUniqueId(), 0L);
        if (now - last > BORDER_MESSAGE_COOLDOWN) {
            player.sendMessage(message);
            lastBorderMessage.put(player.getUniqueId(), now);
        }
    }
}

package me.txmc.core.home.commands;

import me.txmc.core.Main;
import me.txmc.core.home.HomeManager;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class HotspotCommand implements TabExecutor, Listener {
    private final Map<UUID, Location> hotspotLocations = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();
    private final List<String> hotspotOptions = List.of("create", "delete", "teleport");
    private final Map<UUID, Long> lastCreateTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> creationCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> cooldownTasks = new ConcurrentHashMap<>();

    private static final String PERMISSION = "8b8tcore.command.hotspotcreate";
    private int durationInSeconds = 300;

    private Main main;

    public HotspotCommand(HomeManager main) {
        this.main = main.plugin;
        Bukkit.getPluginManager().registerEvents(this, main.plugin);
        startBossBarTask();
    }

    private void startBossBarTask() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(main, (task) -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateBossBarsForPlayer(player);
            }
        }, 20L, 20L);
    }

    private void updateBossBarsForPlayer(Player player) {
        Location playerLocation = player.getLocation();
        double thresholdSq = 256 * 256;

        for (Map.Entry<UUID, Location> entry : hotspotLocations.entrySet()) {
            UUID hotspotOwnerUUID = entry.getKey();
            Location hotspotLocation = entry.getValue();
            BossBar bossBar = playerBossBars.get(hotspotOwnerUUID);
            if (bossBar == null) continue;

            if (playerLocation.getWorld().equals(hotspotLocation.getWorld()) &&
                    playerLocation.distanceSquared(hotspotLocation) <= thresholdSq) {
                bossBar.addViewer(player);
            } else {
                bossBar.removeViewer(player);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
                if (player.hasPermission(PERMISSION) || player.isOp()) createHotspot(player);
                else sendPrefixedLocalizedMessage(player, "hotspot_no_perms_create");
            } else if (args.length > 0 && args[0].equalsIgnoreCase("delete")) {
                if (player.hasPermission(PERMISSION) || player.isOp()) deleteHotspot(player);
                else sendPrefixedLocalizedMessage(player, "hotspot_no_perms_create");
            } else if (args.length > 0 && args[0].equalsIgnoreCase("teleport")) {
                if (args.length == 2) {
                    String targetPlayerName = args[1];
                    teleportToPlayerHotspot(player, targetPlayerName);
                } else {
                    sendPrefixedLocalizedMessage(player, "hotspot_no_playername");
                }
            } else {
                teleportToNearestHotspot(player);
            }
        } else {
            sender.sendMessage("You must be a player to use this command.");
        }
        return true;
    }

    private void createHotspot(Player player) {

        final long DURATION_MILLISECONDS = durationInSeconds * 1000L;
        final long CREATION_COOLDOWN = 30 * 1000L;

        if (System.currentTimeMillis() - lastCreateTimes.getOrDefault(player.getUniqueId(), 0L) < DURATION_MILLISECONDS) {
            sendPrefixedLocalizedMessage(player, "hotspot_already_created");
            return;
        }

        if (hotspotLocations.containsKey(player.getUniqueId())) {
            sendPrefixedLocalizedMessage(player, "hotspot_already_created");
            return;
        }

        if (creationCooldowns.containsKey(player.getUniqueId())) {
            long lastCreationTime = creationCooldowns.get(player.getUniqueId());
            long timeSinceLastCreation = System.currentTimeMillis() - lastCreationTime;

            if (timeSinceLastCreation < CREATION_COOLDOWN) {
                long secondsLeft = (CREATION_COOLDOWN - timeSinceLastCreation) / 1000;
                sendPrefixedLocalizedMessage(player, "hotspot_creation_cooldown", secondsLeft);
                return;
            }
        }

        Location hotspotLocation = player.getLocation();
        hotspotLocations.put(player.getUniqueId(), hotspotLocation);
        lastCreateTimes.put(player.getUniqueId(), System.currentTimeMillis());
        creationCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        sendPrefixedLocalizedMessage(player, "hotspot_created");

        for (Player p : Bukkit.getOnlinePlayers()) {
            sendPrefixedLocalizedMessage(p, "hotspot_created_to_everyone", player.getName(), player.getName());

            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 2.0f, 0.7f);

            Bukkit.getRegionScheduler().runDelayed(main, player.getLocation(), (task) -> {
                if (player.isOnline()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 2.0f, 1.0f);
                }
            }, 6L);
        }

        String hotspotText = GlobalUtils.convertToMiniMessageFormat("<bold><gradient:#5555FF:#0000AA>" + MiniMessage.miniMessage().serialize(player.displayName()) + "<reset><bold><gradient:#5555FF:#0000AA>'s hotspot - Do <gradient:#FFE259:#FFA751>/hotspot</gradient> to teleport.</gradient>");
        Component hotspotTextComponent = MiniMessage.miniMessage().deserialize(hotspotText);
        BossBar bossBar = BossBar.bossBar(hotspotTextComponent,1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayer.getWorld().equals(hotspotLocation.getWorld()) &&
                    nearbyPlayer.getLocation().distance(hotspotLocation) <= 256) {
                bossBar.addViewer(nearbyPlayer);
            }
        }

        playerBossBars.put(player.getUniqueId(), bossBar);

        handleCooldown(bossBar, durationInSeconds, player);
    }

    private void deleteHotspot(Player player) {
        if (!hotspotLocations.containsKey(player.getUniqueId())) {
            sendPrefixedLocalizedMessage(player, "hotspot_not_active");
            return;
        }

        hotspotLocations.remove(player.getUniqueId());
        lastCreateTimes.remove(player.getUniqueId());

        ScheduledTask task = cooldownTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        
        if (bossBar != null) {
            List<Audience> toRemove = new ArrayList<>();
            bossBar.viewers().forEach(vwer -> {
                toRemove.add((Audience) vwer);
            });
            toRemove.forEach(bossBar::removeViewer);
        }

        if(player.isOnline()) sendPrefixedLocalizedMessage(player, "hotspot_deleted");
    }

    private void handleCooldown(BossBar bossBar, int duration, Player player) {
        ScheduledTask existingTask = cooldownTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        int[] timeLeft = {duration};
        final int viewSwitchTime = 5;
        final boolean[] showingHotspotMessage = {true};
        final String colorRed = "<gradient:#CB2D3E:#EF473A>";
        final String colorGreen = "<gradient:#2DCB62:#53AE2C>";
        final String colorYellow = "<gradient:#FFE259:#FFA751>";

        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(main, (scheduledTask) -> {
            if (timeLeft[0] <= 0 || !player.isOnline()) {
                deleteHotspot(player);
                cooldownTasks.remove(player.getUniqueId());
                return;
            }

            bossBar.progress((float) timeLeft[0] / duration);

            int minutes = timeLeft[0] / 60;
            int seconds = timeLeft[0] % 60;
            String timeRemaining = String.format("%02d:%02d", minutes, seconds);

            if (timeLeft[0] % viewSwitchTime == 0) {
                showingHotspotMessage[0] = !showingHotspotMessage[0];
            }

            if (showingHotspotMessage[0]) {
                String hotspotText = GlobalUtils.convertToMiniMessageFormat("<bold><gradient:#5555FF:#0000AA>" +  MiniMessage.miniMessage().serialize(player.displayName()) + "<reset><bold><gradient:#5555FF:#0000AA>'s hotspot - Do <gradient:#FFE259:#FFA751>/hotspot</gradient> to teleport.</gradient>");
                Component hotspotTextComponent = MiniMessage.miniMessage().deserialize(hotspotText);
                bossBar.name(hotspotTextComponent);
            } else {
                String color = colorGreen;
                if(timeLeft[0] <= 30) {
                    color = colorRed;
                    bossBar.color(BossBar.Color.RED);
                }
                else if (timeLeft[0] <= 60) color = colorYellow;

                String hotspotText = GlobalUtils.convertToMiniMessageFormat( "<bold><gradient:#5555FF:#0000AA>" + MiniMessage.miniMessage().serialize(player.displayName()) + "<reset><bold><gradient:#5555FF:#0000AA>'s hotspot ends in " + color + timeRemaining + "</gradient> minutes.</gradient></bold>");
                Component hotspotTextComponent = MiniMessage.miniMessage().deserialize(hotspotText);
                bossBar.name(hotspotTextComponent);
            }

            timeLeft[0]--;
        }, 1L, 20L);

        cooldownTasks.put(player.getUniqueId(), task);
    }


    private void teleportToPlayerHotspot(Player player, String targetPlayerName) {
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        if (targetPlayer == null || !hotspotLocations.containsKey(targetPlayer.getUniqueId())) {
            sendPrefixedLocalizedMessage(player, "hotspot_not_found_for_player", targetPlayerName);
            return;
        }

        if (GlobalUtils.isTeleportRestricted(player)) {
            int range = GlobalUtils.getTeleportRestrictionRange(player);
            sendPrefixedLocalizedMessage(player, "tpa_too_close", range);
            return;
        }

        Location hotspotLocation = hotspotLocations.get(targetPlayer.getUniqueId());

        Bukkit.getGlobalRegionScheduler().runDelayed(main, (task) -> {
            if (player.isOnline() && targetPlayer.isOnline()) {
                player.teleportAsync(hotspotLocation);
                sendPrefixedLocalizedMessage(player, "hotspot_teleported_to_player", targetPlayerName);
            }
        }, 1L);
    }

    private void teleportToNearestHotspot(Player player) {
        Location playerLocation = player.getLocation();
        double closestDistance = Double.MAX_VALUE;
        Location closestHotspot = null;

        for (Location hotspotLocation : hotspotLocations.values()) {
            if (hotspotLocation.getWorld().getName().equals(playerLocation.getWorld().getName())) {
                double distance = playerLocation.distance(hotspotLocation);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestHotspot = hotspotLocation;
                }
            }
        }

        if (closestHotspot == null && !hotspotLocations.isEmpty()) {
            UUID mostRecentPlayerUUID = null;
            long mostRecentTime = 0;
            
            for (Map.Entry<UUID, Location> entry : hotspotLocations.entrySet()) {
                UUID hotspotOwnerUUID = entry.getKey();
                Long creationTime = lastCreateTimes.get(hotspotOwnerUUID);
                if (creationTime != null && creationTime > mostRecentTime) {
                    mostRecentTime = creationTime;
                    mostRecentPlayerUUID = hotspotOwnerUUID;
                }
            }
            
            if (mostRecentPlayerUUID != null) {
                closestHotspot = hotspotLocations.get(mostRecentPlayerUUID);
            }
        }

        if (closestHotspot == null) {
            sendPrefixedLocalizedMessage(player, "hotspot_not_found");
        } else {
            if (GlobalUtils.isTeleportRestricted(player)) {
                int range = GlobalUtils.getTeleportRestrictionRange(player);
                sendPrefixedLocalizedMessage(player, "tpa_too_close", range);
                return;
            }
            Location finalClosestHotspot = closestHotspot;
            Bukkit.getGlobalRegionScheduler().runDelayed(main, (task) -> {
                if (player.isOnline()) {
                    player.teleportAsync(finalClosestHotspot);
                    sendPrefixedLocalizedMessage(player, "hotspot_teleported_to");
                }
            }, 1L);
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();        
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeViewer(player);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return hotspotOptions.stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && "teleport".startsWith(args[0].toLowerCase())) {
            return playerBossBars.keySet().stream()
                    .map(Bukkit::getPlayer)
                    .filter(Objects::nonNull)
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}

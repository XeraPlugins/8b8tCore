package me.txmc.core.home.commands;

import me.txmc.core.home.HomeManager;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class HotspotCommand implements TabExecutor, Listener {
    private final Map<Player, Location> hotspotLocations = new HashMap<>();
    private final Map<Player, BossBar> playerBossBars = new HashMap<>();
    private final List<String> hotspotOptions = List.of("create", "teleport");
    private final Map<UUID, Long> lastCreateTimes = new HashMap<>();
    private static final String PERMISSION = "8b8tcore.command.hotspotcreate";
    private int durationInSeconds = 300;

    public HotspotCommand(HomeManager main) {
        Bukkit.getPluginManager().registerEvents(this, main.plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
                if (player.hasPermission(PERMISSION) || player.isOp()) createHotspot(player);
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

        final long CREATE_COOLDOWN = durationInSeconds * 1000L;
        if (System.currentTimeMillis() - lastCreateTimes.getOrDefault(player.getUniqueId(), 0L) < CREATE_COOLDOWN) {
            sendPrefixedLocalizedMessage(player, "hotspot_already_created");
            return;
        }

        if (hotspotLocations.containsKey(player)) {
            sendPrefixedLocalizedMessage(player, "hotspot_already_created");
            return;
        }

        Location hotspotLocation = player.getLocation();
        hotspotLocations.put(player, hotspotLocation);
        lastCreateTimes.put(player.getUniqueId(), System.currentTimeMillis());

        sendPrefixedLocalizedMessage(player, "hotspot_created");

        String hotspotText = GlobalUtils.convertToMiniMessageFormat("<bold><gradient:#5555FF:#0000AA>" + player.getName() + "'s hotspot - Do <gradient:#FFE259:#FFA751>/hotspot</gradient> to teleport.</gradient>");
        Component hotspotTextComponent = MiniMessage.miniMessage().deserialize(hotspotText);
        BossBar bossBar = BossBar.bossBar(hotspotTextComponent,1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayer.getWorld().equals(hotspotLocation.getWorld()) &&
                    nearbyPlayer.getLocation().distance(hotspotLocation) <= 128) {
                bossBar.addViewer(nearbyPlayer);
            }
        }

        playerBossBars.put(player, bossBar);

        handleCooldown(bossBar, durationInSeconds, player);
    }

    private void handleCooldown(BossBar bossBar, int duration, Player player) {
        new Timer().schedule(new TimerTask() {
            private int timeLeft = duration;
            private final int viewSwitchTime = 5;
            private boolean showingHotspotMessage = true;
            private final String colorRed = "<gradient:#CB2D3E:#EF473A>";
            private final String colorGreen = "<gradient:#2DCB62:#53AE2C>";
            private final String colorYellow = "<gradient:#FFE259:#FFA751>";

            @Override
            public void run() {
                if (timeLeft <= 0 || !player.isOnline()) {
                    List<Audience> toRemove = new ArrayList<>();
                    bossBar.viewers().forEach(vwer -> {
                        toRemove.add((Audience) vwer);
                    });

                    toRemove.forEach(bossBar::removeViewer);

                    for (Map.Entry<Player, BossBar> entry : playerBossBars.entrySet()) {
                        if (entry.getValue().equals(bossBar)) {
                            Player associatedPlayer = entry.getKey();
                            playerBossBars.remove(associatedPlayer);
                            hotspotLocations.remove(associatedPlayer);
                            break;
                        }
                    }
                    cancel();
                    return;
                }

                bossBar.progress((float) timeLeft / duration);

                int minutes = timeLeft / 60;
                int seconds = timeLeft % 60;
                String timeRemaining = String.format("%02d:%02d", minutes, seconds);

                if (timeLeft % viewSwitchTime == 0) {
                    showingHotspotMessage = !showingHotspotMessage;
                }

                if (showingHotspotMessage) {
                    String hotspotText = GlobalUtils.convertToMiniMessageFormat("<bold><gradient:#5555FF:#0000AA>" + player.getName() + "'s hotspot - Do <gradient:#FFE259:#FFA751>/hotspot</gradient> to teleport.</gradient>");
                    Component hotspotTextComponent = MiniMessage.miniMessage().deserialize(hotspotText);
                    bossBar.name(hotspotTextComponent);
                } else {
                    String color = colorGreen;
                    if(timeLeft <= 30) {
                        color = colorRed;
                        bossBar.color(BossBar.Color.RED);
                    }
                    else if (timeLeft <= 60) color = colorYellow;

                    String hotspotText = GlobalUtils.convertToMiniMessageFormat( "<bold><gradient:#5555FF:#0000AA>" + player.getName() + "'s hotspot ends in " + color + timeRemaining + "</gradient> minutes.</gradient></bold>");
                    Component hotspotTextComponent = MiniMessage.miniMessage().deserialize(hotspotText);
                    bossBar.name(hotspotTextComponent);
                }

                timeLeft--;
            }
        }, 0, 1000);
    }


    private void teleportToPlayerHotspot(Player player, String targetPlayerName) {
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        if (targetPlayer == null || !hotspotLocations.containsKey(targetPlayer)) {
            sendPrefixedLocalizedMessage(player, "hotspot_not_found_for_player", targetPlayerName);
            return;
        }

        Location hotspotLocation = hotspotLocations.get(targetPlayer);

        player.teleportAsync(hotspotLocation);
        sendPrefixedLocalizedMessage(player, "hotspot_teleported_to_player", targetPlayerName);
    }

    private void teleportToNearestHotspot(Player player) {
        Location playerLocation = player.getLocation();
        double closestDistance = Double.MAX_VALUE;
        Location closestHotspot = null;

        for (Location hotspotLocation : hotspotLocations.values()) {
            if (hotspotLocation.getWorld().equals(playerLocation.getWorld())) {
                double distance = playerLocation.distance(hotspotLocation);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestHotspot = hotspotLocation;
                }
            }
        }

        if (closestHotspot == null) {
            sendPrefixedLocalizedMessage(player, "hotspot_not_found");
        } else {
            player.teleportAsync(closestHotspot);
            sendPrefixedLocalizedMessage(player, "hotspot_teleported_to");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location playerLocation = player.getLocation();

        for (Map.Entry<Player, Location> entry : hotspotLocations.entrySet()) {
            Player hotspotOwner = entry.getKey();
            Location hotspotLocation = entry.getValue();

            if (playerLocation.getWorld().equals(hotspotLocation.getWorld()) &&
                    playerLocation.distance(hotspotLocation) <= 128) {
                BossBar bossBar = playerBossBars.get(hotspotOwner);
                if (bossBar != null) {
                    bossBar.addViewer(player);
                }
            } else {
                BossBar bossBar = playerBossBars.get(hotspotOwner);
                if (bossBar != null) {
                    bossBar.removeViewer(player);
                }
            }
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
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}

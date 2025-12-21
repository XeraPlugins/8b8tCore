package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseTabCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendMessage;

/**
 * LastSeen command that shows when a player was last online
 * Code ported from the last seen plugin.
 * @author Libalpm (MindComplexity)
 * @since 10/2/2025
 */

public class LastSeenCommand extends BaseTabCommand implements Listener {
    
    private final Main plugin;
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final ConcurrentHashMap<String, PlayerLastSeenData> lastSeenCache = new ConcurrentHashMap<>();
    
    public LastSeenCommand(Main plugin) {
        super(
                "lastseen",
                "/lastseen <player>",
                new String[]{"8b8tcore.command.lastseen"},
                "Check when a player was last online",
                new String[]{"<player>::Player to check last seen time"}
        );
        this.plugin = plugin;
        
        // Register the listener for player quit events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Initialize with existing player data
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateLastSeen(player);
        }
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, "&cUsage: /lastseen <player>");
            return;
        }
        
        String targetName = args[0];
        
        Player onlinePlayer = Bukkit.getPlayer(targetName);
        if (onlinePlayer != null) {
            sendMessage(sender, String.format("&e%s &ais currently online!", onlinePlayer.getName()));
            return;
        }
        
        // Get offline player data
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            
            if (offlinePlayer.getFirstPlayed() == 0L) {
                sendMessage(sender, String.format("&cPlayer '&e%s&c' has never joined the server!", targetName));
                return;
            }
            
            // Check if player is in cache
            String playerId = offlinePlayer.getUniqueId().toString();
            PlayerLastSeenData cachedData = lastSeenCache.get(playerId);
            
            if (cachedData != null) {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                String formattedDate = sdf.format(new Date(cachedData.lastSeen));
                sendMessage(sender, String.format("&e%s &6was last seen on &b%s&6 in world '&e%s&6'", 
                    cachedData.name, formattedDate, cachedData.world));
                
                if (sender.hasPermission("8b8tcore.command.lastseen.location")) {
                    sendMessage(sender, String.format("&6Last location: [&e%.1f&6, &e%.1f&6, &e%.1f&6]", 
                        cachedData.x, cachedData.y, cachedData.z));
                }
            } else {
                long lastPlayed = offlinePlayer.getLastSeen();
                if (lastPlayed > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                    String formattedDate = sdf.format(new Date(lastPlayed));
                    sendMessage(sender, String.format("&e%s &6was last seen on &b%s", 
                        offlinePlayer.getName(), formattedDate));
                } else {
                    sendMessage(sender, String.format("&cNo last seen data available for '&e%s&c'", targetName));
                }
            }
        });
    }
    
    @Override
    public List<String> onTab(String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
            
            suggestions.addAll(lastSeenCache.values().stream()
                    .map(data -> data.name)
                    .collect(Collectors.toList()));
            
            return suggestions.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    /**
     * Update last seen data for a player when they quit
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        updateLastSeen(event.getPlayer());
    }
    
    /**
     * Update last seen data for a player
     */
    public static void updateLastSeen(Player player) {
        PlayerLastSeenData data = new PlayerLastSeenData(
            player.getUniqueId().toString(),
            player.getName(),
            System.currentTimeMillis(),
            player.getLocation().getX(),
            player.getLocation().getY(),
            player.getLocation().getZ(),
            player.getLocation().getWorld().getName()
        );
        lastSeenCache.put(player.getUniqueId().toString(), data);
    }
    
    /**
     * Data class for storing player last seen information
     */
    public static class PlayerLastSeenData {
        public final String uuid;
        public final String name;
        public final long lastSeen;
        public final double x;
        public final double y;
        public final double z;
        public final String world;
        
        public PlayerLastSeenData(String uuid, String name, long lastSeen, double x, double y, double z, String world) {
            this.uuid = uuid;
            this.name = name;
            this.lastSeen = lastSeen;
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
        }
    }
}

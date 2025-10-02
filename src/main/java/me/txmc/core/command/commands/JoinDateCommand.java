package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseTabCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendMessage;

/**
 * JoinDate command that shows when a player first joined the server
 * Code ported from Joindate Plugin
 * @author Libalpm (MindComplexity)
 * @since 10/2/2025
 */
public class JoinDateCommand extends BaseTabCommand {
    
    private final Main plugin;
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    public JoinDateCommand(Main plugin) {
        super(
                "joindate",
                "/joindate [player|reload]",
                new String[]{"8b8tcore.command.joindate", "8b8tcore.command.joindate.others", "8b8tcore.command.joindate.reload"},
                "Check when a player first joined the server",
                new String[]{"<player>::Check another player's join date", "reload::Reload the join date configuration"}
        );
        this.plugin = plugin;
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "&cYou must specify a player name when using this command from console!");
                return;
            }
            showJoinDate(sender, player);
            return;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!hasPermission(sender, "8b8tcore.command.joindate.reload")) {
                sendNoPermission(sender);
                return;
            }
            plugin.reloadConfig();
            sendMessage(sender, "&aJoin date configuration reloaded successfully!");
            return;
        }
        
        if (!hasPermission(sender, "8b8tcore.command.joindate.others")) {
            sendNoPermission(sender);
            return;
        }
        
        String targetName = args[0];
        
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            showJoinDate(sender, target);
            return;
        }
        
        // Get offline player asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (offlinePlayer.getFirstPlayed() == 0L) {
                sendMessage(sender, "&cPlayer '&e" + targetName + "&c' has never joined the server!");
                return;
            }
            showJoinDate(sender, offlinePlayer);
        });
    }
    
    private void showJoinDate(CommandSender sender, Object player) {
        Date joinDate = null;
        String playerName = null;
        
        if (player instanceof Player onlinePlayer) {
            joinDate = new Date(onlinePlayer.getFirstPlayed());
            playerName = onlinePlayer.getName();
        } else if (player instanceof OfflinePlayer offlinePlayer) {
            joinDate = new Date(offlinePlayer.getFirstPlayed());
            playerName = offlinePlayer.getName();
        }
        
        if (joinDate == null) {
            sendMessage(sender, "&cUnable to retrieve join date!");
            return;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String formattedDate = sdf.format(joinDate);
        
        if (sender.equals(player)) {
            sendMessage(sender, "&6You first joined on: &e" + formattedDate);
        } else {
            sendMessage(sender, "&6" + playerName + " &efirst joined on: &b" + formattedDate);
        }
    }
    
    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.isOp() || sender.hasPermission("*");
    }
    
    @Override
    public List<String> onTab(String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("reload");
            
            // Add online player names
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            suggestions.addAll(playerNames);
            
            return suggestions.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}

package me.txmc.core.customexperience;

import me.txmc.core.customexperience.util.PrefixManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Manages and sets the player's prefix in the player list when they join the server.
 *
 * <p>This class is part of the 8b8tCore plugin, which provides custom functionalities
 * including player prefixes based on their permissions.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Assigning the appropriate prefix to a player based on their permissions when they join the server</li>
 *     <li>Updating the player's display name in the player list with the assigned prefix</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/11 03:42 PM
 */
public class PlayerPrefix implements Listener {
    private final JavaPlugin plugin;
    private final PrefixManager prefixManager = new PrefixManager();

    public PlayerPrefix(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String tag = prefixManager.getPrefix(event.getPlayer());
        setupTag(event.getPlayer(), tag);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        String tag = prefixManager.getPrefix(event.getPlayer());
        setupTag(event.getPlayer(), tag);
    }

    @EventHandler
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        String tag = prefixManager.getPrefix(event.getPlayer());
        setupTag(event.getPlayer(), tag);
    }

    public void setupTag(Player player, String tag) {
        player.setPlayerListName(String.format("%s%s",ChatColor.translateAlternateColorCodes('&', tag), player.getDisplayName()));
    }

}


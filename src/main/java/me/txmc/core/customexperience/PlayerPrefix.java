package me.txmc.core.customexperience;

import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import static me.txmc.core.util.GlobalUtils.executeCommand;

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
    private final PrefixManager prefixManager = new PrefixManager();
    private final GeneralDatabase database;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PlayerPrefix(JavaPlugin plugin) {
        this.database = new GeneralDatabase(plugin.getDataFolder().getAbsolutePath());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
//        Player player = event.getPlayer();
//        if (player.getName().startsWith(".")) {
//            executeCommand("lp user " + player.getName() + " permissit ition set sierra.bypass true");
//        }
        String tag = prefixManager.getPrefix(event.getPlayer());
        setupTag(event.getPlayer(), tag);

        String displayName = database.getNickname(event.getPlayer().getName());
        if (displayName != null && !displayName.isEmpty()) {
            Component component = miniMessage.deserialize(GlobalUtils.convertToMiniMessageFormat(displayName));
            event.getPlayer().displayName(component);
        }
    }

    public void setupTag(Player player, String tag) {
        player.playerListName(miniMessage.deserialize(String.format("%s%s",tag, player.getDisplayName())));
    }

}


package me.txmc.core.chat.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.log;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Listener to manage operator permissions on the server.
 * Only players listed in the whitelist are allowed to have operator status.
 *
 * <p>This listener handles various events to ensure that unauthorized
 * operators have their permissions revoked and receive notification.</p>
 *
 * <p>Events handled include:</p>
 * <ul>
 *     <li>Player join events</li>
 *     <li>Command execution events</li>
 *     <li>Player interaction events</li>
 *     <li>Player world change events</li>
 *     <li>Inventory creative mode events</li>
 *     <li>Player game mode change events</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/07/17 11:11 AM
 */

public class OpWhiteListListener implements Listener {
    private final JavaPlugin plugin;

    public OpWhiteListListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkOps(event.getPlayer());
    }

    @EventHandler
    public void onCommandEvent(PlayerCommandPreprocessEvent event) {
        if(checkOps(event.getPlayer())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractionEvent(PlayerInteractEvent event) {
        if(checkOps(event.getPlayer())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        checkOps(event.getPlayer());
    }

    @EventHandler
    public void onInventoryCreative(InventoryCreativeEvent event) {
        Player player = (Player) event.getWhoClicked();
        if(checkOps(player)){
            event.setCancelled(true);
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if(checkOps(player)){
            if(event.getNewGameMode() == GameMode.CREATIVE){
                event.setCancelled(true);
            }
        }
    }

    public boolean checkOps(Player player) {
        List<String> opUsersWhiteList = plugin.getConfig().getStringList("OpUsersWhiteList");

        if ((player.isOp() || player.getGameMode() == GameMode.CREATIVE || player.hasPermission("*")) && !opUsersWhiteList.contains(player.getName())) {
            player.setOp(false);
            if (player.getGameMode() == GameMode.CREATIVE) player.setGameMode(GameMode.SURVIVAL);
            sendPrefixedLocalizedMessage(player, "op_not_allowed");
            log(Level.SEVERE, "The player %s has had operator permissions revoked because it is not allowed.", player.getName());
            return true;
        }
        return false;
    }
}

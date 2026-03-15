package me.txmc.core.chat.listeners;

import me.txmc.core.Reloadable;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.log;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Listener to manage operator permissions on the server.
 * This listener is apart of the 8b8tCore plugin.
 */
public class OpWhiteListListener implements Listener, Reloadable {
    private final JavaPlugin plugin;
    private volatile List<String> opUsersWhiteList;
    private final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();

    public OpWhiteListListener(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    @Override
    public void reloadConfig() {
        this.opUsersWhiteList = List.copyOf(plugin.getConfig().getStringList("OpUsersWhiteList"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkOps(event.getPlayer());
    }

    @EventHandler
    public void onCommandEvent(PlayerCommandPreprocessEvent event) {
        if (checkOps(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractionEvent(PlayerInteractEvent event) {
        if (checkOps(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        checkOps(event.getPlayer());
    }

    @EventHandler
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (checkOps(player)) {
            event.setCancelled(true);
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (checkOps(event.getPlayer()) && event.getNewGameMode() == GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    private boolean checkOps(Player player) {
        UUID playerId = player.getUniqueId();
        if (!processingPlayers.add(playerId)) return false;
        
        try {
            if ((player.isOp() || player.getGameMode() == GameMode.CREATIVE || player.hasPermission("*")) 
                    && !opUsersWhiteList.contains(player.getName())) {
                player.setOp(false);
                if (player.getGameMode() == GameMode.CREATIVE) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                sendPrefixedLocalizedMessage(player, "op_not_allowed");
                log(Level.SEVERE, "Player %s had operator permissions revoked.", player.getName());
                return true;
            }
            return false;
        } finally {
            processingPlayers.remove(playerId);
        }
    }
}

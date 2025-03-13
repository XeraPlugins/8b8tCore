package me.txmc.core.patch.listeners;

import me.txmc.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class VanishVerifier implements Listener {

    private final Main main;

    public VanishVerifier(Main main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (main.getVanishedPlayers().contains(player.getUniqueId())) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.hasPermission("8b8tcore.command.vanish") || !onlinePlayer.isOp() || !onlinePlayer.hasPermission("*")) {
                    onlinePlayer.hidePlayer(main, player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (main.getVanishedPlayers().contains(onlinePlayer.getUniqueId())) {
                if (!player.hasPermission("8b8tcore.command.vanish") || !player.isOp() || !player.hasPermission("*")) {
                        player.hidePlayer(main, onlinePlayer);
                }
            }
        }
    }
}

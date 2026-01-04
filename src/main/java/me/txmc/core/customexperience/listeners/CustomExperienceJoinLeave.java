package me.txmc.core.customexperience.listeners;

import me.txmc.core.Main;
import me.txmc.core.Reloadable;
import me.txmc.core.customexperience.PlayerPrefix;
import me.txmc.core.customexperience.PlayerSimulationDistance;
import me.txmc.core.customexperience.PlayerViewDistance;
import me.txmc.core.database.GeneralDatabase;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class CustomExperienceJoinLeave implements Listener, Reloadable {
    private final JavaPlugin plugin;
    private final PlayerPrefix playerPrefix;
    private final PlayerSimulationDistance playerSimulationDistance;
    private final PlayerViewDistance playerViewDistance;
    private final GeneralDatabase database;
    private final Main main;

    public CustomExperienceJoinLeave(Main main) {
        this.main = main;
        this.plugin = main;
        this.playerPrefix = new PlayerPrefix(plugin);
        this.playerSimulationDistance = new PlayerSimulationDistance(plugin);
        this.playerViewDistance = new PlayerViewDistance(plugin);
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void reloadConfig() {
        playerPrefix.reloadConfig();
        playerSimulationDistance.reloadConfig();
        playerViewDistance.reloadConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        event.joinMessage(null);

        playerPrefix.handlePlayerJoin(player);
        playerSimulationDistance.handlePlayerJoin(player);
        playerViewDistance.handlePlayerJoin(player);

        sendPrefixedLocalizedMessage(player, "vote_info");

        if (main.getVanishedPlayers().contains(playerUUID)) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                UUID onlineUUID = onlinePlayer.getUniqueId();
                onlinePlayer.getScheduler().run(main, (task) -> {
                    Player p = Bukkit.getPlayer(onlineUUID);
                    Player vanished = Bukkit.getPlayer(playerUUID);
                    if (p != null && p.isOnline() && vanished != null && vanished.isOnline()) {
                        if (!p.hasPermission("8b8tcore.command.vanish") && !p.isOp()) {
                            p.hidePlayer(main, vanished);
                        }
                    }
                }, null);
            }
        } else {
            String displayName = MiniMessage.miniMessage().serialize(player.displayName());
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID onlineUUID = p.getUniqueId();
                database.getPlayerShowJoinMsgAsync(p.getName()).thenAccept(showMsg -> {
                    if (showMsg) {
                        Player recipient = Bukkit.getPlayer(onlineUUID);
                        if (recipient != null && recipient.isOnline()) {
                            recipient.getScheduler().run(main, (task) -> {
                                if (recipient.isOnline()) {
                                    sendPrefixedLocalizedMessage(recipient, "join_message", displayName);
                                }
                            }, null);
                        }
                    }
                });
            }
        }

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 2.0f, 0.7f);
        player.getScheduler().runDelayed(main, (task) -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 2.0f, 1.0f);
            }
        }, null, 6L);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        event.quitMessage(null);

        if (!main.getVanishedPlayers().contains(playerUUID)) {
            String displayName = MiniMessage.miniMessage().serialize(player.displayName());
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID onlineUUID = p.getUniqueId();
                database.getPlayerShowJoinMsgAsync(p.getName()).thenAccept(showMsg -> {
                    if (showMsg) {
                        Player recipient = Bukkit.getPlayer(onlineUUID);
                        if (recipient != null && recipient.isOnline()) {
                            recipient.getScheduler().run(main, (task) -> {
                                if (recipient.isOnline()) {
                                    sendPrefixedLocalizedMessage(recipient, "leave_message", displayName);
                                }
                            }, null);
                        }
                    }
                });
            }
        }
    }
}
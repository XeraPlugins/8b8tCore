package me.txmc.core.timestats.Listeners;

import static java.lang.System.currentTimeMillis;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.txmc.core.timestats.PlayerStats;
import me.txmc.core.timestats.TimeStatsSection;
/**
 *
 * @author 5aks
 * @since 7/19/2025 5:30 PM This file was created as a part of 8b8tCore
 *
 */
public class LeaveJoinListener implements Listener {

    public LeaveJoinListener() {
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handleJoinEvent(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handleQuitEvent(event);
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        handleKickEvent(event);
    }

    private void handleJoinEvent(PlayerJoinEvent event) {                                                           //Grabs players info on join

        String playerName = event.getPlayer().getName();
        Player player = event.getPlayer();
        long now = currentTimeMillis();
        PlayerStats playerStats = TimeStatsSection.getDB().fetchPlayer(playerName);
        if (playerStats != null) {                                                                                  //Checks if new
            playerStats.setSeen(now);
            long playtime = playerStats.getPlaytime();
            TimeStatsSection.addPlayerStats(playerName, playerStats);                                               //Adds PlayerStats to cache
            TimeStatsSection.getDB().insertPlayer(playerName, now, playtime);                                       //Updates players DB entry
            return;
        }
        playerStats = new PlayerStats(player);
        long seen = now;
        playerStats.setSeen(seen);
        TimeStatsSection.getDB().insertNewPlayer(playerName, seen, 0);                                              //Adds new player to DB. Join date sets automatically
        TimeStatsSection.addPlayerStats(playerName, playerStats);                                                   //Adds PlayerStats to cache 

    }

    private void handleQuitEvent(PlayerQuitEvent event) {

        String name = event.getPlayer().getName();                                                                   //Yoinks player name ASAP to get stats from cache
        PlayerStats playerStats = TimeStatsSection.getPlayerStats(name);    
        if (playerStats != null) {                                                                                   //This should never be null, but just in case.
            long now = currentTimeMillis();
            long seen = playerStats.getSeen();
            long playtime = playerStats.getPlaytime() + (now - seen);                                                //Calculate new time played from previous JoinEvent
            playerStats.setPlaytime(playtime);
            playerStats.setSeen(now);
            TimeStatsSection.getDB().insertPlayer(name, now, playtime);                                              //Update DB with last seen and playtime
            TimeStatsSection.removePlayerStats(name);                                                                //Clean cache of PlayerStats
        }
    }

    private void handleKickEvent(PlayerKickEvent event) {

        String name = event.getPlayer().getName();
        PlayerStats playerStats = TimeStatsSection.getPlayerStats(name);    
        if (playerStats != null) {
            long now = currentTimeMillis();
            long seen = playerStats.getSeen();
            long playtime = playerStats.getPlaytime() + (now - seen);
            playerStats.setPlaytime(playtime);
            playerStats.setSeen(now);
            TimeStatsSection.getDB().insertPlayer(name, now, playtime);
            TimeStatsSection.removePlayerStats(name);
        }
    }
}

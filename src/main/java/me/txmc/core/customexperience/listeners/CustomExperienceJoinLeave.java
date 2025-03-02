package me.txmc.core.customexperience.listeners;

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

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class CustomExperienceJoinLeave implements Listener {
    private final JavaPlugin plugin;
    private final PlayerPrefix playerPrefix;
    private final PlayerSimulationDistance playerSimulationDistance;
    private final PlayerViewDistance playerViewDistance;
    private final GeneralDatabase database;

    public CustomExperienceJoinLeave(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerPrefix = new PlayerPrefix(plugin);
        this.playerSimulationDistance = new PlayerSimulationDistance(plugin);
        this.playerViewDistance = new PlayerViewDistance(plugin);
        this.database = new GeneralDatabase(plugin.getDataFolder().getAbsolutePath());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        playerPrefix.handlePlayerJoin(player);
        playerSimulationDistance.handlePlayerJoin(player);
        playerViewDistance.handlePlayerJoin(player);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if(database.getPlayerShowJoinMsg(p.getName())) sendPrefixedLocalizedMessage(p, "join_message", MiniMessage.miniMessage().serialize(player.displayName()));
        }

        sendPrefixedLocalizedMessage(player, "vote_info");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 2.0f, 0.7f);
        Bukkit.getRegionScheduler().runDelayed(plugin, player.getLocation(), (task) -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 2.0f, 1.0f);
            }
        }, 6L);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if(database.getPlayerShowJoinMsg(p.getName())) sendPrefixedLocalizedMessage(p, "leave_message", MiniMessage.miniMessage().serialize(player.displayName()));
        }
    }
}
package me.txmc.core.home.listeners;

import lombok.AllArgsConstructor;
import me.txmc.core.home.HomeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AllArgsConstructor
public class JoinListener implements Listener {
    private final HomeManager main;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        main.homes().put(player, main.storage().load(player));
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        main.storage().save(main.homes().get(player), player);
        main.homes().remove(player);
        main.plugin().lastLocations.remove(player);
    }
}

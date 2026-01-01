package me.txmc.core.achievements;

import me.txmc.core.Main;
import me.txmc.core.database.GeneralDatabase;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AchievementsListener implements Listener {
    private final GeneralDatabase database;

    public AchievementsListener(Main plugin) {
        this.database = GeneralDatabase.getInstance();
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Component msg = event.message();
        if (msg == null) return;

        event.message(null);

        Bukkit.getGlobalRegionScheduler().runDelayed(Main.getInstance(), (task) -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                database.getPlayerHideBadgesAsync(p.getName()).thenAccept(hideBadges -> {
                    if (hideBadges || !p.isOnline()) return;
                    p.getScheduler().run(Main.getInstance(), (playerTask) -> {
                        if (p.isOnline()) p.sendMessage(msg);
                    }, null);
                });
            }
        }, 1L);
    }
}
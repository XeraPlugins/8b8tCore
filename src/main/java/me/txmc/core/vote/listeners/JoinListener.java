package me.txmc.core.vote.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.vote.VoteSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * @author 254n_m
 * @since 2024/02/02 3:53 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class JoinListener implements Listener {
    private final VoteSection main;
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        main.getToRewardEntry(player).ifPresent(i -> {
            for (int j = 0; j < i; j++) main.rewardPlayer(player);
        });
        /**main.markAsRewarded(player.getName());*/
    }
}

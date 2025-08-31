package me.txmc.core.vote.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.vote.VoteSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * @author 254n_m + MindComplexity 
 * @since 2024/02/02 3:53 PM, Updated 8/29/2025
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class JoinListener implements Listener {
    private final VoteSection main;
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
                
        main.checkAndMigrateLegacyPlayer(player.getName());
        
        main.getToRewardEntry(player).ifPresent(voteCount -> {
            if (voteCount > 0) {
                main.rewardOfflineVotes(player, voteCount);
                main.markAsRewarded(player.getName().toLowerCase());
            }
        });
    }
}

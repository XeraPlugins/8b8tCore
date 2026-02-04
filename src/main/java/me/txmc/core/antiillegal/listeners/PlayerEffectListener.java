package me.txmc.core.antiillegal.listeners;

import me.txmc.core.antiillegal.check.checks.PlayerEffectCheck;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener to monitor and fix illegal potion effects on players
 * Some of this code was inspired by surf's antiillegal plugin
 * https://github.com/Winds-Studio/Surf/blob/main/src/main/java/cn/dreeam/surf/modules/antiillegal/IllegalDamageAndPotionCheck.java you can find the original code there.
 * 
 * This listener uses a single repeating task to periodically check players,
 * lu: avoiding multiple timers being scheduled for the same purpose.
 * 
 * @author Libalpm (MindComplexity)
 * @since 2025-08-20
 */
public class PlayerEffectListener implements Listener {
    
    private final Plugin plugin;
    private final PlayerEffectCheck effectCheck;
    private final int checkInterval;
    
    public PlayerEffectListener(Plugin plugin) {
        this(plugin, 100); 
    }
    
    public PlayerEffectListener(Plugin plugin, int checkInterval) {
        this.plugin = plugin;
        this.effectCheck = new PlayerEffectCheck();
        this.checkInterval = checkInterval;        
        startEffectChecker();
    }
    
    private void startEffectChecker() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            checkAllPlayers();
        }, 20L, checkInterval);
    }
    
    private void checkAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkPlayer(event.getPlayer());
    }

    public void checkPlayer(Player player) {
        if (player != null && player.isValid() && !player.isDead()) {
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    effectCheck.fixPlayerEffects(player);
                }
            }, null);
        }
    }
    
    public PlayerEffectCheck getEffectCheck() {
        return effectCheck;
    }
}

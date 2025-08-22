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
        /***
         *  MODIFY THIS TO CHANGE THE CHECK INTERVAL
         *  This is the interval at which the plugin will check for illegal effects.
         *  The default is 100 ticks, which is 5 seconds. (20 ticks = 1 second)
         */
        this(plugin, 100); 
    }
    
    public PlayerEffectListener(Plugin plugin, int checkInterval) {
        this.plugin = plugin;
        this.effectCheck = new PlayerEffectCheck();
        this.checkInterval = checkInterval;        
        startEffectChecker();
    }
    
    /**
     * Start the periodic task to check all online players.
     * This is the only repeating timer used in this listener.
     */
    private void startEffectChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAllPlayers();
            }
        }.runTaskTimer(plugin, 20L, checkInterval);
    }
    
    /**
     * Check all online players for illegal effects
     */
    private void checkAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player);
        }
    }
    
    /**
     * Check and fix a specific player's effects
     */
    private void checkAndFixPlayerEffects(Player player) {
        if (effectCheck.checkPlayerEffects(player)) {
            effectCheck.fixPlayerEffects(player);
        }
    }
    
    /**
     * Check player effects when they join the server.
     * 
     * Currently, this just checks the player immediately on join.
     * If you have some custom plugin that applies after join just modify this with a tick timer. 
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkPlayer(event.getPlayer());
    }
    
    /**
     * API Call for a specific player (can be called from commands or other parts of the code)
     */
    public void checkPlayer(Player player) {
        if (player != null && player.isValid() && !player.isDead()) {
            checkAndFixPlayerEffects(player);
        }
    }
    
    /**
     * Get the effect check instance for external use
     */
    public PlayerEffectCheck getEffectCheck() {
        return effectCheck;
    }
}

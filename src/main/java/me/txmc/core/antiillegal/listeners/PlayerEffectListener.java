package me.txmc.core.antiillegal.listeners;

import me.txmc.core.antiillegal.AntiIllegalMain;
import me.txmc.core.antiillegal.check.checks.PlayerEffectCheck;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

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
    private final AntiIllegalMain main;
    
    public PlayerEffectListener(Plugin plugin, AntiIllegalMain main) {
        this(plugin, main, 100); 
    }
    
    public PlayerEffectListener(Plugin plugin, AntiIllegalMain main, int checkInterval) {
        this.plugin = plugin;
        this.main = main;
        this.effectCheck = new PlayerEffectCheck();
        this.checkInterval = checkInterval;        
        startEffectChecker();
        startInventoryChecker();
    }
    
    private void startEffectChecker() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            checkAllPlayersEffects();
        }, 20L, checkInterval);
    }

    private void startInventoryChecker() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            checkAllPlayersInventories();
        }, 20L, checkInterval);
    }
    
    private void checkAllPlayersEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayerEffects(player);
        }
    }

    private void checkAllPlayersInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayerInventory(player);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkPlayerEffects(event.getPlayer());
        checkPlayerInventory(event.getPlayer());
    }

    public void checkPlayerEffects(Player player) {
        if (player != null && player.isValid() && !player.isDead()) {
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    effectCheck.fixPlayerEffects(player);
                }
            }, null);
        }
    }

    public void checkPlayerInventory(Player player) {
        if (player != null && player.isValid() && !player.isDead()) {
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    PlayerInventory inv = player.getInventory();
                    
                    // Main Hand, Offhand
                    main.checkFixItem(inv.getItemInMainHand(), null);
                    main.checkFixItem(inv.getItemInOffHand(), null);

                    // Armor
                    for (ItemStack armor : inv.getArmorContents()) {
                        if (armor != null) main.checkFixItem(armor, null);
                    }

                    // Check inventory slots
                    for (ItemStack item : inv.getContents()) {
                        if (item != null) main.checkFixItem(item, null);
                    }
                }
            }, null);
        }
    }
    
    public PlayerEffectCheck getEffectCheck() {
        return effectCheck;
    }
}

package me.txmc.core.antiillegal.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * This file is apart of 8b8tAntiIllegal.
 * Listener for handling and cleaning up stacked totems in various player interactions.
 */
public class StackedTotemsListener implements Listener {

    private final Plugin plugin;

    public StackedTotemsListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            cleanTotemStacks(player.getInventory());
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            cleanTotemStack(event.getItem().getItemStack());
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        cleanTotemStacks(event.getPlayer().getInventory());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        cleanTotemStacks(event.getPlayer().getInventory());
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        Player attacker = null;
        
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj 
                   && proj.getShooter() instanceof Player p) {
            attacker = p;
        }
        
        if (attacker != null) {
            final Player finalAttacker = attacker;
            finalAttacker.getScheduler().run(plugin, task -> {
                if (finalAttacker.isOnline()) {
                    cleanTotemStacks(finalAttacker.getInventory());
                }
            }, null);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            cleanTotemStacks(player.getInventory());
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        cleanTotemStacks(event.getPlayer().getInventory());
    }

    private void cleanTotemStacks(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING && item.getAmount() > 1) {
                item.setAmount(1);
            }
        }
    }

    private void cleanTotemStack(ItemStack item) {
        if (item.getType() == Material.TOTEM_OF_UNDYING && item.getAmount() > 1) {
            item.setAmount(1);
        }
    }
}

package me.txmc.core.antiillegal.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for handling and cleaning up stacked totems in various player interactions.
 *
 * <p>This class is part of the 8b8tCore plugin and ensures that totems do not exceed
 * the allowed stack size of one in the player's inventory. It listens to multiple events to
 * handle different scenarios where totems might be stacked or modified.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Cleaning up totem stacks when a player clicks in their inventory</li>
 *     <li>Handling item pickups to prevent stacked totems</li>
 *     <li>Ensuring totem stack size remains compliant during item swaps</li>
 *     <li>Clearing totem stacks during inventory drag operations</li>
 *     <li>Handling item pickups into inventory</li>
 *     <li>Adjusting totem stacks when items are dropped by the player</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/03 15:17
 */
public class StackedTotemsListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            var player = (Player) event.getWhoClicked();
            var inventory = player.getInventory();
            cleanTotemStacks(inventory);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            var player = (Player) event.getEntity();
            var item = event.getItem().getItemStack();
            cleanTotemStack(item, player);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        var item = event.getItem();
        if (item.getType() == Material.TOTEM_OF_UNDYING) {
            var player = event.getPlayer();
            var inventory = player.getInventory();
            cleanTotemStacks(inventory);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        var player = event.getPlayer();
        var inventory = player.getInventory();

        cleanTotemStacks(inventory);
    }


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            var player = (Player) event.getWhoClicked();
            var inventory = player.getInventory();
            cleanTotemStacks(inventory);
        }
    }

    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        var inventory = event.getInventory();
        cleanTotemStacks(inventory);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        var player = event.getPlayer();
        var inventory = player.getInventory();
        cleanTotemStacks(inventory);
    }

    private void cleanTotemStacks(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                int amount = item.getAmount();
                if (amount > 1) {
                    item.setAmount(1); // Keep only one totem
                    //inventory.addItem(new ItemStack(Material.TOTEM_OF_UNDYING, amount - 1)); // Add the rest to the inventory
                }
            }
        }
    }

    private void cleanTotemStack(ItemStack item, Player player) {
        if (item.getType() == Material.TOTEM_OF_UNDYING) {
            int amount = item.getAmount();
            if (amount > 1) {
                item.setAmount(1);
                //player.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, amount - 1));
            }
        }
    }

    private void cleanItemStack(ItemStack item) {
        int amount = item.getAmount();
        if (amount > 1) {
            item.setAmount(1);
        }
    }

}

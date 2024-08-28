package me.txmc.core.antiillegal.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.AntiIllegalMain;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author 254n_m
 * @since 2023/10/15 4:30 AM
 * This file was created as a part of 8b8tAntiIllegal
 */
@RequiredArgsConstructor
public class InventoryListeners implements Listener {
    private final AntiIllegalMain main;

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        inventoryEvent(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        inventoryEvent(event);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        main.checkFixItem(event.getCursor(), event);
    }

    @EventHandler
    public void onHopper(InventoryMoveItemEvent event) {
        main.checkFixItem(event.getItem(), event);
    }

    @EventHandler
    private void inventoryEvent(InventoryEvent event) {
        for (ItemStack itemStack : event.getInventory()) main.checkFixItem(itemStack, null);
    }

    @EventHandler
    private void inventoryClickEvent(InventoryClickEvent event) {
        for (ItemStack itemStack : event.getInventory()) main.checkFixItem(itemStack, event);
    }

    @EventHandler
    private void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
        for (ItemStack itemStack : event.getPlayer().getInventory()) main.checkFixItem(itemStack, event);
    }

}

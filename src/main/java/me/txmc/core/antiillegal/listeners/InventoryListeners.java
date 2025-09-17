package me.txmc.core.antiillegal.listeners;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.AntiIllegalMain;
import me.txmc.core.antiillegal.check.Check;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author 254n_m + MindComplexity
 * @since 2023/10/15 4:30 AM, Updated 2025/09/17
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
        checkBundleContents(event.getCursor());
    }

    @EventHandler
    public void onHopper(InventoryMoveItemEvent event) {
        main.checkFixItem(event.getItem(), event);
        checkBundleContents(event.getItem());
    }

    @EventHandler
    private void inventoryEvent(InventoryEvent event) {
        for (ItemStack itemStack : event.getInventory()) {
            main.checkFixItem(itemStack, null);
            checkBundleContents(itemStack);
        }
    }

    @EventHandler
    private void inventoryClickEvent(InventoryClickEvent event) {
        for (ItemStack itemStack : event.getInventory()) {
            main.checkFixItem(itemStack, event);
            checkBundleContents(itemStack);
        }
    }

    @EventHandler
    private void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
        for (ItemStack itemStack : event.getPlayer().getInventory()) {
            main.checkFixItem(itemStack, event);
            checkBundleContents(itemStack);
        }
    }

    private void checkBundleContents(ItemStack item) {
        if (item == null || item.getType() != Material.BUNDLE) return;
        
        BundleContents bundleContents = item.getData(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContents == null) return;
        
        for (ItemStack bundleItem : bundleContents.contents()) {
            if (bundleItem == null) continue;
            
            for (Check check : main.checks()) {
                if (check.shouldCheck(bundleItem) && check.check(bundleItem)) {
                    item.setAmount(0);
                    return;
                }
            }
        }
    }
}

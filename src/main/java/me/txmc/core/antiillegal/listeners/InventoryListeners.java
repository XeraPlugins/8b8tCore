package me.txmc.core.antiillegal.listeners;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import io.papermc.paper.datacomponent.item.ItemContainerContents; // Added this
import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.AntiIllegalMain;
import me.txmc.core.antiillegal.check.Check;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.Inventory; // Added this
import org.bukkit.inventory.ItemStack;

/**
 * @author 254n_m + MindComplexity
 * @since 2023/10/15 4:30 AM, Updated 2026/01/03
 * This file was created as a part of 8b8tAntiIllegal
*/

@RequiredArgsConstructor
public class InventoryListeners implements Listener {
    private final AntiIllegalMain main;

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        String invType = inv.getType().name();
        
        boolean openedShulker = invType.contains("SHULKER");
        
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            
            main.checkFixItem(item, event);
            
            if (openedShulker && isContainer(item.getType())) {
                clearContainerContents(item);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        for (ItemStack item : event.getInventory()) {
            main.checkFixItem(item, null);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        main.checkFixItem(event.getCursor(), event);
        main.checkFixItem(event.getCurrentItem(), event);
        checkBundleContents(event.getCursor());
        checkBundleContents(event.getCurrentItem());
    }

    @EventHandler
    public void onHopper(InventoryMoveItemEvent event) {
        main.checkFixItem(event.getItem(), event);
        checkBundleContents(event.getItem());
    }

    private void clearContainerContents(ItemStack container) {
        if (!container.hasData(DataComponentTypes.CONTAINER)) return;
        
        ItemContainerContents contents = container.getData(DataComponentTypes.CONTAINER);
        if (contents == null) return;
        
        boolean hasContents = contents.contents().stream()
                .anyMatch(item -> item != null && !item.getType().isAir());
        
        if (hasContents) {
            container.unsetData(DataComponentTypes.CONTAINER);
        }
    }

    private void checkBundleContents(ItemStack item) {
        if (item == null || item.getType() != Material.BUNDLE) return;

        BundleContents contents = item.getData(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return;

        for (ItemStack bundleItem : contents.contents()) {
            if (bundleItem == null) continue;

            for (Check check : main.checks()) {
                if (check.shouldCheck(bundleItem) && check.check(bundleItem)) {
                    item.setAmount(0);
                    return;
                }
            }
        }
    }

    private boolean isShulkerBox(Material type) {
        return type.name().contains("SHULKER_BOX");
    }
    
    private boolean isContainer(Material type) {
        return type.name().contains("SHULKER_BOX") ||
               type == Material.CHEST ||
               type == Material.TRAPPED_CHEST ||
               type == Material.BARREL ||
               type == Material.DISPENSER ||
               type == Material.DROPPER ||
               type == Material.HOPPER ||
               type == Material.CHISELED_BOOKSHELF;
    }
}
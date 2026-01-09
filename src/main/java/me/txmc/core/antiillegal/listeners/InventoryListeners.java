package me.txmc.core.antiillegal.listeners;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.AntiIllegalMain;
import me.txmc.core.antiillegal.check.Check;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author MindComplexity + 254_m
 * @since 2023/10/15 4:30 AM, Updated 2026/01/03
 * This file was created as a part of 8b8tAntiIllegal
*/

@RequiredArgsConstructor
public class InventoryListeners implements Listener {
    private final AntiIllegalMain main;
    private static final int MAX_BUNDLE_WEIGHT = 64;
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
        if (item == null || !isBundle(item.getType())) return;
        if (!item.hasData(DataComponentTypes.BUNDLE_CONTENTS)) return;

        if (checkBundleRecursion(item, 0)) {
            item.setAmount(0);
            return;
        }

        BundleContents contents = item.getData(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return;

        int totalWeight = 0;

        for (ItemStack bundleItem : contents.contents()) {
            if (bundleItem == null || bundleItem.getType().isAir()) continue;

            if (isBundle(bundleItem.getType())) {
                item.setAmount(0);
                return;
            }

            if (bundleItem.getAmount() > bundleItem.getType().getMaxStackSize()) {
                item.setAmount(0);
                return;
            }

            int maxStack = bundleItem.getType().getMaxStackSize();
            totalWeight += bundleItem.getAmount() * (64 / maxStack);

            for (Check check : main.checks()) {
                if (check.shouldCheck(bundleItem) && check.check(bundleItem)) {
                    item.setAmount(0);
                    return;
                }
            }
        }

        if (totalWeight > MAX_BUNDLE_WEIGHT) {
            item.setAmount(0);
        }
    }

    private boolean isBundle(Material type) {
        return type.name().endsWith("BUNDLE");
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

    private boolean checkBundleRecursion(ItemStack item, int depth) {
        if (item == null || !isBundle(item.getType())) return false;
        if (depth >= 1) return true;

        try {
            if (item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.BundleMeta bundleMeta) {
                for (ItemStack inner : bundleMeta.getItems()) {
                    if (inner == null) continue;
                    if (isBundle(inner.getType())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
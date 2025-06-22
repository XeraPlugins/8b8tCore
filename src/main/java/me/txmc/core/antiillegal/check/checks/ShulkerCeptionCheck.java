package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.Arrays;

/**
 * Checks for shulkers inside shulkers ("shulker ceptions") and removes them.
 *
 * @since 2025-06-22
 */
public class ShulkerCeptionCheck implements Check {

    @Override
    public boolean check(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.SHULKER_BOX) return false;

        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return false;

        BlockState state = meta.getBlockState();
        if (!(state instanceof ShulkerBox shulkerBox)) return false;

        Inventory inv = shulkerBox.getInventory();
        // Check for any nested shulker box inside
        return Arrays.stream(inv.getContents())
                .anyMatch(i -> i != null && i.getType() == Material.SHULKER_BOX);
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true; // Check all items
    }

    @Override
    public void fix(ItemStack item) {
        if (item == null) return;
        if (item.getType() != Material.SHULKER_BOX) return;

        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return;

        BlockState state = meta.getBlockState();
        if (!(state instanceof ShulkerBox shulkerBox)) return;

        Inventory inv = shulkerBox.getInventory();
        boolean changed = false;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack content = inv.getItem(i);
            if (content != null && content.getType() == Material.SHULKER_BOX) {
                inv.setItem(i, null);  // Remove nested shulker box
                changed = true;
            }
        }

        if (changed) {
            // Update block state and item meta
            shulkerBox.update();
            meta.setBlockState(shulkerBox);
            item.setItemMeta(meta);
        }
    }
}

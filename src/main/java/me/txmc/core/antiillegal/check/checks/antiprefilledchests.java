package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * Prevents players from having items already prefilled in chests this is not possible without modifying the NBT data of the item.
 * @author MindComplexity (Aka Libalpm)
 * @since 2025-08-20
 */
public class antiprefilledchests implements Check {
    
    @Override
    public boolean check(ItemStack item) {
        if (item == null) return false;
        if (!isNonShulkerContainer(item)) return false;

        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return false;

        BlockState state = meta.getBlockState();
        if (!(state instanceof Container container)) return false;

        Inventory inv = container.getInventory();
        
        for (ItemStack content : inv.getContents()) {
            if (content != null) {
                return true; 
            }
        }
        
        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return isNonShulkerContainer(item);
    }

    @Override
    public void fix(ItemStack item) {
        if (item == null) return;
        if (!isNonShulkerContainer(item)) return;

        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return;

        BlockState state = meta.getBlockState();
        if (!(state instanceof Container container)) return;

        Inventory inv = container.getInventory();
        
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, null);
        }
        
        container.update();
        meta.setBlockState(container);
        item.setItemMeta(meta);
    }
    
    private boolean isNonShulkerContainer(ItemStack item) {
        if (item == null) return false;
        String type = item.getType().toString();
        return type.endsWith("CHEST") ||
               type.endsWith("TRAPPED_CHEST") ||
               type.endsWith("BARREL") ||
               type.endsWith("DISPENSER") ||
               type.endsWith("DROPPER") ||
               type.endsWith("HOPPER");
    }
}

package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * @author MindComplexity
 * @since 2026/01/26
 * This file was created as a part of 8b8tCore
*/

public class ItemSizeCheck implements Check {
    // masons made up number
    private final int MAX_SIZE = 106476;

    @Override
    public boolean check(ItemStack item) {
        int size = getSize(item);
        return size > MAX_SIZE;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        
        // Our name check will handle the rest, this is mainly to solve crash maps / book bans.
        Material type = item.getType();
        return item.getItemMeta() instanceof BlockStateMeta 
                || type == Material.WRITTEN_BOOK 
                || type == Material.WRITABLE_BOOK 
                || type == Material.FILLED_MAP;
    }

    @Override
    public void fix(ItemStack item) {
        item.setAmount(0);
    }

    private int getSize(ItemStack itemStack) {
        return GlobalUtils.calculateItemSize(itemStack);
    }
}

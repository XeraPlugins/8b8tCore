package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents stacked Totems of Undying anywhere by enforcing max stack size 1.
 * Can be used on any ItemStack regardless of where it is found.
 * @since 2025-06-22
 */
public class StackedTotemCheck implements Check {

    @Override
    public boolean check(ItemStack item) {
        if (item == null) return false;
        return item.getType() == Material.TOTEM_OF_UNDYING && item.getAmount() > 1;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true;
    }

    @Override
    public void fix(ItemStack item) {
        if (item == null) return;

        if (item.getType() == Material.TOTEM_OF_UNDYING && item.getAmount() > 1) {
            item.setAmount(1);
        }
    }
}
package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;import org.bukkit.inventory.ItemStack;

/**
 * @author 254n_m
 * @since 2023/09/29 6:53 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public class IllegalDataCheck implements Check {
    @Override
    public boolean check(ItemStack item) {
        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return false;
    }

    @Override
    public void fix(ItemStack item) {

    }
}

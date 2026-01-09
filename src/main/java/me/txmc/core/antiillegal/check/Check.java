package me.txmc.core.antiillegal.check;

import org.bukkit.inventory.ItemStack;

/**
 * @author 254n_m
 * @since 2023/09/18 10:37 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public interface Check {
    boolean check(ItemStack item);

    boolean shouldCheck(ItemStack item);

    void fix(ItemStack item);
}

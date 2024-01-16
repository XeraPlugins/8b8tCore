package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

/**
 * @author 254n_m
 * @since 2024/01/15 7:28 PM
 * This file was created as a part of 8b8tCore
 */
public class PotionCheck implements Check {
    @Override
    public boolean check(ItemStack item) {
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        return meta.hasCustomEffects() || meta.hasColor();
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta() instanceof PotionMeta;
    }

    @Override
    public void fix(ItemStack item) {
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.clearCustomEffects();
        item.setItemMeta(meta);
    }
}

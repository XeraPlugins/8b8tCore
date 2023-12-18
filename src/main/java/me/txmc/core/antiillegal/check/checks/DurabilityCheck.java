package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author 254n_m
 * @since 2023/09/20 2:07 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public class DurabilityCheck implements Check {
    @Override
    public boolean check(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable && damageable.getDamage() < 0) return true;
        return meta.isUnbreakable();
    }

    @Override
    public boolean shouldCheck(ItemStack material) {
        return true;
    }

    @Override
    public void fix(ItemStack item) {
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta.isUnbreakable()) meta.setUnbreakable(false);
        if (meta instanceof Damageable damageable) {
            if (damageable.getDamage() < 0) {
                damageable.setDamage(1);
            } else if (damageable.getDamage() > item.getType().getMaxDurability()) damageable.setDamage(item.getType().getMaxDurability());
            item.setItemMeta(meta);
        }
    }
}

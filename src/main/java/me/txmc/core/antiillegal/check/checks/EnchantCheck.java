package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

/**
 * @author 254n_m
 * @since 2023/09/20 9:47 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public class EnchantCheck implements Check {
    @Override
    public boolean check(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasEnchants()) return false;
        if (item.getType().isBlock()) return true;
        Map<Enchantment, Integer> enchants = meta.getEnchants();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            if (entry.getValue() > entry.getKey().getMaxLevel()) return true;
        }
        return false;
//        if (!item.hasItemMeta()) return false;
//        ItemMeta meta = item.getItemMeta();
//        if (!meta.hasEnchants()) return false;
//        Map<Enchantment, Integer> enchants = meta.getEnchants();
//        Enchantment[] enchantments = enchants.keySet().toArray(new Enchantment[0]);
//        Integer[] levels = enchants.values().toArray(new Integer[0]);
//        if (enchants.size() > 1) {
//            for (int i = 1; i < enchants.size(); i++) {
//                Enchantment currentEnch = enchantments[i], lastEnch = enchantments[i - 1];
//                int currentLevel = levels[i];
//                if (currentLevel > currentEnch.getMaxLevel()) return true;
//                if (!currentEnch.canEnchantItem(item) || !lastEnch.canEnchantItem(item)) return true;
//                if (currentEnch.conflictsWith(lastEnch)) return true;
//                if (lastEnch.conflictsWith(currentEnch)) return true;
//            }
//        } else {
//
//        }
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true;
    }

    @Override
    public void fix(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Map<Enchantment, Integer> enchants = meta.getEnchants();
        boolean remove = item.getType().isBlock();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            if (remove) {
                meta.removeEnchant(entry.getKey());
                continue;
            }
            if (entry.getValue() > entry.getKey().getMaxLevel()) {
                meta.removeEnchant(entry.getKey());
                meta.addEnchant(entry.getKey(), entry.getKey().getMaxLevel(), false);
            }
        }
        item.setItemMeta(meta);
    }
}

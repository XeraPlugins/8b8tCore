package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * @author 254n_m
 * @since 2023/09/20 7:35 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public class LoreCheck implements Check {
    @Override
    public boolean check(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasLore();
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return item.getType() != Material.MAP && item.getType() != Material.FILLED_MAP;
    }

    @Override
    public void fix(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.lore(List.of());
        item.setItemMeta(meta);
    }
}

package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author 254n_m
 * @since 2023/09/20 7:38 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public class AttributeCheck implements Check {
    @Override
    public boolean check(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        boolean illegal = item.getItemMeta().hasAttributeModifiers();
        if (illegal) {
            if (me.txmc.core.antiillegal.AntiIllegalMain.debug) me.txmc.core.util.GlobalUtils.log(java.util.logging.Level.INFO, "&cAttributeCheck flagged item %s. HasModifiers: %b", item.getType(), item.getItemMeta().hasAttributeModifiers());
        }
        return illegal;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true;
    }

    @Override
    public void fix(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta.getAttributeModifiers() != null) meta.getAttributeModifiers().forEach((a, m) -> meta.removeAttributeModifier(a));
        meta.removeItemFlags(meta.getItemFlags().toArray(ItemFlag[]::new));
        item.setItemMeta(meta);
    }
}

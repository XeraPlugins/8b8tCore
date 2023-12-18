package me.txmc.core.antiillegal.util;

import me.txmc.core.antiillegal.AntiIllegalMain;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/**
 * @author 254n_m
 * @since 2023/09/18 10:29 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public class Utils {
    public static void checkStand(ArmorStand stand, AntiIllegalMain main) {
        EntityEquipment eq = stand.getEquipment();
        main.checkFixItem(eq.getItemInMainHand(), null);
        main.checkFixItem(eq.getItemInOffHand(), null);
        for (ItemStack item : eq.getArmorContents()) main.checkFixItem(item, null);
    }
}

package me.txmc.core.antiillegal.check.checks;

import io.papermc.paper.datacomponent.item.DeathProtection;
import me.txmc.core.antiillegal.check.Check;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author MindComplexity
 * @since 2025/09/17
 * This file was created as a part of 8b8tAntiIllegal
 */

public class IllegalDataCheck implements Check {
    private static final int MAX_LEGAL_AMPLIFIER = 5;
    private static final int MAX_LEGAL_DURATION = 9600;

    @Override
    public boolean check(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        try {
            if (meta.isGlider() && item.getType() != Material.ELYTRA) {
                return true;
            }
            
            if (hasDeathProtectionComponent(meta) && item.getType() != Material.TOTEM_OF_UNDYING) {
                return true;
            }

            if (hasIllegalFoodEffects(meta)) {
                return true;
            }
            
        } catch (Exception e) {
            return false;
        }
        
        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return item != null && item.hasItemMeta();
    }

    @Override
    public void fix(ItemStack item) {
        item.setAmount(0);
    }
    
    private boolean hasDeathProtectionComponent(ItemMeta meta) {
        String componentString = meta.getAsComponentString();
        return componentString.contains("minecraft:death_protection");
    }
    
    private boolean hasIllegalFoodEffects(ItemMeta meta) {
        String componentString = meta.getAsComponentString().toLowerCase();
        
        if (componentString.contains("minecraft:food") || componentString.contains("minecraft:consumable")) {
            if (componentString.contains("amplifier") && componentString.matches(".*amplifier[=\\s]+([6-9]|\\d{2,}).*")) {
                return true;
            }
            
            if (componentString.contains("duration") && componentString.matches(".*duration[=\\s]+(9[7-9][0-9]{2}|[1-9]\\d{5,}).*")) {
                return true;
            }
        }        
        return false;
    }
}

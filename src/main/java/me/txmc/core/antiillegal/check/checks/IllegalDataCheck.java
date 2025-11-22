package me.txmc.core.antiillegal.check.checks;

import io.papermc.paper.datacomponent.DataComponentTypes;
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
            String s = meta.getAsComponentString();
            if (s != null && s.toLowerCase().contains("minecraft:luck")) {
                return true;
            }
            if (meta.isGlider() && item.getType() != Material.ELYTRA) {
                return true;
            }
            
            if (hasDeathProtectionComponent(meta) && item.getType() != Material.TOTEM_OF_UNDYING) {
                return true;
            }

            if (hasIllegalFoodEffects(meta)) {
                return true;
            }
            
            if (hasIllegalBlockState(meta)) {
                return true;
            }
            
            if (hasIllegalMaxDamage(meta)) {
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
        if (item == null || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        try {
            String sLuck = meta.getAsComponentString();
            if (sLuck != null && sLuck.toLowerCase().contains("minecraft:luck")) {
                item.setAmount(0);
                return;
            }
            boolean durable = item.getType().getMaxDurability() > 0;

            if (meta.isGlider() && item.getType() != Material.ELYTRA) {
                meta.setGlider(false);
            }
            item.setItemMeta(meta);

            if (item.getType() != Material.TOTEM_OF_UNDYING) {
                item.unsetData(DataComponentTypes.DEATH_PROTECTION);
            }
            item.unsetData(DataComponentTypes.MAX_DAMAGE);

            String s = item.getItemMeta() != null ? item.getItemMeta().getAsComponentString() : null;
            if (s != null) {
                if (durable && s.contains("!minecraft:max_damage")) {
                    int max = item.getType().getMaxDurability();
                    if (max > 0) item.setData(DataComponentTypes.MAX_DAMAGE, max);
                }
                if (durable && s.toLowerCase().contains("minecraft:max_stack_size")) {
                    item.unsetData(DataComponentTypes.MAX_STACK_SIZE);
                }
            }
        } catch (Exception ignored) {}
    }
    
    private boolean hasDeathProtectionComponent(ItemMeta meta) {
        String componentString = meta.getAsComponentString();
        return componentString.contains("minecraft:death_protection");
    }
    
    private boolean hasIllegalMaxDamage(ItemMeta meta) {
        String componentString = meta.getAsComponentString();
        return componentString.contains("!minecraft:max_damage");
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
    private boolean hasIllegalBlockState(ItemMeta meta) {
        String componentString = meta.getAsComponentString();
        return componentString.contains("waterlogged: \"true\"");
    }
}

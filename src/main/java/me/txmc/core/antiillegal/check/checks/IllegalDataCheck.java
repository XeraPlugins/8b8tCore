package me.txmc.core.antiillegal.check.checks;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.datacomponent.item.Tool;
import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;

/**
 * This file is apart of 8b8tcore.
 * @author MindComplexity
 * @since 01/02/2026
*/

public class IllegalDataCheck implements Check {
    
    private static final int MAX_LEGAL_AMPLIFIER = 5;
    private static final int MAX_LEGAL_DURATION = 9600;
    private static final int MAX_NAME_PLAIN_LENGTH = 128;
    private static final int MAX_NAME_JSON_LENGTH = 4096;
    private static final float MAX_LEGAL_TOOL_SPEED = 50.0f;

    @Override
    public boolean check(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        
        Material type = item.getType();
        
        if (isContainer(type)) return false;
        
        try {
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (hasIllegalName(meta)) return true;
                    if (meta.isGlider() && type != Material.ELYTRA) return true;
                }
            }

            if (hasIllegalWaterloggedState(item)) {
                return true;
            }
            
            if (type != Material.TOTEM_OF_UNDYING && item.hasData(DataComponentTypes.DEATH_PROTECTION)) {
                return true;
            }
            
            if (isPotion(type) && hasIllegalPotionEffects(item)) {
                return true;
            }
            
            if (hasIllegalFoodEffects(item)) {
                return true;
            }
            
            if (type.getMaxDurability() > 0 && !item.hasData(DataComponentTypes.MAX_DAMAGE)) {
                return true;
            }
            
            if (item.hasData(DataComponentTypes.MAX_STACK_SIZE)) {
                Integer maxStack = item.getData(DataComponentTypes.MAX_STACK_SIZE);
                // Negated component (when max_stack_size component is null)
                if (maxStack == null || maxStack < 1 || maxStack > 99) {
                    return true;
                }
            } else {
                // Check for items that should stack but have been limited to 1
                int vanillaMaxStack = type.getMaxStackSize();
                int actualMaxStack = item.getMaxStackSize();
                if (vanillaMaxStack > 1 && actualMaxStack == 1) {
                    return true;
                }
            }

            if (hasIllegalToolComponent(item)) {
                return true;
            }

            if (item.getType() == Material.FILLED_MAP && item.hasItemMeta() && item.getItemMeta() instanceof MapMeta mm) {
                 PersistentDataContainer pdc = mm.getPersistentDataContainer();
                 for (NamespacedKey key : pdc.getKeys()) {
                     if (key.toString().contains("VV|") || key.toString().contains("Protocol")) return true;
                 }
            }
            
        } catch (Exception e) {
            return false;
        }
        
        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return !isContainer(item.getType());
    }

    @Override
    public void fix(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        
        Material type = item.getType();
        
        if (isContainer(type)) return;
        
        try {
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    boolean metaChanged = false;
                    
                    if (hasIllegalName(meta)) {
                        meta.customName(null);
                        metaChanged = true;
                    }
                    
                    if (meta.isGlider() && type != Material.ELYTRA) {
                        meta.setGlider(false);
                        metaChanged = true;
                    }
                    
                    if (metaChanged) item.setItemMeta(meta);
                }
            }

            fixWaterloggedState(item);

            if (type != Material.TOTEM_OF_UNDYING) {
                item.unsetData(DataComponentTypes.DEATH_PROTECTION);
            }

            if (isPotion(type) && hasIllegalPotionEffects(item)) {
                item.unsetData(DataComponentTypes.POTION_CONTENTS);
            }
            
            if (hasIllegalFoodEffects(item)) {
                item.unsetData(DataComponentTypes.FOOD);
                item.unsetData(DataComponentTypes.CONSUMABLE);
            }

            if (type.getMaxDurability() > 0 && !item.hasData(DataComponentTypes.MAX_DAMAGE)) {
                item.setData(DataComponentTypes.MAX_DAMAGE, (int) type.getMaxDurability());
            }

            if (item.hasData(DataComponentTypes.MAX_STACK_SIZE)) {
                Integer maxStack = item.getData(DataComponentTypes.MAX_STACK_SIZE);
                if (maxStack == null) {
                    int vanillaMaxStack = type.getMaxStackSize();
                    if (vanillaMaxStack > 1) {
                        item.setData(DataComponentTypes.MAX_STACK_SIZE, vanillaMaxStack);
                    } else {
                        item.unsetData(DataComponentTypes.MAX_STACK_SIZE);
                    }
                } else if (maxStack < 1 || maxStack > 99) {
                    item.unsetData(DataComponentTypes.MAX_STACK_SIZE);
                }
            } else {
                int vanillaMaxStack = type.getMaxStackSize();
                int actualMaxStack = item.getMaxStackSize();
                
                if (vanillaMaxStack > 1 && actualMaxStack == 1) {
                    item.setData(DataComponentTypes.MAX_STACK_SIZE, vanillaMaxStack);
                }
            }

            if (hasIllegalToolComponent(item)) {
                item.unsetData(DataComponentTypes.TOOL);
            }

            if (item.getType() == Material.FILLED_MAP) {
                 sanitizeCrashMap(item);
            }
            
        } catch (Exception ignored) {}
    }

    private boolean hasIllegalToolComponent(ItemStack item) {
        if (!item.hasData(DataComponentTypes.TOOL)) return false;

        boolean shouldHaveTool = item.getType().getDefaultData(DataComponentTypes.TOOL) != null;

        if (!shouldHaveTool) return true;

        Tool tool = item.getData(DataComponentTypes.TOOL);
        if (tool == null) return false;

        for (Tool.Rule rule : tool.rules()) {
            Float speed = rule.speed();
            if (speed != null && speed > MAX_LEGAL_TOOL_SPEED) {
                return true;
            }
        }
        if (tool.defaultMiningSpeed() > MAX_LEGAL_TOOL_SPEED) {
            return true;
        }

        return false;
    }
    
    private boolean isContainer(Material type) {
        return type.name().contains("SHULKER_BOX") || type.name().endsWith("BUNDLE");
    }
    
    private boolean isPotion(Material type) {
        return type == Material.POTION || type == Material.SPLASH_POTION || 
               type == Material.LINGERING_POTION || type == Material.TIPPED_ARROW;
    }

    private boolean hasIllegalWaterloggedState(ItemStack item) {
        if (!item.getType().isBlock()) return false;
        if (!item.hasData(DataComponentTypes.BLOCK_DATA)) return false;

        try {
            var properties = item.getData(DataComponentTypes.BLOCK_DATA);
            if (properties == null) return false;
            String dataString = properties.toString();
            return dataString.contains("waterlogged=true") || dataString.contains("waterlogged=\"true\"");
        } catch (Exception e) {
            return false;
        }
    }
    
    private void fixWaterloggedState(ItemStack item) {
        if (!item.getType().isBlock()) return;
        
        try {
            if (hasIllegalWaterloggedState(item)) {
                item.unsetData(DataComponentTypes.BLOCK_DATA);
            }
        } catch (Exception ignored) {}
    }

    private boolean hasIllegalPotionEffects(ItemStack item) {
        if (!item.hasData(DataComponentTypes.POTION_CONTENTS)) return false;
        
        try {
            PotionContents contents = item.getData(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) return false;
            
            for (PotionEffect effect : contents.customEffects()) {
                if (effect.getType().equals(PotionEffectType.LUCK)) return true;
                if (effect.getAmplifier() > MAX_LEGAL_AMPLIFIER) return true;
                if (effect.getDuration() > MAX_LEGAL_DURATION) return true;
                if (effect.isInfinite()) return true;
            }
        } catch (Exception e) {
            return false;
        }
        
        return false;
    }
    
    private boolean hasIllegalFoodEffects(ItemStack item) {
        if (!item.hasData(DataComponentTypes.FOOD)) return false;
        
        try {
            Material type = item.getType();
            if (!type.isEdible() && item.hasData(DataComponentTypes.FOOD)) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        
        return false;
    }

    private boolean hasIllegalName(ItemMeta meta) {
        if (!meta.hasCustomName()) return false;
        Component customName = meta.customName();
        if (customName == null) return false;

        if (GlobalUtils.getComponentDepth(customName) > 8) return true;
        
        String json = GsonComponentSerializer.gson().serialize(customName);
        if (json.length() > MAX_NAME_JSON_LENGTH) return true;
        
        String plainText = GlobalUtils.getStringContent(customName);
        if (plainText.length() > MAX_NAME_PLAIN_LENGTH) return true;
        
        return countNestingDepth(json) > 3;
    }

    private int countNestingDepth(String json) {
        int maxDepth = 0;
        int currentDepth = 0;
        for (int i = 0; i < json.length() - 6; i++) {
            if (json.regionMatches(i, "\"extra\"", 0, 7)) {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            }
        }
        return maxDepth;
    }
    private ItemStack sanitizeCrashMap(ItemStack item) {
        if (item.getType() != Material.FILLED_MAP) return item;
        if (!(item.getItemMeta() instanceof MapMeta mm)) return item;
        
        // Strip ViaVersion corruption from PersistentDataContainer
        PersistentDataContainer pdc = mm.getPersistentDataContainer();
        for (NamespacedKey key : new ArrayList<>(pdc.getKeys())) {
            String k = key.toString();
            if (k.contains("VV|") || k.contains("Protocol") || k.contains("original_hashes")) {
                pdc.remove(key);
            }
        }
        
        item.setItemMeta(mm);
        return item;
    }
}
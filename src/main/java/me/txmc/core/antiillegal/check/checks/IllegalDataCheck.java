package me.txmc.core.antiillegal.check.checks;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import me.txmc.core.antiillegal.check.Check;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
                    if (hasIllegalWaterloggedState(meta, type)) return true;
                }
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
                if (maxStack != null && maxStack != type.getMaxStackSize()) {
                    return true;
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
                    
                    if (fixWaterloggedState(meta, type)) {
                        metaChanged = true;
                    }
                    
                    if (metaChanged) item.setItemMeta(meta);
                }
            }

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

            item.unsetData(DataComponentTypes.MAX_STACK_SIZE);

        } catch (Exception ignored) {}
    }
    
    private boolean isContainer(Material type) {
        return type.name().contains("SHULKER_BOX") || type == Material.BUNDLE;
    }
    
    private boolean isPotion(Material type) {
        return type == Material.POTION || type == Material.SPLASH_POTION || 
               type == Material.LINGERING_POTION || type == Material.TIPPED_ARROW;
    }


    private boolean hasIllegalWaterloggedState(ItemMeta meta, Material type) {
        if (!type.isBlock()) return false;
        if (!(meta instanceof BlockStateMeta blockStateMeta)) return false;
        
        try {
            BlockData blockData = type.createBlockData();
            if (!(blockData instanceof Waterlogged)) return false;
            
            if (blockStateMeta.hasBlockState()) {
                BlockData storedData = blockStateMeta.getBlockState().getBlockData();
                if (storedData instanceof Waterlogged waterlogged) {
                    return waterlogged.isWaterlogged();
                }
            }
        } catch (Exception e) {
            return false;
        }
        
        return false;
    }
    
    private boolean fixWaterloggedState(ItemMeta meta, Material type) {
        if (!type.isBlock()) return false;
        if (!(meta instanceof BlockStateMeta blockStateMeta)) return false;
        
        try {
            if (blockStateMeta.hasBlockState()) {
                var blockState = blockStateMeta.getBlockState();
                BlockData storedData = blockState.getBlockData();
                if (storedData instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) {
                    waterlogged.setWaterlogged(false);
                    blockState.setBlockData(waterlogged);
                    blockStateMeta.setBlockState(blockState);
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        
        return false;
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
        
        String json = GsonComponentSerializer.gson().serialize(customName);
        if (json.length() > MAX_NAME_JSON_LENGTH) return true;
        
        String plainText = PlainTextComponentSerializer.plainText().serialize(customName);
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
}
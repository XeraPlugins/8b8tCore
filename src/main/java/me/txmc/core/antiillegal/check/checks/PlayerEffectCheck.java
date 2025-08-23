package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks player effects on players to ensure they don't have illegal values
 * @author MindComplexity (aka Libalpm)
 * @since 2025-08-21
 */
public class PlayerEffectCheck implements Check {

    // Vanilla potion effect limits: Map<EffectType, MaxLevel>
    private static final Map<PotionEffectType, Integer> VANILLA_LEVEL_LIMITS = new HashMap<>();
    
    private static final int MAX_LEGAL_DURATION = 9600; // 8 minutes for vanilla potions
    private static final int MAX_LEGAL_DURATION_EXTENDED = 10800; // 10 minutes (extended potions)
    private static final int MAX_LEGAL_DURATION_SPECIAL = 144000; // 120 minutes (edgecases like Bad Omen)
    
    static {
        // Standard potions (max level 1)
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.NIGHT_VISION, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.INVISIBILITY, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.LUCK, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.SLOW_FALLING, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.WATER_BREATHING, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.FIRE_RESISTANCE, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.WEAKNESS, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.DOLPHINS_GRACE, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.CONDUIT_POWER, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.ABSORPTION, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.DARKNESS, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.OOZING, 1);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.GLOWING, 1);

        /*
        // Bedrock (Not in the Bukkit API as it's only for Java)
        // https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html
        // VANILLA_LEVEL_LIMITS.put(PotionEffectType.Fatal_Poison, 1);
        // Removed in 1.21
        // VANILLA_LEVEL_LIMITS.put(PotionEffectType.RAID_OMEN, 0); // Pillager RAIDS
        // Illegal effects
        // VANILLA_LEVEL_LIMITS.put(PotionEffectType.HEALTH_BOOST, 0);
        */
       
        // Potions with max level 2
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.SPEED, 2);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.STRENGTH, 2);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.REGENERATION, 2);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.POISON, 2);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.JUMP_BOOST, 2);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.HASTE, 2);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.INSTANT_DAMAGE, 2);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.INSTANT_HEALTH, 2);

        // Potions with max level 4
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.SLOWNESS, 4);
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.RESISTANCE, 4);

        
        // Special cases
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.WIND_CHARGED, 0); // Windcharge weapon
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.INFESTED, 0); // Windcharge weapon
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.TRIAL_OMEN, 0); // Trial Chamber
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.WEAVING, 0); // Creates cobwebs on death
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.WITHER, 2); // Wither Boss - Potion of Decay
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.SATURATION, 0); // Only via Golden Apples
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.UNLUCK, 0); // Only via commands
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.LEVITATION, 2); // Shulkers 
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.MINING_FATIGUE, 3); // Elder Guardian
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.HUNGER, 3); // Pufferfish
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.HERO_OF_THE_VILLAGE, 5); // Village curing
        VANILLA_LEVEL_LIMITS.put(PotionEffectType.BAD_OMEN, 5); // Pillager raids


    }

    @Override
    public boolean check(ItemStack item) {
        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return false;
    }

    @Override
    public void fix(ItemStack item) {
        // This function does nothing.
    }

    /**
     * Check if a player has any illegal potion effects
     * @param player The player to check
     * @return true if player has illegal effects, false otherwise
     */
    public boolean checkPlayerEffects(Player player) {
        if (player == null) return false;
        
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (isIllegalEffect(effect)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Remove all illegal potion effects from a player
     * @param player The player to fix
     */
    public void fixPlayerEffects(Player player) {
        if (player == null) return;
        
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (isIllegalEffect(effect)) {
                player.removePotionEffect(effect.getType());
            }
        }
    }

    /**
     * Check if a specific effect is illegal
     * @param effect The potion effect to check
     * @return true if the effect is illegal, false otherwise
     */
    public boolean isIllegalEffect(PotionEffect effect) {
        if (effect == null) return false;
        
        PotionEffectType type = effect.getType();
         // Amplifier starts at 0, so +1 for the actual level
        int level = effect.getAmplifier() + 1;
        int duration = effect.getDuration();
        
        // If the effect type is not in the map, it is illegal or unknown
        // You can add more effects to the map if you want to allow them. 
        if (!VANILLA_LEVEL_LIMITS.containsKey(type)) {
            return true;
        }
        
        int maxAllowedLevel = VANILLA_LEVEL_LIMITS.get(type);
        
        if (maxAllowedLevel == 0) {
            return true;
        }
        
        if (level > maxAllowedLevel) {
            return true;
        }
        
        if (isExcessiveDuration(duration, type)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isExcessiveDuration(int duration, PotionEffectType type) {
        if (type == PotionEffectType.BAD_OMEN) {
            return duration > MAX_LEGAL_DURATION_SPECIAL;
        }
        
        if (duration > MAX_LEGAL_DURATION_EXTENDED) {
            return true;
        }
        return duration > MAX_LEGAL_DURATION;
    }
}

package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.Iterator;

/**
 * @author 254n_m
 * @since 2024/01/15 7:28 PM
 * This file was created as a part of 8b8tCore
 */
public class PotionCheck implements Check {

    private static final int MAX_LEGAL_DURATION = 490 * 20;
    private static final int MAX_LEGAL_AMPLIFIER = 2; // Amplificador máximo permitido

    @Override
    public boolean check(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta)) return false;
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        for (PotionEffect effect : meta.getCustomEffects()) {

            if (isIllegalEffect(effect)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta;
    }

    @Override
    public void fix(ItemStack item) {
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        boolean modified = false;
        Iterator<PotionEffect> iterator = meta.getCustomEffects().iterator();
        while (iterator.hasNext()) {
            PotionEffect effect = iterator.next();
            if (isIllegalEffect(effect)) {
                iterator.remove();
                modified = true;
            }
        }

        if (modified) {
            item.setItemMeta(meta);
        }
    }

    private boolean isIllegalEffect(PotionEffect effect) {
        return effect.getDuration() > MAX_LEGAL_DURATION || effect.getAmplifier() > MAX_LEGAL_AMPLIFIER;

    }
}

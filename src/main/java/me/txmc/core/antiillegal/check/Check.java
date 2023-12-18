package me.txmc.core.antiillegal.check;

import org.bukkit.inventory.ItemStack;

/**
 * @author 254n_m
 * @since 2023/09/18 10:37 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public interface Check {
    /*
        TODO:
             Check if item is over stacked DONE
             Check durability & unbreakable tag DONE
             Check for lore DONE
             Check for attributes DONE
             Ensure that the name is valid DONE
             Check if enchants on item are valid & at the right levels SEMI
             Check if item is illegal on its own (bedrock, end portal frames, barriers etc etc)
             Check potion effects & colour
             Check fireworks for flight illegal flight duration
     */

    /**
     * @param item The item to be checked
     * @return Returns true if item is invalid otherwise false
     */
    boolean check(ItemStack item);

    /**
     * @param item The material in question
     * @return true if this check should check this material otherwise false
     */
    boolean shouldCheck(ItemStack item);

    void fix(ItemStack item);
}

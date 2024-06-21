package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * @author 254n_m
 * @since 2024/03/25 4:02 PM
 * This file was created as a part of 8b8tCore
 */

public class ItemSizeCheck implements Check {
    private int maxSize = 1597152 / 15; //Protocol max divided by 15 IDK if this will break vanilla items but leeeee needs a patch

    public ItemSizeCheck() {
        try {
            Object streamCodec = Class.forName("net.minecraft.world.item.ItemStack").getDeclaredField("OPTIONAL_STREAM_CODEC");
            System.out.println();
        } catch (Throwable t) {
            GlobalUtils.log(Level.SEVERE, "Failed to setup reflection for item size checking. Please see stacktrace below for more details");
            t.printStackTrace();
        }
    }

    @Override
    public boolean check(ItemStack item) {
        return getSize(item) > maxSize;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta;
    }

    @Override
    public void fix(ItemStack item) {
        item.setAmount(0);
    }

    private int getSize(ItemStack itemStack) {
        try {
            Object nmsStack = itemStack.getClass().getField("handle").get(itemStack);

            return -1;
        } catch (Throwable t) {
            if (t instanceof NoSuchFieldException) return -1;
            GlobalUtils.log(Level.WARNING, "Failed to determine size of ItemStack. Please see stacktrace below for more info");
            t.printStackTrace();
            return -1;
        }
    }
}

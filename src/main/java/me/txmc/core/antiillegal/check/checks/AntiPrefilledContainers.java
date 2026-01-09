package me.txmc.core.antiillegal.check.checks;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import me.txmc.core.antiillegal.check.Check;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.List;
import java.util.Set;

/**
 * Prevents players from having items already prefilled in chests this is not possible without modifying the NBT data of the item.
 * @author MindComplexity (Aka Libalpm)
 * @since 2026-01-02
*/
public class AntiPrefilledContainers implements Check {

    private static final Set<Material> CONTAINERS = Set.of(
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.BARREL,
            Material.DISPENSER,
            Material.DROPPER,
            Material.HOPPER,
            Material.CHISELED_BOOKSHELF
    );

    private static final Set<Material> ALL_STORAGE = Set.of(
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
            Material.DISPENSER, Material.DROPPER, Material.HOPPER,
            Material.CHISELED_BOOKSHELF,
            Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX
    );

    @Override
    public boolean check(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!shouldCheck(item)) return false;

        if (item.hasData(DataComponentTypes.CONTAINER)) {
            ItemContainerContents contents = item.getData(DataComponentTypes.CONTAINER);
            if (contents != null) {
                List<ItemStack> items = contents.contents();
                for (ItemStack content : items) {
                    if (content != null && !content.getType().isAir()) {
                        if (ALL_STORAGE.contains(content.getType())) {
                            return true;
                        }
                        return true;
                    }
                }
            }
        }

        if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta meta) {
            if (meta.hasBlockState()) {
                BlockState state = meta.getBlockState();
                if (state instanceof Container container) {
                    for (ItemStack content : container.getInventory().getContents()) {
                        if (content != null && !content.getType().isAir()) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return item != null && CONTAINERS.contains(item.getType());
    }

    @Override
    public void fix(ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        if (item.hasData(DataComponentTypes.CONTAINER)) {
            item.unsetData(DataComponentTypes.CONTAINER);
        }

        if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta meta) {
            if (meta.hasBlockState()) {
                BlockState state = meta.getBlockState();
                if (state instanceof Container container) {
                    container.getInventory().clear();
                    container.update();
                    meta.setBlockState(state);
                    item.setItemMeta(meta);
                }
            }
        }
    }
}
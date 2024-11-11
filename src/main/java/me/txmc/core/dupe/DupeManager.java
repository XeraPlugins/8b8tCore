package me.txmc.core.dupe;

import me.txmc.core.Main;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.List;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 0x15d3v2
 * @since 2024/11/11 111:17 PM
 * This file was created as a fork of 8b8tCore
 */
public class DupeManager {
    private final FileConfiguration config;
    private final Main plugin;

    public DupeManager(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    private final List<Material> nonDupeableItems = new ArrayList<>();

    public void loadNonDupeableItems() {
        nonDupeableItems.clear();
        List<String> nonDupeableItemStrings = config.getStringList("Dupe.NonDupeableItems");
        for (String itemName : nonDupeableItemStrings) {
            Material material = Material.matchMaterial(itemName.toUpperCase());
            if (material != null) {
                nonDupeableItems.add(material);
            }
        }
    }

    public boolean isDupeable(ItemStack item, Entity entity) {
        int droppedItemCount = entity.getNearbyEntities(10, 10, 10)
                .stream()
                .filter(e -> e instanceof org.bukkit.entity.Item)
                .toArray()
                .length;

        if (droppedItemCount >= config.getInt("Dupe.MaxItemsOnGround", 18)) {
            sendPrefixedLocalizedMessage((Player) entity, "framedupe_items_limit");
            return false;
        }

        Material itemType = item.getType();
        if (isShulkerBox(item)) {
            BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();
            if (blockStateMeta != null && blockStateMeta.getBlockState() instanceof ShulkerBox) {
                ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
                Inventory shulkerInventory = shulkerBox.getInventory();

                for (ItemStack shulkerItem : shulkerInventory.getContents()) {
                    if (shulkerItem != null && nonDupeableItems.contains(shulkerItem.getType())) {
                        return false;
                    }
                }
            }
        }

        return !nonDupeableItems.contains(itemType);
    }

    private boolean isShulkerBox(ItemStack item) {
        Material type = item.getType();
        return type == Material.SHULKER_BOX ||
                type == Material.BLACK_SHULKER_BOX ||
                type == Material.BLUE_SHULKER_BOX ||
                type == Material.BROWN_SHULKER_BOX ||
                type == Material.CYAN_SHULKER_BOX ||
                type == Material.GRAY_SHULKER_BOX ||
                type == Material.GREEN_SHULKER_BOX ||
                type == Material.LIGHT_BLUE_SHULKER_BOX ||
                type == Material.LIGHT_GRAY_SHULKER_BOX ||
                type == Material.LIME_SHULKER_BOX ||
                type == Material.MAGENTA_SHULKER_BOX ||
                type == Material.ORANGE_SHULKER_BOX ||
                type == Material.PINK_SHULKER_BOX ||
                type == Material.PURPLE_SHULKER_BOX ||
                type == Material.RED_SHULKER_BOX ||
                type == Material.WHITE_SHULKER_BOX ||
                type == Material.YELLOW_SHULKER_BOX;
    }
}

package me.txmc.core.patch.listeners;

import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Listener for handling the NBT (Named Binary Tag) data of items in the player's inventory.
 *
 * <p>This class is part of the 8b8tCore plugin and is responsible for ensuring that items containing
 * excessive amounts of data are cleared from the player's inventory upon join.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Detecting items in the player's inventory upon join</li>
 *     <li>Calculating the size of each item's metadata</li>
 *     <li>Clearing items that exceed a specific size threshold</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/03 14:49
 */
public class NbtBanPatch implements Listener {
    private final int MAX_ITEM_SIZE_BYTES;

    public NbtBanPatch(JavaPlugin plugin) {
        this.MAX_ITEM_SIZE_BYTES = plugin.getConfig().getInt("NbtBanItemChecker.maxItemSizeAllowed", 50000);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        handleInventory(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        getLogger().info("Se disparó el leave");
        Player player = event.getPlayer();
        handleInventory(player);
    }


    private void handleInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            int itemSize;
            if (item != null) {
                if (item.getType().toString().endsWith("SHULKER_BOX") ||
                        item.getType().toString().endsWith("CHEST") ||
                        item.getType().toString().endsWith("TRAPPED_CHEST") ||
                        item.getType().toString().endsWith("BARREL")) {

                    itemSize = processContainerItem(item);
                } else {
                    itemSize = calculateStringSizeInBytes(item.toString());
                }
                if (itemSize > MAX_ITEM_SIZE_BYTES) {
                    player.getInventory().remove(item);
                    getLogger().warn("Cleared item in " + player.getName() + "'s inventory with size " + itemSize + " bytes named '" + getItemName(item) + "'");
                    sendPrefixedLocalizedMessage(player, "nbtPatch_deleted_item", getItemName(item));
                }
            }
        }
    }

    public static int processContainerItem(ItemStack containerItem) {
        int totalSize = 0;

        BlockStateMeta blockStateMeta = (BlockStateMeta) containerItem.getItemMeta();
        if (blockStateMeta != null && blockStateMeta.getBlockState() instanceof Container) {
            Container container = (Container) blockStateMeta.getBlockState();
            Inventory containerInventory = container.getInventory();

            for (ItemStack item : containerInventory.getContents()) {
                if (item != null) {
                    if (
                            item.getType().toString().endsWith("SHULKER_BOX") ||
                            item.getType().toString().endsWith("CHEST") ||
                            item.getType().toString().endsWith("TRAPPED_CHEST") ||
                            item.getType().toString().endsWith("BARREL")
                    ) {
                        totalSize += processContainerItem(item);
                    } else {
                        totalSize += calculateStringSizeInBytes(item.toString());

                    }
                }
            }
        }
        return totalSize;
    }

    public static int calculateStringSizeInBytes(String data) {
        String plainData = GlobalUtils.getStringContent(Component.text(data));
        byte[] byteArray = plainData.getBytes(StandardCharsets.UTF_8);
        getLogger().info(byteArray.length);
        return byteArray.length;
    }

    public static String getItemName(ItemStack itemStack) {
        if (itemStack == null) {
            return "";
        }

        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            return String.valueOf(itemStack.getItemMeta().getDisplayName());
        } else {
            return itemStack.getType().toString().replace("_", " ").toLowerCase();
        }
    }
}

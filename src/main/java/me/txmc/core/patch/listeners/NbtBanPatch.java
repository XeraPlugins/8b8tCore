package me.txmc.core.patch.listeners;

import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Container;
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

import java.nio.charset.StandardCharsets;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import static org.apache.logging.log4j.LogManager.getLogger;

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
        Player player = event.getPlayer();
        handleInventory(player);
    }

    private void handleInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            int itemSize = isContainerItem(item) ? processContainerItem(item) : calculateStringSizeInBytes(item.toString());

            if (itemSize > MAX_ITEM_SIZE_BYTES) {
                player.getInventory().remove(item);
                getLogger().warn("Cleared item in " + player.getName() + "'s inventory with size " + itemSize + " bytes named '" + getItemName(item) + "'");
                sendPrefixedLocalizedMessage(player, "nbtPatch_deleted_item", getItemName(item));
            }
        }
    }

    public static int processContainerItem(ItemStack containerItem) {
        int totalSize = 0;

        ItemMeta meta = containerItem.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta) {
            if (blockStateMeta.getBlockState() instanceof Container container) {
                Inventory containerInventory = container.getInventory();

                for (ItemStack item : containerInventory.getContents()) {
                    if (item != null) {
                        totalSize += isContainerItem(item)
                                ? processContainerItem(item)
                                : calculateStringSizeInBytes(item.toString());
                    }
                }
            }
        }
        return totalSize;
    }

    public static int calculateStringSizeInBytes(String data) {
        String plainData = GlobalUtils.getStringContent(Component.text(data));
        return plainData.getBytes(StandardCharsets.UTF_8).length;
    }

    public static String getItemName(ItemStack itemStack) {
        try {
            if (itemStack == null) return "";
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            } else {
                return itemStack.getType().toString().replace("_", " ").toLowerCase();
            }
        } catch (Exception ignore) {
            return "";
        }
    }

    private static boolean isContainerItem(ItemStack item) {
        if (item == null) return false;
        String type = item.getType().toString();
        return type.endsWith("SHULKER_BOX") ||
                type.endsWith("CHEST") ||
                type.endsWith("TRAPPED_CHEST") ||
                type.endsWith("BARREL") ||
                type.endsWith("DISPENSER") ||
                type.endsWith("DROPPER") ||
                type.endsWith("HOPPER");
    }
}
package me.txmc.core.patch.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.txmc.core.patch.listeners.NbtBanPatch.calculateStringSizeInBytes;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import static org.apache.logging.log4j.LogManager.getLogger;

public class AntiLagChestListener implements Listener {
    private final int MAX_ITEM_SIZE_BYTES;
    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastOpenTimes = new HashMap<>();

    public AntiLagChestListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.MAX_ITEM_SIZE_BYTES = plugin.getConfig().getInt("AntiLagChest.maxItemSizeAllowed", 50000);
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (!isCheckedInventory(event.getInventory().getType())) return;

        Player player = (Player) event.getPlayer();

        final boolean ENABLED = plugin.getConfig().getBoolean("AntiLagChest.enabled", true);
        final long OPEN_COOLDOWN = plugin.getConfig().getLong("AntiLagChest.openCooldown", 1000L);

        if (!ENABLED) return;

        if (System.currentTimeMillis() - lastOpenTimes.getOrDefault(player.getUniqueId(), 0L) < OPEN_COOLDOWN) {
            if (event.getInventory().getHolder() instanceof ChestedHorse ||
                    event.getInventory().getHolder() instanceof Llama ||
                    event.getInventory().getHolder() instanceof Vehicle ||
                    event.getInventory().getHolder() instanceof Player) {
                return;
            }

            event.setCancelled(true);
            sendPrefixedLocalizedMessage(player, "chest_cooldown");
            return;
        }

        lastOpenTimes.put(player.getUniqueId(), System.currentTimeMillis());

        Inventory inventory = event.getInventory();

        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                int itemSize;
                try {
                    itemSize = processContainerItem(item);
                } catch (Exception e) {
                    itemSize = calculateStringSizeInBytes(item.toString());
                }

                if (itemSize > MAX_ITEM_SIZE_BYTES) {
                    inventory.remove(item);
                    getLogger().warn("Cleared a " + item.getType() + " with a size of " + itemSize + " bytes from a " + inventory.getType() + " triggered by " + player.getName());
                    sendPrefixedLocalizedMessage(player, "nbtPatch_deleted_item", item.getType().name());
                }
            }
        }
    }

    private boolean isCheckedInventory(InventoryType type) {
        return switch (type) {
            case CHEST, HOPPER, SHULKER_BOX, DISPENSER, DROPPER, BARREL -> true;
            default -> false;
        };
    }

    public static int processContainerItem(ItemStack item) {
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return 0;

        BlockState state = meta.getBlockState();
        if (!(state instanceof Container container)) return 0;

        Inventory internalInventory = container.getInventory();
        int totalSize = 0;

        for (ItemStack internalItem : internalInventory.getContents()) {
            if (internalItem != null) {
                totalSize += calculateStringSizeInBytes(internalItem.toString());
            }
        }

        String rawCustomName = container.getCustomName();
        if (rawCustomName != null) {
            try {
                Component jsonName = GsonComponentSerializer.gson().deserialize(rawCustomName);
                totalSize += calculateStringSizeInBytes(jsonName.toString());
            } catch (Exception e) {
                Component legacyName = LegacyComponentSerializer.legacySection().deserialize(rawCustomName);
                totalSize += calculateStringSizeInBytes(legacyName.toString());
            }
        }

        return totalSize;
    }
}

package me.txmc.core.antiillegal.listeners;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import static org.apache.logging.log4j.LogManager.getLogger;
import java.nio.charset.StandardCharsets;

/**
 * @author 254n_m
 * @since 2023/09/20 11:32 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public class AttackListener implements Listener {
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (event.getDamage() > 30D) {
            event.setCancelled(true);
            ((Player) event.getDamager()).damage(event.getDamage());
        }
    }

    /**
     * @author Minelord9000
     * Thanks to Minelord9000 for the
     * book ban patch
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Iterate through the player's inventory
        for (ItemStack item : player.getInventory().getContents()) {

            // Check if the item is a shulker
            if (item != null && item.getType().toString().endsWith("SHULKER_BOX")) {

                BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();

                if (blockStateMeta != null && blockStateMeta.getBlockState() instanceof ShulkerBox) {
                    ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
                    Inventory shulkerInventory = shulkerBox.getInventory();

                    boolean containsBooks = false;

                    for (ItemStack shulkerItem : shulkerInventory.getContents()) {
                        // Check if the shulker has written books
                        if (shulkerItem != null && shulkerItem.getType() == Material.WRITTEN_BOOK) {
                            containsBooks = true;
                            break;
                        }
                    }

                    // The shulker's metadata is converted to a string, and then the size of the string in bytes is calculated
                    int shulkerSize = calculateStringSizeInBytes(item.getItemMeta().toString());
                    getLogger().info("Books Shulker size: " + shulkerSize + " bytes => " + player.getName() + "'s inventory.");

                    // Check if the shulker exceeds a specific number of bytes, if so, its contents are removed
                    if (containsBooks && shulkerSize > 65000) { // Max shulker size in bytes calculated based on tests
                        shulkerInventory.clear();
                        blockStateMeta.setBlockState(shulkerBox);
                        item.setItemMeta(blockStateMeta);
                        getLogger().info("Cleared Shulker Box containing too large books in " + player.getName() + "'s inventory.");
                    }
                }
            }
        }
    }

    // Method to calculate the size of the given string in bytes
    private int calculateStringSizeInBytes(String data)  {
        byte[] byteArray = data.getBytes(StandardCharsets.UTF_8);
        return byteArray.length;
    }
}

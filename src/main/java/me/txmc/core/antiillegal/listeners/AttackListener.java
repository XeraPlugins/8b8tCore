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
        getLogger().info("PlayerQuitEvent triggered for player: " + player.getName());

        // Iterate through the player's inventory
        for (ItemStack item : player.getInventory().getContents()) {
            getLogger().info("" + item.getType().toString());
            if (item != null && item.getType().toString().endsWith("SHULKER_BOX")) {
                getLogger().info("tiene shulkers");
                BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();
                if (blockStateMeta != null && blockStateMeta.getBlockState() instanceof ShulkerBox) {
                    ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
                    Inventory shulkerInventory = shulkerBox.getInventory();

                    boolean containsBooks = false;
                    for (ItemStack shulkerItem : shulkerInventory.getContents()) {
                        if (shulkerItem != null && shulkerItem.getType() == Material.WRITTEN_BOOK) {
                            containsBooks = true;
                            break;
                        }
                    }

                    if (containsBooks) {
                        shulkerInventory.clear();
                        blockStateMeta.setBlockState(shulkerBox);
                        item.setItemMeta(blockStateMeta);
                        getLogger().info("Cleared Shulker Box containing books in " + player.getName() + "'s inventory.");
                    }
                }
            }
    }
}
}

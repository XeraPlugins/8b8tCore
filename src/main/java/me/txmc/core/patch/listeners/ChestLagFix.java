package me.txmc.core.patch.listeners;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import me.txmc.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.txmc.core.util.GlobalUtils.*;

public class ChestLagFix implements Listener {

    public static final HashMap<Player, Integer> chestHashMap = new HashMap<>();

    Main plugin;

    public ChestLagFix(Main plugin) {
        this.plugin = plugin;

    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        int maxSpam = 20;
        String kickMessage = "kick";//I have no idea what their messaging system is like.
        boolean deleteBooks = true;//came by default
        InventoryType inventoryType = event.getInventory().getType();
        Player player = (Player) event.getPlayer();
        if (isCheckedInventory(inventoryType)) {
            chestHashMap.put(player, chestHashMap.getOrDefault(player, 0) + 1);
            if (deleteBooks) {
                deleteNBTBooks(event.getInventory());
            }
            if (chestHashMap.get(player) > maxSpam) {
                GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();

                scheduler.execute(plugin, () -> {
                    executeCommand("minecraft:kick %s", player.getName() + " " + kickMessage);
                });
                chestHashMap.remove(player);
            }
        }
    }



    public boolean isCheckedInventory(InventoryType type) {
        switch (type) {
            case CHEST:
            case HOPPER:
            case ENDER_CHEST:
            case SHULKER_BOX:
            case DISPENSER:
            case DROPPER:
                return true;
        }
        return false;
    }

    private void deleteNBTBooks(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                if (item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.WRITABLE_BOOK) {
                    BookMeta bookMeta = (BookMeta) item.getItemMeta();
                    if (isBanBook(bookMeta)) {
                        inventory.remove(item);
                        Bukkit.getLogger().warning("Removed an NBT book from a chest");
                    }
                }
                if (item.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                    if (blockStateMeta.getBlockState() instanceof ShulkerBox shulker) {
                        for (ItemStack shulkerItem : shulker.getInventory().getContents()) {
                            if (shulkerItem != null) {
                                if (shulkerItem.getType() == Material.WRITTEN_BOOK || shulkerItem.getType() == Material.WRITABLE_BOOK) {
                                    BookMeta book = (BookMeta) shulkerItem.getItemMeta();
                                    if (isBanBook(book)) {
                                        shulker.getInventory().remove(shulkerItem);
                                        Bukkit.getLogger().warning("Removed an NBT book from a shulker box");
                                    }
                                }
                            }
                        }
                        blockStateMeta.setBlockState(shulker);
                        item.setItemMeta(blockStateMeta);
                    }
                }
            }
        }
    }

    private boolean isBanBook(BookMeta book) {
        for (String bookPages : book.getPages()) {
            Pattern pattern = Pattern.compile("[^\\p{L}0-9¿?:.,!¡%$#()']");//This includes all letters in any language, including those with accents, umlauts and tildes.
            Matcher matcher = pattern.matcher(bookPages);
            return matcher.find();
        }
        return false;
    }
}

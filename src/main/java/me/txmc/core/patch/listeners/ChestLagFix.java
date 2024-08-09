package me.txmc.core.patch.listeners;

import me.txmc.core.chat.tasks.AnnouncementTask;
import me.txmc.core.tablist.util.Utils;
import me.txmc.core.util.GlobalUtils;
import me.txmc.core.util.SecondPassEvent;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import  me.txmc.core.Main;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChestLagFix implements Listener {
    public HashMap<Player, Integer> chestHashMap = new HashMap<>();
    Main plugin;

    public ChestLagFix(Main plugin) {
        this.plugin = plugin;

    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        try {
            InventoryType inventoryType = event.getInventory().getType();
            Player player = (Player) event.getPlayer();
            if (isCheckedInventory(inventoryType)) {
                if (chestHashMap.containsKey(player)) {
                    chestHashMap.replace(player, chestHashMap.get(player) + 1);
                } else {
                    chestHashMap.put(player, 1);
                }
                deleteNBTBooks(event.getInventory());
                if (chestHashMap.get(player) > 5) {
                    ((Player) event.getPlayer()).kickPlayer("Chest lag exploit has been patched");
                    chestHashMap.remove(player);
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        GlobalUtils.getRegionTps(event.getPlayer().getLocation()).thenAccept(tps -> {
            if (tps >= 17) {
                chestHashMap.clear();
            }});
    }


    @EventHandler
    public void onSecondPass(SecondPassEvent event) {
        Utils.secondPass(chestHashMap);
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
                        System.out.println("[ChestLagFix] Removed an NBT book from a chest");
                    }
                }
                if (item.getItemMeta() instanceof BlockStateMeta) {
                    BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();
                    if (blockStateMeta.getBlockState() instanceof ShulkerBox) {
                        ShulkerBox shulker = (ShulkerBox) blockStateMeta.getBlockState();
                        for (ItemStack shulkerItem : shulker.getInventory().getContents()) {
                            if (shulkerItem != null) {
                                if (shulkerItem.getType() == Material.WRITTEN_BOOK || shulkerItem.getType() == Material.WRITABLE_BOOK) {
                                    BookMeta book = (BookMeta) shulkerItem.getItemMeta();
                                    if (isBanBook(book)) {
                                        shulker.getInventory().remove(shulkerItem);
                                        System.out.println("[ChestLagFix] Removed an NBT book from a chest");
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
            Pattern pattern = Pattern.compile("[^a-zA-Z0-9]");
            Matcher matcher = pattern.matcher(bookPages);
            return matcher.find();
        }
        return false;
    }
}
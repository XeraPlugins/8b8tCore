package me.txmc.core.dupe.donkeydupe;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Donkey Dupe Logic
 * This file is apart of the 8b8tCore plugin.
 * @author MindComplexity (aka Libalpm)
 * @since 2026/02/04
*/

@RequiredArgsConstructor
public class DonkeyDupe implements Listener {

    private final Main plugin;
    
    private final Map<UUID, ChestedHorse> trackedAnimals = new ConcurrentHashMap<>();

    private final double MIN_DISTANCE = 6.0;

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ChestedHorse animal)) return;

        Player player = (Player) event.getPlayer();

        if (!plugin.getConfig().getBoolean("DonkeyDupe.enabled", true)) return;
        
        boolean votersOnly = plugin.getConfig().getBoolean("DonkeyDupe.votersOnly", false);
        if (votersOnly && !player.hasPermission("8b8tcore.dupe.donkey")) return;

        if (trackedAnimals.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        trackedAnimals.put(player.getUniqueId(), animal);

        Inventory inv = event.getInventory();

        player.getScheduler().runDelayed(plugin, (task) -> {
            if (player.isOnline() && trackedAnimals.containsKey(player.getUniqueId())) {
                if (animal.isValid()) {
                    player.openInventory(inv);
                } else {
                    trackedAnimals.remove(player.getUniqueId());
                }
            }
        }, null, 40L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (event.getInventory().getSize() == 18 && event.getInventory().getHolder() instanceof Player) {
            trackedAnimals.remove(player.getUniqueId());
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ChestedHorse)) return;

        ChestedHorse animal = trackedAnimals.get(player.getUniqueId());

        if (animal == null) return;
        if (holder != animal) return;

        Location pLoc = player.getLocation();
        Location aLoc = animal.getLocation();
        
        double distance = (pLoc.getWorld() == aLoc.getWorld()) ? pLoc.distance(aLoc) : Double.MAX_VALUE;

        if (distance >= MIN_DISTANCE && player.getVehicle() != null) {
            Component title = (animal.customName() != null) ? animal.customName() : Component.text("Entity");
            Inventory fakeInventory = Bukkit.createInventory(player, 18, title);
            fakeInventory.setContents(animal.getInventory().getContents());

            player.getScheduler().runDelayed(plugin, (task) -> {
                if (player.isOnline()) {
                    player.openInventory(fakeInventory);
                }
            }, null, 2L);

        } else {
            trackedAnimals.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null && clickedInventory.getSize() == 18 && clickedInventory.getHolder() instanceof Player) {
            ChestedHorse animal = trackedAnimals.get(player.getUniqueId());
            if (animal == null) return;

            final double MAX_DISTANCE = 128.0;
            
            Location pLoc = player.getLocation();
            Location aLoc = animal.getLocation();
            
            double distance = (pLoc.getWorld() == aLoc.getWorld()) ? pLoc.distance(aLoc) : Double.MAX_VALUE;

            if (distance < MAX_DISTANCE) {
                event.setCancelled(true);
                player.closeInventory();
                trackedAnimals.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        trackedAnimals.remove(event.getPlayer().getUniqueId());
    }
}
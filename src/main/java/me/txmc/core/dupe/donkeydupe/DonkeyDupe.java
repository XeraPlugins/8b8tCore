package me.txmc.core.dupe.donkeydupe;

import me.txmc.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Handles the duplication of items stored in chested animals (e.g., donkeys) when interacting
 * with their inventory, ensuring specific conditions are met.
 *
 * <p>This class is part of the 8b8tCore plugin, which introduces custom mechanics
 * for Minecraft Folia Server.</p>
 *
 * <p>Configuration:</p>
 * <ul>
 *     <li><b>DonkeyDupe.enabled</b>: Enable or disable the donkey dupe functionality.</li>
 *     <li><b>DonkeyDupe.twoPlayerMode</b>: Enable 2-player simultaneous access dupe.</li>
 * </ul>
 *
 * <p>Functionality:</p>
 * <ul>
 *     <li>Creates a fake inventory if the player moves too far from the tracked animal.</li>
 *     <li>Allows two players to simultaneously access the same donkey for duplication.</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/12/06 12:14 AM
 */

public class DonkeyDupe implements Listener {

    private final Main plugin;
    private final Map<UUID, ChestedHorse> trackedAnimals = new HashMap<>();
    private final Map<UUID, Set<UUID>> donkeyViewers = new HashMap<>();
    private final Map<UUID, Map<Integer, ItemStack>> donkeySnapshots = new HashMap<>();

    final double MIN_DISTANCE = 6.0;

    public DonkeyDupe(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ChestedHorse)) return;

        Player player = (Player) event.getPlayer();
        ChestedHorse animal = (ChestedHorse) holder;

        final boolean ENABLED = plugin.getConfig().getBoolean("DonkeyDupe.enabled", true);
        if (!ENABLED) return;

        final boolean VOTERS_ONLY = plugin.getConfig().getBoolean("DonkeyDupe.votersOnly", false);
        if (VOTERS_ONLY && !player.hasPermission("8b8tcore.dupe.donkey")) return;

        final boolean TWO_PLAYER_MODE = plugin.getConfig().getBoolean("DonkeyDupe.twoPlayerMode", true);

        if (TWO_PLAYER_MODE) {
            UUID donkeyId = animal.getUniqueId();
            donkeyViewers.computeIfAbsent(donkeyId, k -> new HashSet<>()).add(player.getUniqueId());
            
            if (!donkeySnapshots.containsKey(donkeyId)) {
                Map<Integer, ItemStack> snapshot = new HashMap<>();
                for (int i = 0; i < animal.getInventory().getSize(); i++) {
                    ItemStack item = animal.getInventory().getItem(i);
                    if (item != null) {
                        snapshot.put(i, item.clone());
                    }
                }
                donkeySnapshots.put(donkeyId, snapshot);
            }
            
            trackedAnimals.put(player.getUniqueId(), animal);
            return;
        }

        if (trackedAnimals.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        trackedAnimals.put(player.getUniqueId(), animal);

        Inventory inv = event.getInventory();

        Bukkit.getRegionScheduler().runDelayed(plugin, player.getLocation(), (task) -> {
            if (player.isOnline() && trackedAnimals.containsKey(player.getUniqueId())) {
                player.openInventory(inv);
            }
        }, 40L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if(event.getInventory().getSize() == 18 && event.getInventory().getHolder() instanceof Player){
            Player player = (Player) event.getPlayer();
            trackedAnimals.remove(player.getUniqueId());
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ChestedHorse)) return;

        Player player = (Player) event.getPlayer();
        ChestedHorse animal = trackedAnimals.get(player.getUniqueId());

        if (animal == null) return;
        if (holder != animal) return;

        final boolean TWO_PLAYER_MODE = plugin.getConfig().getBoolean("DonkeyDupe.twoPlayerMode", true);
        UUID donkeyId = animal.getUniqueId();

        if (TWO_PLAYER_MODE) {
            Set<UUID> viewers = donkeyViewers.get(donkeyId);
            if (viewers != null) {
                viewers.remove(player.getUniqueId());
                
                if (viewers.isEmpty()) {
                    donkeyViewers.remove(donkeyId);
                    
                    Map<Integer, ItemStack> snapshot = donkeySnapshots.get(donkeyId);
                    if (snapshot != null) {
                        for (Map.Entry<Integer, ItemStack> entry : snapshot.entrySet()) {
                            animal.getInventory().setItem(entry.getKey(), entry.getValue().clone());
                        }
                        donkeySnapshots.remove(donkeyId);
                    }
                }
            }
            trackedAnimals.remove(player.getUniqueId());
            return;
        }

        if (animal.getLocation().distance(player.getLocation()) >= MIN_DISTANCE && player.getVehicle() != null ) {
            Inventory fakeInventory = Bukkit.createInventory(player, 18, animal.getName() != null ? animal.getName() : "Entity");
            fakeInventory.setContents(animal.getInventory().getContents());

            Bukkit.getRegionScheduler().runDelayed(plugin, player.getLocation(), (task) -> {
                player.openInventory(fakeInventory);
            }, 2L);

        } else {
            trackedAnimals.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null && clickedInventory.getSize() == 18 && clickedInventory.getHolder() instanceof Player) {
            ChestedHorse animal = trackedAnimals.get(player.getUniqueId());
            if (animal == null) return;

            final double MIN_DISTANCE = 128.0;
            if (animal.getLocation().distance(player.getLocation()) < MIN_DISTANCE) {
                event.setCancelled(true);
                clickedInventory.close();
                trackedAnimals.remove(player.getUniqueId());
            }
        }
    }
            }

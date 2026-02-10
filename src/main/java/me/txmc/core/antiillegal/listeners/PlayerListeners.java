package me.txmc.core.antiillegal.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.AntiIllegalMain;
import me.txmc.core.antiillegal.check.checks.PlayerEffectCheck;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.logging.Level;

import static me.txmc.core.antiillegal.util.Utils.checkStand;
import static me.txmc.core.util.GlobalUtils.executeCommand;

/**
 * @author MindComplexity
 * @since 2026/01/26
 * This file was created as a part of 8b8tAntiIllegal
*/
@RequiredArgsConstructor
public class PlayerListeners implements Listener {
    private final AntiIllegalMain main;

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        PlayerEffectCheck effectCheck = main.getEffectCheck();
        if (effectCheck != null) {
            effectCheck.fixPlayerEffects(player);
        }

        PlayerInventory inv = player.getInventory();
        main.checkFixItem(inv.getItemInMainHand(), null);
        main.checkFixItem(inv.getItemInOffHand(), null);
        
        for (ItemStack armor : inv.getArmorContents()) {
            if (armor != null) main.checkFixItem(armor, null);
        }
        
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEnderChestOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null) main.checkFixItem(item, null);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        main.checkFixItem(event.getItemDrop().getItemStack(), null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOffhand(PlayerSwapHandItemsEvent event) {
        main.checkFixItem(event.getMainHandItem(), event);
        main.checkFixItem(event.getOffHandItem(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        main.checkFixItem(event.getItem().getItemStack(), null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        PlayerInventory inv = event.getPlayer().getInventory();
        ItemStack usedItem = (event.getHand() == EquipmentSlot.OFF_HAND) ? inv.getItemInOffHand() : inv.getItemInMainHand();
        ItemStack otherItem = (event.getHand() == EquipmentSlot.OFF_HAND) ? inv.getItemInMainHand() : inv.getItemInOffHand();

        if (main.checkFixItem(usedItem, event)) {
            event.setCancelled(true);
        }
        main.checkFixItem(otherItem, event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerInventory inv = player.getInventory();
            main.checkFixItem(inv.getItemInMainHand(), event);
            main.checkFixItem(inv.getItemInOffHand(), event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        PlayerInventory inv = event.getPlayer().getInventory();
        ItemStack usedItem = (event.getHand() == EquipmentSlot.OFF_HAND) ? inv.getItemInOffHand() : inv.getItemInMainHand();
        ItemStack otherItem = (event.getHand() == EquipmentSlot.OFF_HAND) ? inv.getItemInMainHand() : inv.getItemInOffHand();

        if (main.checkFixItem(usedItem, event)) {
            event.setCancelled(true);
        }
        main.checkFixItem(otherItem, event);
        if (event.getRightClicked() instanceof ArmorStand stand) {
            checkStand(stand, main);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        main.checkFixItem(event.getPlayerItem(), event);
        main.checkFixItem(event.getArmorStandItem(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof ThrownPotion potion && potion.getShooter() instanceof Player player) {
            main.checkFixItem(potion.getItem(), event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        for (ItemStack item : event.getDrops()) {
            main.checkFixItem(item, null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        for (ItemStack item : event.getPlayer().getInventory().getContents()) {
            if (item != null) main.checkFixItem(item, null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            main.checkFixItem(player.getInventory().getItemInOffHand(), event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            PlayerInventory inv = player.getInventory();
            boolean mainHandIllegal = main.checkFixItem(inv.getItemInMainHand(), event);
            boolean offHandIllegal = main.checkFixItem(inv.getItemInOffHand(), event);
            if (mainHandIllegal || offHandIllegal) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (main.checkFixItem(event.getItemInHand(), event)) {
            event.setCancelled(true);
        }
    }
}

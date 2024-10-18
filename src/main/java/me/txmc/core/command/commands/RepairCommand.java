package me.txmc.core.command.commands;

import me.txmc.core.command.BaseTabCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Allows players to repair the item they are currently holding or all items in their inventory.
 *
 * <p>This class is part of the 8b8tCore plugin, which adds custom functionalities
 * to Minecraft Servers.</p>
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>Command: /repair</li>
 *     <li>Permission: 8b8tcore.command.repair</li>
 *     <li>Description: Repairs the item in the player's hand, or all items in their inventory if "all" is specified.</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/27 09:18 PM
 */

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class RepairCommand extends BaseTabCommand implements Listener {
    private final List<String> repairOptions;
    private final Map<UUID, Long> lastPvPCombatTime;
    private final long combatCooldown;

    public RepairCommand(JavaPlugin plugin) {
        super("repair", "/repair [all]", "8b8tcore.command.repair");
        repairOptions = List.of("all");
        lastPvPCombatTime = new HashMap<>();
        combatCooldown = TimeUnit.SECONDS.toMillis(30);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if (isInCombatCooldown(player)) {
            sendPrefixedLocalizedMessage(player, "repair_fail_while_pvp");
            return;
        }

        if (args.length > 0) {
            String argument = args[0];
            if (argument.equalsIgnoreCase("all")) {
                repairAllItems(player);
            }

        } else {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (itemInHand == null || itemInHand.getType() == Material.AIR) {
                sendPrefixedLocalizedMessage(player, "repair_no_item");
                return;
            }

            if (itemInHand.getType().getMaxDurability() <= 0) {
                sendPrefixedLocalizedMessage(player, "repair_not_repairable");
                return;
            }

            itemInHand.setDurability((short) 0);
            sendPrefixedLocalizedMessage(player, "repair_success");
        }
    }

    private boolean isInCombatCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (lastPvPCombatTime.containsKey(playerId)) {
            long lastCombat = lastPvPCombatTime.get(playerId);
            if ((currentTime - lastCombat) < combatCooldown) {
                return true;
            }
        }
        return false;
    }

    private void repairAllItems(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR && item.getType().getMaxDurability() > 0) {
                item.setDurability((short) 0);
            }
        }
        sendPrefixedLocalizedMessage(player, "repair_all_success");
    }

    @Override
    public List<String> onTab(String[] args) {
        if (args.length == 1) {
            return repairOptions.stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player target) {
            UUID damagerId = damager.getUniqueId();
            UUID targetId = target.getUniqueId();
            long currentTime = System.currentTimeMillis();

            lastPvPCombatTime.put(damagerId, currentTime);
            lastPvPCombatTime.put(targetId, currentTime);
        }
    }
}

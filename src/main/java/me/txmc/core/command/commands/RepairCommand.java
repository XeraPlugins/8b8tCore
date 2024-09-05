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
public class RepairCommand extends BaseTabCommand {
    private final List<String> repairOptions;

    public RepairCommand() {
        super("repair", "/repair [all]", "8b8tcore.command.repair");
        repairOptions = List.of("all");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }

        if (args.length == 0) {
            sendPrefixedLocalizedMessage(player, "repair_no_argument");
            return;
        }

        String argument = args[0];

        if (argument.equalsIgnoreCase("all")) {
            repairAllItems(player);
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
}
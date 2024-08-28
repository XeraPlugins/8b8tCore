package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Allows players to repair the item they are currently holding.
 *
 * <p>This class is part of the 8b8tCore plugin, which adds custom functionalities
 * to Minecraft Servers.</p>
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>Command: /repair</li>
 *     <li>Permission: 8b8tcore.command.repair</li>
 *     <li>Description: Repairs the item in the player's hand, setting its durability to full.</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/27 09:18 PM
 */
public class RepairCommand extends BaseCommand {
    private final Main plugin;

    public RepairCommand(Main plugin) {
        super(
                "repair",
                "/repair",
                "8b8tcore.command.repair",
                "Repair the item in your hand");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }

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
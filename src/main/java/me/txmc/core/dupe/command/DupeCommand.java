package me.txmc.core.dupe.command;

import lombok.AllArgsConstructor;
import me.txmc.core.dupe.DupeSection;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Handles the /dupe command which allows players to duplicate items in their hand.
 *
 * <p>This command is part of the 8b8tCore plugin and can be used by players with the appropriate permissions to duplicate
 * items in their inventory or kill himself.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Checking if the player has the required permission or is an operator</li>
 *     <li>Duplicating the item in hand and dropping it in the world</li>
 *     <li>Enforcing a limit on the number of items that can be present in a chunk</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/01 11:28 AM
 */

@AllArgsConstructor
public class DupeCommand implements CommandExecutor {
    private final DupeSection main;
    private static final String PERMISSION = "8b8tcore.command.dupe";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (player.hasPermission(PERMISSION) || player.isOp()) {
                int copies = 1;

                if (args.length > 0) {
                    try {
                        copies = Integer.parseInt(args[0]);

                        if (copies < 1 || copies > 9) {
                            copies = 1;
                        }
                    } catch (NumberFormatException e) {
                        return true;
                    }
                }

                ItemStack itemInHand = player.getInventory().getItemInMainHand();

                Block block = player.getLocation().getBlock();

                if (itemInHand != null && !itemInHand.getType().isAir()) {
                    for (int i = 0; i < copies; i++) {
                        if (getItemCountInChunk(block) >= 19) {
                            sendPrefixedLocalizedMessage(player, "framedupe_items_limit");
                            return true;
                        }
                        ItemStack duplicatedItem = itemInHand.clone();
                        player.getWorld().dropItemNaturally(player.getLocation(), duplicatedItem);
                    }

                    sendPrefixedLocalizedMessage(player, "dupe_success");
                } else {
                    sendPrefixedLocalizedMessage(player, "dupe_failed");
                }
            } else {
                sendPrefixedLocalizedMessage(player, "dupe_permission");
                player.setHealth(0);
            }
        } else {
            sendMessage(sender, "&3You must be a player to use this command.");
        }
        return true;
    }

    private int getItemCountInChunk(Block block) {
        return (int) Arrays.stream(block.getChunk().getEntities())
                .filter(entity -> entity instanceof Item)
                .map(entity -> (Item) entity)
                .count();
    }
}

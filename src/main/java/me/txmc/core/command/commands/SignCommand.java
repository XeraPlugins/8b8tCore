package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.Container;

import java.util.Collections;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Allows players to sign the item in their hand with their username.
 *
 * <p>This class is part of the 8b8tCore plugin, which adds custom functionalities
 * to Minecraft servers.</p>
 *
 * @author Alejandro Garcia (agarciacorte)
 * @since 2024/11/17
 */
public class SignCommand extends BaseCommand {

    public SignCommand() {
        super(
                "sign",
                "/sign",
                "8b8tcore.command.sign",
                "Sign an item with your username",
                null
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = getSenderAsPlayer(sender).orElse(null);
        if (player != null) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType() == Material.AIR) {
                sendPrefixedLocalizedMessage(player, "sign_fail_no_item");
                return;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
                if (isStorageItem(item)) {
                    signAllItems(item, player.getName());
                    sendPrefixedLocalizedMessage(player, "sign_success");
                }
                return;
            }

            signSingleItem(item, player);
        } else {
            sendErrorMessage(sender, PLAYER_ONLY);
        }
    }

    private boolean isStorageItem(ItemStack item) {
        return item.getType().toString().endsWith("SHULKER_BOX") ||
                item.getType().toString().endsWith("CHEST") ||
                item.getType().toString().endsWith("TRAPPED_CHEST") ||
                item.getType().toString().endsWith("BARREL") ||
                item.getType().toString().endsWith("DISPENSER") ||
                item.getType().toString().endsWith("DROPPER") ||
                item.getType().toString().endsWith("HOPPER");
    }

    private void signAllItems(ItemStack containerItem, String playerName) {

        BlockStateMeta blockStateMeta = (BlockStateMeta) containerItem.getItemMeta();
        if (blockStateMeta != null && blockStateMeta.getBlockState() instanceof Container) {
            Container container = (Container) blockStateMeta.getBlockState();
            Inventory containerInventory = container.getInventory();

            for (ItemStack item : containerInventory.getContents()) {
                if (item != null && !isItemSigned(item)) {
                    signItem(item, playerName);
                }
            }

            blockStateMeta.setBlockState(container);
            containerItem.setItemMeta(blockStateMeta);
        }
    }

    private void signSingleItem(ItemStack item, Player player) {
        if (isItemSigned(item)) {
            sendPrefixedLocalizedMessage(player, "sign_fail_already_signed");
            return;
        }

        signItem(item, player.getName());
        sendPrefixedLocalizedMessage(player, "sign_success");
    }

    private boolean isItemSigned(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            for (String lore : meta.getLore()) {
                if (lore.contains("by @")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void signItem(ItemStack item, String playerName) {
        ItemMeta meta = item.getItemMeta();
        String signature = ChatColor.BLUE + "by " + ChatColor.YELLOW + "@" + playerName;

        if (meta != null) {
            if (meta.hasLore()) {
                meta.getLore().add(signature);
            } else {
                meta.setLore(Collections.singletonList(signature));
            }
            item.setItemMeta(meta);
        }
    }
}

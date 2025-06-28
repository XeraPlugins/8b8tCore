package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class OpenInv extends BaseCommand {
    public OpenInv() {
        super(
                "open",
                "/open <inv | ender> <player>",
                "8b8tcore.command.openinv",
                "Open peoples inventories",
                new String[]{"inv::Open the inventory of the specified player", "ender::Open the ender chest of the specified player"}
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = getSenderAsPlayer(sender).orElse(null);
        if (player != null) {
            if (args.length < 2) {
                sendErrorMessage(sender, getUsage());
            } else {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sendMessage(sender, "&cPlayer&r&a %s&r&c not online&r", args[1]);
                    return;
                }
                switch (args[0]) {
                    case "ender":
                        player.openInventory(target.getEnderChest());
                        break;
                    case "inv":
                    case "inventory":
                        Inventory inv = Bukkit.createInventory(null, 36, target.getName() + "'s Inventory");
                        inv.setContents(Arrays.copyOfRange(target.getInventory().getContents(), 0, 36));
                        player.openInventory(inv);
                        break;
                    default:
                        sendErrorMessage(sender, "Unknown argument " + args[0]);
                }
            }
        } else sendErrorMessage(sender, PLAYER_ONLY);
    }
}
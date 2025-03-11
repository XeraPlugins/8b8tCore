package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class VanishCommand extends BaseCommand {
    private final Main main;

    public VanishCommand(Main main) {
        super(
                "vanish",
                "/vanish",
                "8b8tcore.command.vanish",
                "Toggle vanish mode");
        this.main = main;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cYou must be a player to use this command.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("8b8tcore.command.vanish") && !player.isOp()) {
            sendMessage(player, "&cYou do not have permissions to use this command.");
            return;
        }

        boolean isVanished = main.getVanishedPlayers().contains(player.getUniqueId());

        if (isVanished) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.showPlayer(main, player);
            }
            main.getVanishedPlayers().remove(player.getUniqueId());
            sendPrefixedLocalizedMessage(player, "vanish_false");
        } else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.hasPermission("8b8tcore.command.vanish") || !player.isOp()) {
                    onlinePlayer.hidePlayer(main, player);
                }
            }
            main.getVanishedPlayers().add(player.getUniqueId());
            sendPrefixedLocalizedMessage(player, "vanish_true");
        }
    }
}
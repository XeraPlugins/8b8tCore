package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendMessage;

/**
 * This file is apart of 8b8tcore.
 * @author MindComplexity
 * @since 01/02/2026
 */
public class ToggleAchievementsCommand extends BaseCommand {
    private final GeneralDatabase database;

    public ToggleAchievementsCommand(Main plugin) {
        super("achievements", "/achievements", "8b8tcore.command.badges");
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        database.getPlayerHideBadgesAsync(player.getName()).thenAcceptAsync(current -> {
            boolean newValue = !current;
            database.updateHideBadges(player.getName(), newValue);

            player.getScheduler().run(me.txmc.core.Main.getInstance(), (task) -> {
                if (newValue) {
                    sendMessage(player, "&aYou will no longer see other players achievements.");
                } else {
                    sendMessage(player, "&aYou will now see other players achievements.");
                }
            }, null);
        });
    }
}

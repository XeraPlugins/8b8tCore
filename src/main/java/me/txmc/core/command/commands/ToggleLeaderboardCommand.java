package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class ToggleLeaderboardCommand extends BaseCommand {
    private final GeneralDatabase database;

    public ToggleLeaderboardCommand(Main plugin) {
        super("toggleleaderboard", 
              "/toggleleaderboard [vanilla|custom]", 
              "8b8tcore.command.toggleleaderboard",
              "Toggle between vanilla and custom leaderboard display",
              new String[]{
                  "vanilla::Switch to vanilla leaderboard",
                  "custom::Switch to custom leaderboard"
              });
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if (args.length > 0) {
            String arg = args[0].toLowerCase();
            if (arg.equals("vanilla") || arg.equals("custom")) {
                boolean useVanilla = arg.equals("vanilla");
                database.setVanillaLeaderboard(player.getName(), useVanilla);
                sendPrefixedLocalizedMessage(player, useVanilla ? "leaderboard_vanilla" : "leaderboard_custom");
                return;
            }
        }

        // Toggle current state if no valid argument provided
        boolean current = database.isVanillaLeaderboard(player.getName());
        database.setVanillaLeaderboard(player.getName(), !current);
        sendPrefixedLocalizedMessage(player, !current ? "leaderboard_vanilla" : "leaderboard_custom");
    }
}

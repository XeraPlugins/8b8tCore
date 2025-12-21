package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class LeaderboardCommand extends BaseCommand {
    private final GeneralDatabase database;

    public LeaderboardCommand(Main plugin) {
        super(
            "leaderboard", 
            "/leaderboard <vanilla|custom>", 
            "8b8tcore.command.leaderboard",
            "Switch between vanilla and custom leaderboard display",
            new String[]{
                "vanilla::Switch to vanilla leaderboard",
                "custom::Switch to custom leaderboard"
            }
        );
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if (args.length == 0) {
            boolean current = database.isVanillaLeaderboard(player.getName());
            database.setVanillaLeaderboard(player.getName(), !current);
            String mode = !current ? "vanilla" : "custom";
            sendPrefixedLocalizedMessage(player, "leaderboard_toggled", mode);
        } else {
            String mode = args[0].toLowerCase();
            if (mode.equals("vanilla") || mode.equals("custom")) {
                boolean useVanilla = mode.equals("vanilla");
                database.setVanillaLeaderboard(player.getName(), useVanilla);
                sendPrefixedLocalizedMessage(player, "leaderboard_set", mode);
            } else {
                sendPrefixedLocalizedMessage(player, "leaderboard_usage");
            }
        }
    }
}

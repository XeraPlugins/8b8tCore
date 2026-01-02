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

        Main plugin = (Main) Main.getInstance();
        me.txmc.core.chat.ChatSection chatSection = (me.txmc.core.chat.ChatSection) plugin.getSectionByName("ChatControl");
        if (chatSection == null) return;
        me.txmc.core.chat.ChatInfo info = chatSection.getInfo(player);
        if (info == null) return;

        if (args.length > 0) {
            String arg = args[0].toLowerCase();
            if (arg.equals("vanilla") || arg.equals("custom")) {
                boolean useVanilla = arg.equals("vanilla");
                if (info.isUseVanillaLeaderboard() == useVanilla) {
                    sendPrefixedLocalizedMessage(player, useVanilla ? "leaderboard_vanilla" : "leaderboard_custom");
                    return;
                }
                
                info.setUseVanillaLeaderboard(useVanilla);
                database.setVanillaLeaderboard(player.getName(), useVanilla);
                refreshPlayer(player);
                sendPrefixedLocalizedMessage(player, useVanilla ? "leaderboard_vanilla" : "leaderboard_custom");
                return;
            }
        }

        boolean current = info.isUseVanillaLeaderboard();
        boolean newValue = !current;
        info.setUseVanillaLeaderboard(newValue);
        database.setVanillaLeaderboard(player.getName(), newValue);
        refreshPlayer(player);
        sendPrefixedLocalizedMessage(player, newValue ? "leaderboard_vanilla" : "leaderboard_custom");
    }

    private void refreshPlayer(Player player) {
        Main plugin = (Main) Main.getInstance();
        player.getScheduler().run(plugin, (task) -> {
            me.txmc.core.Section section = plugin.getSectionByName("TabList");
            if (section instanceof me.txmc.core.tablist.TabSection tabSection) {
                tabSection.setTab(player, true);
            }
        }, null);
    }
}

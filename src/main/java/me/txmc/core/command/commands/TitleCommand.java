package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseTabCommand;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.database.GeneralDatabase;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class TitleCommand extends BaseTabCommand {
    private final PrefixManager prefixManager;
    private final GeneralDatabase database;

    public TitleCommand(Main plugin) {
        super("title", "/title <rank|clear>", "8b8tcore.command.title");
        this.prefixManager = new PrefixManager();
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return;
        }

        List<String> availableRanks = prefixManager.getAvailableRanks(player);

        if (args.length == 0) {
            showAvailableRanks(player, availableRanks);
            return;
        }

        String input = args[0].toLowerCase();

        if (input.equals("clear") || input.equals("none")) {
            database.updateSelectedRank(player.getName(), null);
            refreshPlayer(player);
            sendPrefixedLocalizedMessage(player, "title_cleared");
            return;
        }

        selectRank(player, input, availableRanks);
    }

    private void showAvailableRanks(Player player, List<String> availableRanks) {
        if (availableRanks.isEmpty()) {
            sendPrefixedLocalizedMessage(player, "title_no_ranks");
            return;
        }

        String currentRank = database.getSelectedRank(player.getName());
        StringBuilder message = new StringBuilder("&aAvailable ranks: ");

        for (String rank : availableRanks) {
            String rankName = stripPrefix(rank);
            if (rank.equals(currentRank)) {
                message.append("&a[&e").append(rankName).append("&a] ");
            } else {
                message.append("&7").append(rankName).append(" ");
            }
        }

        message.append("\n&7Use &6/title <rank>&7 to select a rank");
        message.append("\n&7Use &6/title clear &7to remove your title");

        player.sendMessage(MiniMessage.miniMessage().deserialize(me.txmc.core.util.GlobalUtils.convertToMiniMessageFormat(message.toString())));
    }

    private void selectRank(Player player, String input, List<String> availableRanks) {
        String targetRank = input.startsWith("8b8tcore.prefix.") 
                ? input 
                : "8b8tcore.prefix." + input;

        if (availableRanks.contains(targetRank)) {
            database.updateSelectedRank(player.getName(), targetRank);
            refreshPlayer(player);
            sendPrefixedLocalizedMessage(player, "title_success", stripPrefix(targetRank));
            return;
        }

        String customPermission = "8b8tcore.prefix.custom";
        if (player.hasPermission(customPermission) && targetRank.startsWith(customPermission)) {
            database.updateSelectedRank(player.getName(), customPermission);
            refreshPlayer(player);
            sendPrefixedLocalizedMessage(player, "title_success", "custom");
            return;
        }

        sendPrefixedLocalizedMessage(player, "title_no_permission_for_rank");
    }

    private void refreshPlayer(Player player) {
        Main plugin = (Main) Main.getInstance();
        player.getScheduler().run(plugin, (task) -> {
           me.txmc.core.Section section = plugin.getSectionByName("TabList");
           if (section instanceof me.txmc.core.tablist.TabSection tabSection) {
               tabSection.setTab(player);
           }
        }, null);
    }

    private String stripPrefix(String rank) {
        return rank.replace("8b8tcore.prefix.", "");
    }

    @Override
    public List<String> onTab(String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("clear");
            completions.add("none");
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
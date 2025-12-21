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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/

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
            CompletableFuture<Void> f1 = database.updateSelectedRank(player.getName(), null);
            CompletableFuture<Void> f2 = database.updateCustomGradient(player.getName(), null);
            CompletableFuture<Void> f3 = database.updateGradientAnimation(player.getName(), "none");
            
            CompletableFuture.allOf(f1, f2, f3).thenRun(() -> refreshPlayer(player));
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
            String rankName = prefixManager.getRankDisplayName(rank);
            if (rank.equals(currentRank)) {
                message.append("&a[&e").append(rankName).append("&a] ");
            } else {
                message.append("&7").append(rankName).append(" ");
            }
        }

        message.append("\n&7Use &6/title <name>&7 to select a title");
        message.append("\n&7Use &6/title clear &7to remove your title");

        player.sendMessage(MiniMessage.miniMessage().deserialize(me.txmc.core.util.GlobalUtils.convertToMiniMessageFormat(message.toString())));
    }

    private void selectRank(Player player, String input, List<String> availableRanks) {
        String inputLower = input.toLowerCase();
        String targetRank = null;

        for (String rank : availableRanks) {
            String friendly = prefixManager.getRankDisplayName(rank).toLowerCase();
            String permission = rank.toLowerCase();
            String stripped = stripPrefix(rank).toLowerCase();
            
            if (inputLower.equals(friendly) || inputLower.equals(permission) || inputLower.equals(stripped)) {
                targetRank = rank;
                break;
            }
        }

        if (targetRank != null) {
            database.updateSelectedRank(player.getName(), targetRank).thenRun(() -> refreshPlayer(player));
            sendPrefixedLocalizedMessage(player, "title_success", prefixManager.getRankDisplayName(targetRank));
            return;
        }

        sendPrefixedLocalizedMessage(player, "title_no_permission_for_rank");
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

    private String stripPrefix(String rank) {
        return rank.replace("8b8tcore.prefix.", "");
    }

    @Override
    public List<String> onTab(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("clear");
            completions.add("none");
            
            prefixManager.getAvailableRanks(player).forEach(rank -> {
                completions.add(prefixManager.getRankDisplayName(rank).toLowerCase());
                String stripped = stripPrefix(rank);
                if (!completions.contains(stripped)) completions.add(stripped);
            });

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
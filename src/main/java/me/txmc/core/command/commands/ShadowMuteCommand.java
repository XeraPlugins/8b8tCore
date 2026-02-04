package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseTabCommand;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class ShadowMuteCommand extends BaseTabCommand {
    private final List<String> shadowmuteOptions;
    private final GeneralDatabase database;

    public ShadowMuteCommand(Main plugin) {
        super(
                "shadowmute",
                "/shadowmute <add | remove> <player> [hours (default 72)]",
                "8b8tcore.command.shadowmute",
                "Mute a player without their knowledge"
        );
        this.database = GeneralDatabase.getInstance();
        shadowmuteOptions = List.of("add", "remove");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "&cSyntax error: /shadowmute <add | remove> <player> [hours (default 72)]");
            return;
        }

        String action = args[0].toLowerCase();
        String playerName = args[1];

        database.isMutedAsync(playerName).thenAccept(isMuted -> {
            if (action.equals("add")) {
                if (isMuted) {
                    sendMessage(sender, "&8" + playerName + " is already muted.");
                    return;
                }

                int hours = 72;
                if (args.length >= 3) {
                    try {
                        hours = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sendMessage(sender, "&cHours argument must be a numeric value.");
                        return;
                    }
                }

                long currentTime = Instant.now().getEpochSecond();
                final long finalMuteUntil = currentTime + (hours * 3600);
                final int finalHours = hours;

                database.mute(playerName, finalMuteUntil).thenRun(() -> {
                    Player target = org.bukkit.Bukkit.getPlayer(playerName);
                    if (target != null) {
                        Main plugin = (Main) Main.getInstance();
                        me.txmc.core.chat.ChatSection chatSection = (me.txmc.core.chat.ChatSection) plugin.getSectionByName("ChatControl");
                        if (chatSection != null) {
                            me.txmc.core.chat.ChatInfo info = chatSection.getInfo(target);
                            if (info != null) info.setMutedUntil(finalMuteUntil);
                        }
                    }
                    sendMessage(sender, "&8" + playerName + " has been shadowmuted for " + finalHours + " hours.");
                });
            } else if (action.equals("remove")) {
                if (!isMuted) {
                    sendMessage(sender, "&c" + playerName + " is not muted.");
                    return;
                }

                database.unmute(playerName).thenRun(() -> {
                    Player target = org.bukkit.Bukkit.getPlayer(playerName);
                    if (target != null) {
                        Main plugin = (Main) Main.getInstance();
                        me.txmc.core.chat.ChatSection chatSection = (me.txmc.core.chat.ChatSection) plugin.getSectionByName("ChatControl");
                        if (chatSection != null) {
                            me.txmc.core.chat.ChatInfo info = chatSection.getInfo(target);
                            if (info != null) info.setMutedUntil(0);
                        }
                    }
                    sendMessage(sender, "&8" + playerName + " has been unmuted.");
                });
            } else {
                sendMessage(sender, "&cInvalid Option: /shadowmute <add | remove> <player> [hours (default 72)]");
            }
        });
    }

    @Override
    public List<String> onTab(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length == 1) {
            return shadowmuteOptions.stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return getOnlinePlayers().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> getOnlinePlayers() {
        return org.bukkit.Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}

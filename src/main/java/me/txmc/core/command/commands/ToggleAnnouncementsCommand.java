package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.chat.ChatInfo;
import me.txmc.core.chat.ChatSection;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.vote.VoteSection;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class ToggleAnnouncementsCommand extends BaseCommand {
    private final GeneralDatabase database;
    private final PrefixManager prefixManager = new PrefixManager();
    private final Main plugin;

    public ToggleAnnouncementsCommand(Main plugin) {
        super("announcements", "/announcements", "8b8tcore.command.announcements");
        this.plugin = plugin;
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        boolean hasRank = prefixManager.hasRank(player);
        var section = plugin.getSectionByName("Vote");
        if (section instanceof VoteSection voteSection) {
            voteSection.hasVoterRoleAsync(player.getName()).thenAccept(hasVoterRole -> {
                if (!hasRank && !hasVoterRole) {
                    sendMessage(player, "&cYou must be a voter or have any rank to toggle announcements.");
                    return;
                }

                database.getPlayerHideAnnouncementsAsync(player.getName()).thenAcceptAsync(current -> {
                    boolean newValue = !current;
                    database.updateHideAnnouncements(player.getName(), newValue);

                    ChatSection chatSection = (ChatSection) plugin.getSectionByName("ChatControl");
                    if (chatSection != null) {
                        ChatInfo info = chatSection.getInfo(player);
                        if (info != null) info.setHideAnnouncements(newValue);
                    }

                    player.getScheduler().run(plugin, (task) -> {
                        if (newValue) {
                            sendMessage(player, "&aYou will no longer see server announcements.");
                        } else {
                            sendMessage(player, "&aYou will now see server announcements.");
                        }
                    }, null);
                });
            });
        } else {
            if (!hasRank) {
                sendMessage(player, "&cYou must have any rank to toggle announcements.");
                return;
            }
            // Handler for when VoteSection is not available (Removes the annoying console warning)
            database.getPlayerHideAnnouncementsAsync(player.getName()).thenAcceptAsync(current -> {
                boolean newValue = !current;
                database.updateHideAnnouncements(player.getName(), newValue);

                ChatSection chatSection = (ChatSection) plugin.getSectionByName("ChatControl");
                if (chatSection != null) {
                    ChatInfo info = chatSection.getInfo(player);
                    if (info != null) info.setHideAnnouncements(newValue);
                }

                player.getScheduler().run(plugin, (task) -> {
                    if (newValue) {
                        sendMessage(player, "&aYou will no longer see server announcements.");
                    } else {
                        sendMessage(player, "&aYou will now see server announcements.");
                    }
                }, null);
            });
        }
    }
}
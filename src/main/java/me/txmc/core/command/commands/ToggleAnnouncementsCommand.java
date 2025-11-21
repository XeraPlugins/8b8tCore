package me.txmc.core.command.commands;

import me.txmc.core.Main;
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
        this.database = new GeneralDatabase(plugin.getDataFolder().getAbsolutePath());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        boolean hasRank = prefixManager.hasRank(player);
        boolean hasVoterRole = false;
        var section = plugin.getSectionByName("Vote");
        if (section instanceof VoteSection voteSection) {
            hasVoterRole = voteSection.hasVoterRole(player.getName());
        }

        if (!hasRank && !hasVoterRole) {
            sendMessage(player, "&cYou must be a voter or have any rank to toggle announcements.");
            return;
        }

        boolean current = database.getPlayerHideAnnouncements(player.getName());
        boolean newValue = !current;
        database.updateHideAnnouncements(player.getName(), newValue);

        if (newValue) {
            sendMessage(player, "&aYou will no longer see server announcements.");
        } else {
            sendMessage(player, "&aYou will now see server announcements.");
        }
    }
}
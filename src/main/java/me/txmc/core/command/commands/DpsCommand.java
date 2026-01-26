package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class DpsCommand extends BaseCommand {
    private final GeneralDatabase database;

    public DpsCommand() {
        super("dps", "/dps", "8b8tcore.command.dps", "Disable phantom spawning for yourself");
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PLAYER_ONLY);
            return;
        }

        database.getPreventPhantomSpawnAsync(player.getName()).thenAccept(current -> {
            boolean newValue = !current;
            database.updatePreventPhantomSpawn(player.getName(), newValue);

            String statusKey = newValue ? "dps_disabled" : "dps_enabled";
            sendPrefixedLocalizedMessage(player, statusKey);
        });
    }
}

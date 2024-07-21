package me.txmc.core.command.commands;

import me.txmc.core.Localization;
import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import static me.txmc.core.util.GlobalUtils.translateChars;

/**
 * <p>This class handles the <code>/tpsinfo</code> command, which provides TPS (Ticks Per Second)
 * and MSPT (Milliseconds per Tick) metrics for the server.</p>
 *
 * <p>When executed by a player with the required permissions, the command retrieves
 * and displays the current server TPS, MSPT, and the lowest TPS.</p>
 *
 * <p><b>Functionality:</b></p>
 * <ul>
 *     <li>Checks if the command sender has the required permissions.</li>
 *     <li>Retrieves the current server TPS and MSPT.</li>
 *     <li>Calculates the lowest TPS among regions with online players.</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/07/18 3:19 PM
 */
public class TpsinfoCommand extends BaseCommand {
    private final Main plugin;
    public TpsinfoCommand(Main plugin) {
        super(
                "tpsinfo",
                "/tpsinfo",
                "8b8tcore.tpsinfo",
                "Show TPS information");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if(!sender.hasPermission("8b8tcore.tpsinfo") && !sender.isOp()){
            sendPrefixedLocalizedMessage(player,"tps_failed");
            return;
        }


        double tps = GlobalUtils.getCurrentRegionTps();
        double mspt = getServerMSPT();
        int onlinePlayers = plugin.getServer().getOnlinePlayers().size();

        Localization loc = Localization.getLocalization(player.locale().getLanguage());
        String tpsMsg = String.join("\n", loc.getStringList("TpsMessage"));
        String strTps = String.format("%.2f", tps);
        String strMspt = String.format("%.2f", mspt);
        getLowestRegionTPS().thenAccept(lowestTPS -> player.sendMessage( translateChars(String.format(tpsMsg, strTps, strMspt, String.format("%.2f", lowestTPS), onlinePlayers))));

    }

    private double getServerMSPT() {
        return 1000.0 / GlobalUtils.getCurrentRegionTps();
    }

    private CompletableFuture<Double> getLowestRegionTPS() {
        List<CompletableFuture<Double>> futures = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            futures.add(GlobalUtils.getRegionTps(player.getLocation()));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    double lowestTPS = Double.MAX_VALUE;
                    for (CompletableFuture<Double> future : futures) {
                        try {
                            double regionTPS = future.get();
                            if (regionTPS < lowestTPS) {
                                lowestTPS = regionTPS;
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            //ignore
                        }
                    }
                    return lowestTPS;
                });
    }
}

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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import static me.txmc.core.util.GlobalUtils.translateChars;

public class TpsinfoCommand extends BaseCommand {
    private final Main plugin;

    public TpsinfoCommand(Main plugin) {
        super("tpsinfo", "/tpsinfo", "8b8tcore.tpsinfo", "Show TPS information");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if (!sender.hasPermission("8b8tcore.tpsinfo") && !sender.isOp()) {
            sendPrefixedLocalizedMessage(player, "tps_failed");
            return;
        }

        player.getScheduler().run(plugin, (st) -> {
            double tps = GlobalUtils.getCurrentRegionTps();
            double mspt = GlobalUtils.getCurrentRegionMspt();
            int onlinePlayers = plugin.getServer().getOnlinePlayers().size();

            Localization loc = Localization.getLocalization(player.locale().getLanguage());
            String tpsMsg = String.join("\n", loc.getStringList("TpsMessage"));
            String strTps = formatTps(tps);
            String strMspt = String.format("%s%.2f", GlobalUtils.getMSPTColor(mspt), mspt);

            getLowestRegionTPS().thenAccept(lowestTPS -> {
                player.getScheduler().run(plugin, (task) -> {
                    if (!player.isOnline()) return;
                    String strLowest = formatTps(lowestTPS);
                    player.sendMessage(translateChars(String.format(tpsMsg, strTps, strMspt, strLowest, onlinePlayers)));
                }, null);
            });
        }, null);
    }

    private String formatTps(double tps) {
        return (tps >= 20.0) 
            ? String.format("%s20.00", GlobalUtils.getTPSColor(tps)) 
            : String.format("%s%.2f", GlobalUtils.getTPSColor(tps), tps);
    }

    private CompletableFuture<Double> getLowestRegionTPS() {
        List<CompletableFuture<Double>> futures = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            
            CompletableFuture<Double> future = new CompletableFuture<>();
            player.getScheduler().run(plugin, (task) -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    GlobalUtils.getRegionTps(p.getLocation()).thenAccept(future::complete);
                } else {
                    future.complete(-1.0);
                }
            }, () -> future.complete(-1.0));
            
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    double lowestTPS = Double.MAX_VALUE;
                    for (CompletableFuture<Double> future : futures) {
                        double regionTPS = future.getNow(-1.0);
                        if (regionTPS > 0 && regionTPS < lowestTPS) {
                            lowestTPS = regionTPS;
                        }
                    }
                    return lowestTPS == Double.MAX_VALUE ? 20.0 : lowestTPS;
                });
    }
}
package me.txmc.core.tpa.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Localization;
import me.txmc.core.tpa.TPASection;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static me.txmc.core.util.GlobalUtils.*;

/**
 * @author 254n_m
 * @since 2023/12/18 4:17 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class TPACommand implements CommandExecutor {
    private final TPASection main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player from) {

            if (args.length == 1) {
                Player to = Bukkit.getPlayer(args[0]);
                if (to == null) {
                    sendPrefixedLocalizedMessage(from, "tpa_player_not_online", args[0]);
                    return true;
                }
                if (to == from) {
                    sendPrefixedLocalizedMessage(from, "tpa_self_tpa");
                    return true;
                }

                int maxDistanceFromSpawn = getMaxDistanceFromSpawn(from);
                if(from.getWorld().getEnvironment() == World.Environment.NETHER){
                    maxDistanceFromSpawn = getMaxDistanceFromSpawn(from) / 8;
                }
                if (isWithinRestrictedArea(from, maxDistanceFromSpawn)) {
                    sendPrefixedLocalizedMessage(from, "tpa_too_close", maxDistanceFromSpawn);
                    return true;
                }

                TextComponent acceptButton = Component.text("ACCEPT").clickEvent(ClickEvent.runCommand("/tpayes " + from.getName()));
                TextComponent denyButton = Component.text("DENY").clickEvent(ClickEvent.runCommand("/tpano " + from.getName()));
                TextReplacementConfig acceptReplace = TextReplacementConfig.builder().match("accept").replacement(acceptButton).build();
                TextReplacementConfig denyReplace = TextReplacementConfig.builder().match("deny").replacement(denyButton).build();

                Localization loc = Localization.getLocalization(to.locale().getLanguage());
                String str = String.format(loc.get("tpa_request_received"), from.getName(), "accept", "deny");
                TextComponent component = (TextComponent) GlobalUtils.translateChars(str).replaceText(acceptReplace).replaceText(denyReplace);
                if (main.hasRequested(from, to) || main.hasHereRequested(from, to)) {
                    sendPrefixedLocalizedMessage(from, "tpa_already_sent", to.getName());
                    return true;
                } else {
                    sendPrefixedComponent(to, component);
                    sendPrefixedLocalizedMessage(from, "tpa_request_sent", to.getName());
                    main.registerRequest(from, to);
                }
            } else sendPrefixedLocalizedMessage(from, "tpa_syntax");
        } else sendMessage(sender, "&cYou must be a player");
        return true;
    }

    private int getMaxDistanceFromSpawn(Player player) {
        Map<String, Integer> distanceMap = Map.of(
                "home.spawn.donator6", 1000,
                "home.spawn.donator5", 2000,
                "home.spawn.donator4", 4000,
                "home.spawn.donator3", 6000,
                "home.spawn.donator2", 8000,
                "home.spawn.donator1", 10000,
                "home.spawn.voter", 15000
        );

        int maxDistance = 20000;

        for (Map.Entry<String, Integer> entry : distanceMap.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                maxDistance = Math.min(maxDistance, entry.getValue());
            }
        }

        return maxDistance;
    }

    private boolean isWithinRestrictedArea(Player player, int range) {
        if (player.isOp()) return false;
        Location loc = player.getLocation();
        return loc.getBlockX() < range && loc.getBlockX() > -range && loc.getBlockZ() < range && loc.getBlockZ() > -range;
    }
}

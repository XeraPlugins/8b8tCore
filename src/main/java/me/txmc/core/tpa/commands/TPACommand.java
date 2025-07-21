package me.txmc.core.tpa.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Localization;
import me.txmc.core.tpa.TPASection;
import me.txmc.core.tpa.ToggledPlayer;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import static me.txmc.core.util.GlobalUtils.*;

/**
 * @author 254n_m
 * @since 2023/12/18 4:17 PM
 * Edited by 5aks
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class TPACommand implements CommandExecutor {
    private final TPASection main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player from)) {
            sendMessage(sender, "&cYou must be a player");
            return true;
        }

        if (args.length < 1) {
            sendPrefixedLocalizedMessage(from, "tpa_syntax");
            return true;
        }
        Player to;
        if (Bukkit.getPlayer(args[0]) == null) {
            sendPrefixedLocalizedMessage(from, "tpa_player_not_online", args[0]);
            return true;
        }
        if(args[0].contains(" ")){
            String temp = args[0].substring(0, args[0].length() - 2);
            to = Bukkit.getPlayer(temp);
        }else to = Bukkit.getPlayer(args[0]);

        if(main.checkToggle(to) || main.checkBlocked(to, from)){
            sendPrefixedLocalizedMessage(from, "tpa_request_blocked");
            return true;
        }


        if (to == from) {
            sendPrefixedLocalizedMessage(from, "tpa_self_tpa");
            return true;
        }

        int maxDistanceFromSpawn = getMaxDistanceFromSpawn(from);
        if (from.getWorld().getEnvironment() == World.Environment.NETHER) {
            maxDistanceFromSpawn /= 8;
        }
        if (isWithinRestrictedArea(from, maxDistanceFromSpawn)) {
            sendPrefixedLocalizedMessage(from, "tpa_too_close", maxDistanceFromSpawn);
            return true;
        }

        // Build ACCEPT / DENY buttons
        TextComponent acceptButton = Component.text("ACCEPT")
                .clickEvent(ClickEvent.runCommand("/tpayes " + from.getName()));
        TextComponent denyButton = Component.text("DENY")
                .clickEvent(ClickEvent.runCommand("/tpano " + from.getName()));
        TextReplacementConfig acceptReplace =
                TextReplacementConfig.builder().match("accept").replacement(acceptButton).build();
        TextReplacementConfig denyReplace =
                TextReplacementConfig.builder().match("deny").replacement(denyButton).build();

        Localization loc = Localization.getLocalization(to.locale().getLanguage());
        String template = String.format(
                loc.get("tpa_request_received"),
                from.getName(), "accept", "deny"
        );
        TextComponent message = (TextComponent) GlobalUtils.translateChars(template)
                .replaceText(acceptReplace)
                .replaceText(denyReplace);

        if (main.hasRequested(from, to) || main.hasHereRequested(from, to)) {
            sendPrefixedLocalizedMessage(from, "tpa_already_sent", to.getName());
        } else {
            sendPrefixedComponent(to, message);
            sendPrefixedLocalizedMessage(from, "tpa_request_sent", to.getName());
            main.registerRequest(from, to);
        }

        return true;
    }

    private int getMaxDistanceFromSpawn(Player player) {
        FileConfiguration cfg = main.plugin.getConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("TPAHOMERADIUS");
        // read the configured default (no hard-coded fallback)
        int maxDistance = sec.getInt("default");

        // scan for any tpa.spawn.<n> permission on the player
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission();
            if (perm.startsWith("tpa.spawn.")) {
                try {
                    int dist = Integer.parseInt(perm.substring("tpa.spawn.".length()));
                    maxDistance = Math.min(maxDistance, dist);
                } catch (NumberFormatException ignored) {}
            }
        }
        return maxDistance;
    }

    private boolean isWithinRestrictedArea(Player player, int range) {
        if (player.isOp()) return false;
        Location loc = player.getLocation();
        return loc.getBlockX() < range && loc.getBlockX() > -range
                && loc.getBlockZ() < range && loc.getBlockZ() > -range;
    }
}
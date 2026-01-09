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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.txmc.core.util.GlobalUtils.*;

/**
 * Combined TPA commands for better maintainability.
 */
public class TPACommands {

    @RequiredArgsConstructor
    public static class TPACommand implements CommandExecutor {
        private final TPASection main;

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player from)) {
                sendMessage(sender, "&cYou must be a player");
                return true;
            }

            if (args.length < 1) {
                sendPrefixedLocalizedMessage(from, "tpa_syntax");
                return true;
            }

            Player to = Bukkit.getPlayer(args[0]);
            if (to == null) {
                sendPrefixedLocalizedMessage(from, "tpa_player_not_online", args[0]);
                return true;
            }

            if (main.checkToggle(to) || main.checkBlocked(to, from)) {
                sendPrefixedLocalizedMessage(from, "tpa_request_blocked");
                return true;
            }

            if (to == from) {
                sendPrefixedLocalizedMessage(from, "tpa_self_tpa");
                return true;
            }

            if (me.txmc.core.util.GlobalUtils.isTeleportRestricted(from)) {
                int range = me.txmc.core.util.GlobalUtils.getTeleportRestrictionRange(from);
                sendPrefixedLocalizedMessage(from, "tpa_too_close", range);
                return true;
            }

            TextComponent acceptButton = Component.text("ACCEPT").clickEvent(ClickEvent.runCommand("/tpayes " + from.getName()));
            TextComponent denyButton = Component.text("DENY").clickEvent(ClickEvent.runCommand("/tpano " + from.getName()));
            TextReplacementConfig acceptReplace = TextReplacementConfig.builder().match("accept").replacement(acceptButton).build();
            TextReplacementConfig denyReplace = TextReplacementConfig.builder().match("deny").replacement(denyButton).build();

            Localization loc = Localization.getLocalization(to.locale().getLanguage());
            String template = String.format(loc.get("tpa_request_received"), from.getName(), "accept", "deny");
            TextComponent message = (TextComponent) GlobalUtils.translateChars(template).replaceText(acceptReplace).replaceText(denyReplace);

            if (main.hasRequested(from, to) || main.hasHereRequested(from, to)) {
                sendPrefixedLocalizedMessage(from, "tpa_already_sent", to.getName());
            } else {
                sendPrefixedComponent(to, message);
                sendPrefixedLocalizedMessage(from, "tpa_request_sent", to.getName());
                main.registerRequest(from, to);
            }
            return true;
        }


    }

    @RequiredArgsConstructor
    public static class TPAAcceptCommand implements CommandExecutor {
        private final TPASection main;

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
            if (sender instanceof Player requested) {
                Player requester = null;
                if (args.length == 0) {
                    requester = main.getLastRequest(requested);
                    if (requester == null) {
                        sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
                        return true;
                    }
                } else if (args.length == 1) {
                    Player potentialRequester = Bukkit.getPlayer(args[0]);
                    if (potentialRequester != null && main.hasRequested(potentialRequester, requested)) {
                        requester = potentialRequester;
                    } else if (potentialRequester != null && main.hasHereRequested(potentialRequester, requested)) {
                        requester = potentialRequester;
                    }
                    if (requester == null) {
                        sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
                        return true;
                    }
                } else {
                    sendPrefixedLocalizedMessage(requested, "tpa_syntax");
                    return true;
                }

                if (main.hasHereRequested(requester, requested)) {
                    if (me.txmc.core.util.GlobalUtils.isTeleportRestricted(requested)) {
                        int range = me.txmc.core.util.GlobalUtils.getTeleportRestrictionRange(requested);
                        sendPrefixedLocalizedMessage(requested, "tpa_too_close", range);
                        return true;
                    }
                    acceptTPAHere(requested, requester);
                } else if (main.hasRequested(requester, requested)) {
                    if (me.txmc.core.util.GlobalUtils.isTeleportRestricted(requester)) {
                        int range = me.txmc.core.util.GlobalUtils.getTeleportRestrictionRange(requester);
                        sendPrefixedLocalizedMessage(requested, "tpa_too_close_other", range, requester.getName());
                        return true;
                    }
                    acceptTPA(requested, requester);
                }
            } else {
                sendMessage(sender, "&cYou must be a player");
            }
            return true;
        }

        private void acceptTPA(Player requested, @Nullable Player requester) {
            if (requester == null || !requester.isOnline() || requested == null || !requested.isOnline()) {
                sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
                main.removeRequest(requester, requested);
                return;
            }
            main.plugin.lastLocations.put(requester.getUniqueId(), requester.getLocation());
            Location targetLoc = requested.getLocation();
            targetLoc.getWorld().getChunkAtAsyncUrgently(targetLoc.getBlock()).thenAccept(chunk -> {
                if (requester.isOnline()) {
                    requester.teleportAsync(targetLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
            });
            sendPrefixedLocalizedMessage(requester, "tpa_teleporting");
            sendPrefixedLocalizedMessage(requested, "tpa_teleporting");
            main.removeRequest(requester, requested);
        }

        private void acceptTPAHere(Player requested, @Nullable Player requester) {
            if (requester == null || !requester.isOnline() || requested == null || !requested.isOnline()) {
                sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
                main.removeHereRequest(requester, requested);
                return;
            }
            main.plugin.lastLocations.put(requested.getUniqueId(), requested.getLocation());
            Location targetLoc = requester.getLocation();
            targetLoc.getWorld().getChunkAtAsyncUrgently(targetLoc.getBlock()).thenAccept(chunk -> {
                if (requested.isOnline()) {
                    requested.teleportAsync(targetLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
            });
            sendPrefixedLocalizedMessage(requester, "tpa_teleporting");
            sendPrefixedLocalizedMessage(requested, "tpa_teleporting");
            main.removeHereRequest(requester, requested);
        }
    }

    @RequiredArgsConstructor
    public static class TPADenyCommand implements CommandExecutor {
        private final TPASection main;

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
            if (sender instanceof Player requested) {
                Player requester = null;
                if (args.length == 0) {
                    requester = main.getLastRequest(requested);
                    denyTPA(requested, requester);
                } else if (args.length == 1) {
                    requester = Bukkit.getPlayer(args[0]);
                    if (requester != null && main.hasRequested(requester, requested)) {
                        denyTPA(requested, requester);
                    } else {
                        sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
                    }
                } else {
                    sendPrefixedLocalizedMessage(requested, "tpa_syntax");
                }
            } else {
                sendMessage(sender, "&cYou must be a player");
            }
            return true;
        }

        private void denyTPA(Player requested, @Nullable Player requester) {
            if (requester == null) {
                sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
                return;
            }
            sendPrefixedLocalizedMessage(requester, "tpa_request_denied_from", requested.getName());
            sendPrefixedLocalizedMessage(requested, "tpa_request_denied_to", requester.getName());
            if (main.hasRequested(requester, requested)) main.removeRequest(requester, requested);
            if (main.hasHereRequested(requester, requested)) main.removeHereRequest(requester, requested);
        }
    }

    @RequiredArgsConstructor
    public static class TPACancelCommand implements CommandExecutor {
        private final TPASection main;

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (sender instanceof Player player) {
                if (args.length == 0) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (main.hasRequested(player, onlinePlayer)) {
                            sendPrefixedLocalizedMessage(player, "tpa_request_cancelled_from", onlinePlayer.getName());
                            sendPrefixedLocalizedMessage(onlinePlayer, "tpa_request_cancelled_to", player.getName());
                            main.removeRequest(player, onlinePlayer);
                        }
                        if (main.hasHereRequested(player, onlinePlayer)) {
                            sendPrefixedLocalizedMessage(player, "tpa_request_cancelled_from", onlinePlayer.getName());
                            sendPrefixedLocalizedMessage(onlinePlayer, "tpa_request_cancelled_to", player.getName());
                            main.removeHereRequest(player, onlinePlayer);
                        }
                    }
                } else if (args.length == 1) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target == null) {
                        sendPrefixedLocalizedMessage(player, "tpa_player_not_online", args[0]);
                        return true;
                    }
                    if (main.hasRequested(player, target)) {
                        main.removeRequest(player, target);
                        sendPrefixedLocalizedMessage(player, "tpa_request_cancelled_from", target.getName());
                        sendPrefixedLocalizedMessage(target, "tpa_request_cancelled_to", player.getName());
                    } else {
                        sendPrefixedLocalizedMessage(player, "tpa_no_request_found");
                    }
                }
            } else {
                sendMessage(sender, "&cYou must be a player");
            }
            return true;
        }
    }

    @RequiredArgsConstructor
    public static class TPAHereCommand implements CommandExecutor {
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
                    if (main.checkToggle(to) || main.checkBlocked(to, from)) {
                        sendPrefixedLocalizedMessage(from, "tpa_request_blocked");
                        return true;
                    }
                    TextComponent acceptButton = Component.text("ACCEPT").clickEvent(ClickEvent.runCommand("/tpayes " + from.getName()));
                    TextComponent denyButton = Component.text("DENY").clickEvent(ClickEvent.runCommand("/tpano " + from.getName()));
                    TextReplacementConfig acceptReplace = TextReplacementConfig.builder().match("accept").replacement(acceptButton).build();
                    TextReplacementConfig denyReplace = TextReplacementConfig.builder().match("deny").replacement(denyButton).build();

                    Localization loc = Localization.getLocalization(to.locale().getLanguage());
                    String str = String.format(loc.get("tpahere_request_received"), from.getName(), "accept", "deny");
                    TextComponent component = (TextComponent) GlobalUtils.translateChars(str).replaceText(acceptReplace).replaceText(denyReplace);

                    if (main.hasRequested(from, to) || main.hasHereRequested(from, to)) {
                        sendPrefixedLocalizedMessage(from, "tpa_already_sent", to.getName());
                    } else {
                        sendPrefixedComponent(to, component);
                        sendPrefixedLocalizedMessage(from, "tpa_request_sent", to.getName());
                        main.registerHereRequest(from, to);
                    }
                } else sendPrefixedLocalizedMessage(from, "tpahere_syntax");
            } else sendMessage(sender, "&cYou must be a player");
            return true;
        }
    }

    @RequiredArgsConstructor
    public static class TPAIgnoreCommand implements CommandExecutor {
        private final TPASection main;

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
            if (sender instanceof Player from) {
                if (args.length == 1) {
                    Player blocked = Bukkit.getPlayer(args[0]);
                    if (blocked == null) {
                        sendPrefixedLocalizedMessage(from, "tpa_player_not_online", args[0]);
                        return true;
                    }
                    if (main.checkBlocked(from, blocked)) {
                        sendPrefixedLocalizedMessage(from, "tpa_requests_unblocked", blocked.getName());
                        main.removeBlockedPlayer(from, blocked);
                    } else {
                        sendPrefixedLocalizedMessage(from, "tpa_requests_blocked", blocked.getName());
                        main.addBlockedPlayer(from, blocked);
                    }
                } else {
                    sendPrefixedLocalizedMessage(from, "tpa_block_syntax");
                }
            } else {
                sendMessage(sender, "You must be a player.");
            }
            return true;
        }
    }

    @RequiredArgsConstructor
    public static class TPAToggleCommand implements CommandExecutor {
        private final TPASection main;

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "&cYou must be a player");
                return true;
            }
            if (args.length != 0) {
                sendPrefixedLocalizedMessage(player, "tpatoggle_syntax");
                return true;
            }
            main.togglePlayer(player);
            if (main.checkToggle(player)) {
                sendPrefixedLocalizedMessage(player, "tpa_requests_disabled");
            } else {
                sendPrefixedLocalizedMessage(player, "tpa_requests_enabled");
            }
            return true;
        }
    }
}

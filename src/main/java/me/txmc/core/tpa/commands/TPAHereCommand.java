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

import static me.txmc.core.util.GlobalUtils.*;

/**
 * Handles the /tpahere command, which allows players to request teleportation of another player to their current location.
 *
 * <p>This command is part of the TPA system in the 8b8tCore plugin and facilitates requesting a player to teleport to the
 * command issuer's location.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Sending a tpahere request to a specified player</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/09/08 02:37 PM
 */
@RequiredArgsConstructor
public class TPAHereCommand implements CommandExecutor {
    private final TPASection main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player from) {
            if (args.length == 1) {
                if (Bukkit.getPlayer(args[0]) == null){
                    sendPrefixedLocalizedMessage(from, "tpa_player_not_online", args[0]);
                    return true;
                }
                Player to = Bukkit.getPlayer(args[0]);
                if (to == from) {
                    sendPrefixedLocalizedMessage(from, "tpa_self_tpa");
                    return true;
                }
                if(main.checkToggle(to)){
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
                    return true;
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

package me.txmc.core.customexperience.util;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages and retrieves the appropriate prefix for players based on their permissions.
 *
 * <p>This class is part of the 8b8tCore plugin, which provides custom functionalities
 * including permission-based prefixes for players.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Retrieving the prefix assigned to a player based on their permissions</li>
 *     <li>Prefixes are configured to represent different player ranks or roles</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/11 03:42 PM
 */
public class PrefixManager {

    private static final List<String> PREFIX_HIERARCHY = Arrays.asList(
            "*",
            "8b8tcore.prefix.dev",
            "8b8tcore.prefix.bot",
            "8b8tcore.prefix.donator5",
            "8b8tcore.prefix.donator4",
            "8b8tcore.prefix.donator3",
            "8b8tcore.prefix.donator2",
            "8b8tcore.prefix.donator1"
    );

    private static final Map<String, String> PREFIXES = new HashMap<>();
    static {
        PREFIXES.put("*", "&7[&5OWNER&a✔&7]&r");
        PREFIXES.put("8b8tcore.prefix.dev", "&7[&9DEV&a✔&7]&r");
        PREFIXES.put("8b8tcore.prefix.bot", "&7[&2BOT&7]&r");
        PREFIXES.put("8b8tcore.prefix.donator5", "&7[&bDONOR5&7]&r");
        PREFIXES.put("8b8tcore.prefix.donator4", "&7[&2DONOR4&7]&r");
        PREFIXES.put("8b8tcore.prefix.donator3", "&7[&1DONOR3&7]&r");
        PREFIXES.put("8b8tcore.prefix.donator2", "&7[&6DONOR2&7]&r");
        PREFIXES.put("8b8tcore.prefix.donator1", "&7[&8DONOR1&7]&r");
    }

    public String getPrefix(Player player) {
        String highestPrefix = "";

        for (String permission : PREFIX_HIERARCHY) {
            if (player.hasPermission(permission)) {
                highestPrefix = PREFIXES.get(permission) + " ";
                break;
            }
        }
        return highestPrefix;
    }
}
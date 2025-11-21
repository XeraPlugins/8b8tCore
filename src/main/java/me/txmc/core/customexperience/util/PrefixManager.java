package me.txmc.core.customexperience.util;

import org.bukkit.entity.Player;
import me.txmc.core.Main;
import me.txmc.core.database.GeneralDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.*;

/**
 * Manages and retrieves the appropriate prefix for players based on their permissions,
 * including the ability to animate prefixes.
 *
 * <p>This class is part of the 8b8tCore plugin, which provides custom functionalities
 * including permission-based prefixes for players, with animated tags.</p>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/11 03:42 PM
 */
public class PrefixManager {

    private static final Map<String, String> PREFIXES = new HashMap<>();
    private static double globalAnimationIndex = 0.0;
    private static GeneralDatabase database;

    private static final List<String> PREFIX_HIERARCHY = Arrays.asList(
            "8b8tcore.prefix.owner",
            "8b8tcore.prefix.dev",
            "8b8tcore.prefix.bot",
            "8b8tcore.prefix.youtuber",
            "8b8tcore.prefix.thetroll2001",
            "8b8tcore.prefix.donator6",
            "8b8tcore.prefix.donator5",
            "8b8tcore.prefix.donator4",
            "8b8tcore.prefix.donator3",
            "8b8tcore.prefix.donator2",
            "8b8tcore.prefix.donator1"


    );

    static {
        PREFIXES.put("8b8tcore.prefix.owner", "<gradient:#BA3FFC:#D9ADFD:#BA3FFC:%s>[OWNER<green>✔</green>]</gradient>");
        PREFIXES.put("8b8tcore.prefix.dev", "<gradient:#5555FF:#9492F5:#5555FF:%s>[DEV<green>✔</green>]</gradient>");
        PREFIXES.put("8b8tcore.prefix.bot", "<gradient:#00AA00:#6ef385:#00AA00:%s>[BOT]</gradient>");
        PREFIXES.put("8b8tcore.prefix.youtuber", "<gradient:#8B0000:#FF0000:#8B0000:%s>[Youtuber]</gradient>");
        PREFIXES.put("8b8tcore.prefix.thetroll2001", "<gradient:#FF0000:#FF7F00:#FFFF00:#00FF00:#0000FF:#4B0082:#8F00FF:#4B0082:#0000FF:#00FF00:#FFFF00:#FF7F00:#FF0000:%s>[Troll]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator6", "<gradient:#9145b0:#ceafcf:#9145b0:%s>[Ultra]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator5", "<gradient:#a18800:#ecc700:#a18800:%s>[Pro+]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator4", "<gradient:#ef9a98:#f7cac9:#ef9a98:%s>[Pro]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator3", "<gradient:#999B9B:#ced5d5:#999B9B:%s>[Mini]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator2", "<gradient:#CD7F32:#fc9937:#CD7F32:%s>[SE]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator1", "<gradient:#20252c:#62666c:#20252c:%s>[Basic]</gradient>");
    }

    public String getPrefix(Player player) {
        if (database == null && Main.getInstance() != null) {
            database = new GeneralDatabase(Main.getInstance().getDataFolder().getAbsolutePath());
        }

        if (database != null && database.getPlayerHidePrefix(player.getName())) {
            return "";
        }

        String highestPermission = "";

        for (String permission : PREFIX_HIERARCHY) {
            if (player.hasPermission(permission)) {
                highestPermission = permission;
                break;
            }
        }

        if (highestPermission.isEmpty()) {
            return "";
        }

        String basePrefix = PREFIXES.get(highestPermission);
        globalAnimationIndex = (globalAnimationIndex + 0.1) % 1.0;
        String animatedPrefix = basePrefix.replace("%s", String.format("%.1f", globalAnimationIndex));

        return animatedPrefix + " ";
    }

    public boolean hasRank(Player player) {
        for (String permission : PREFIX_HIERARCHY) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
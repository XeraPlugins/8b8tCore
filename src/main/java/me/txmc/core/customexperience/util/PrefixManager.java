package me.txmc.core.customexperience.util;

import org.bukkit.entity.Player;

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

    private static final Map<String, List<String>> ANIMATED_PREFIXES = new HashMap<>();

    private final Map<UUID, Integer> playerFrameIndex = new HashMap<>();

    static {
        ANIMATED_PREFIXES.put("*", Arrays.asList(
                "&#AB08FB[O&#BA3FFCWN&#CA76FCER&#3dd13d✔&#D9ADFD]&r",
                "&#BA3FFC[O&#AB08FBWN&#BA3FFCER&#3dd13d✔&#CA76FC]&r",
                "&#CA76FC[O&#BA3FFCWN&#AB08FBER&#3dd13d✔&#BA3FFC]&r",
                "&#D9ADFD[O&#CA76FCWN&#BA3FFCER&#3dd13d✔&#AB08FB]&r",
                "&#CA76FC[O&#D9ADFDWN&#CA76FCER&#3dd13d✔&#BA3FFC]&r",
                "&#BA3FFC[O&#CA76FCWN&#D9ADFDER&#3dd13d✔&#CA76FC]&r"
        ));
        ANIMATED_PREFIXES.put("8b8tcore.prefix.dev", Arrays.asList(
                "&#5555FF[D&#7574FAEV&#3dd13d✔&#9492F5]&r",
                "&#7574FA[D&#5555FFEV&#3dd13d✔&#7574FA]&r",
                "&#9492F5[D&#7574FAEV&#3dd13d✔&#5555FF]&r"
        ));
        ANIMATED_PREFIXES.put("8b8tcore.prefix.bot", Arrays.asList(
                  "&#00AA00[B&#36CE50OT&#36CE50]&r",
                  "&#36CE50[B&#00AA00OT&#36CE50]&r",
                  "&#36CE50[B&#36CE50OT&#00AA00]&r"
        ));
        ANIMATED_PREFIXES.put("8b8tcore.prefix.donator5", Arrays.asList(
                  "&#00AAA3[D&#1CC6C2ON&#39E3E0OR&#55FFFF5]&r",
                  "&#1CC6C2[D&#00AAA3ON&#1CC6C2OR&#39E3E05]&r",
                  "&#39E3E0[D&#1CC6C2ON&#00AAA3OR&#1CC6C25]&r",
                  "&#55FFFF[D&#39E3E0ON&#1CC6C2OR&#00AAA35]&r",
                  "&#39E3E0[D&#55FFFFON&#39E3E0OR&#1CC6C25]&r",
                  "&#1CC6C2[D&#39E3E0ON&#55FFFFOR&#39E3E05]&r"
        ));
        ANIMATED_PREFIXES.put("8b8tcore.prefix.donator4", Arrays.asList(
                  "&#00AA00[D&#1CC62BON&#39E357OR&#55FF824]&r",
                  "&#1CC62B[D&#00AA00ON&#1CC62BOR&#39E3574]&r",
                  "&#39E357[D&#1CC62BON&#00AA00OR&#1CC62B4]&r",
                  "&#55FF82[D&#39E357ON&#1CC62BOR&#00AA004]&r",
                  "&#39E357[D&#55FF82ON&#39E357OR&#1CC62B4]&r",
                  "&#1CC62B[D&#39E357ON&#55FF82OR&#39E3574]&r"
        ));
        ANIMATED_PREFIXES.put("8b8tcore.prefix.donator3", Arrays.asList(
                  "&#0000AA[D&#0020ABON&#003FACOR&#005FAD3]&r",
                  "&#0020AB[D&#0000AAON&#0020ABOR&#003FAC3]&r",
                  "&#003FAC[D&#0020ABON&#0000AAOR&#0020AB3]&r",
                  "&#005FAD[D&#003FACON&#0020ABOR&#0000AA3]&r",
                  "&#003FAC[D&#005FADON&#003FACOR&#0020AB3]&r",
                  "&#0020AB[D&#003FACON&#005FADOR&#003FAC3]&r"
        ));
        ANIMATED_PREFIXES.put("8b8tcore.prefix.donator2", Arrays.asList(
                  "&#FFAA00[D&#FFBA31ON&#FFCB61OR&#FFDB922]&r",
                  "&#FFBA31[D&#FFAA00ON&#FFBA31OR&#FFCB612]&r",
                  "&#FFCB61[D&#FFBA31ON&#FFAA00OR&#FFBA312]&r",
                  "&#FFDB92[D&#FFCB61ON&#FFBA31OR&#FFAA002]&r",
                  "&#FFCB61[D&#FFDB92ON&#FFCB61OR&#FFBA312]&r",
                  "&#FFBA31[D&#FFCB61ON&#FFDB92OR&#FFCB612]&r"
        ));
        ANIMATED_PREFIXES.put("8b8tcore.prefix.donator1", Arrays.asList(
                  "&#AAAAAA[D&#C2C2C2ON&#DBDBDBOR&#F3F3F31]&r",
                  "&#C2C2C2[D&#AAAAAAON&#C2C2C2OR&#DBDBDB1]&r",
                  "&#DBDBDB[D&#C2C2C2ON&#AAAAAAOR&#C2C2C21]&r",
                  "&#F3F3F3[D&#DBDBDBON&#C2C2C2OR&#AAAAAA1]&r",
                  "&#DBDBDB[D&#F3F3F3ON&#DBDBDBOR&#C2C2C21]&r",
                  "&#C2C2C2[D&#DBDBDBON&#F3F3F3OR&#DBDBDB1]&r"
        ));
    }

    public String getPrefix(Player player) {
        String highestPermission = "";

        for (String permission : PREFIX_HIERARCHY) {
            if (player.hasPermission(permission)) {
                highestPermission = permission;
                break;
            }
        }

        List<String> frames = ANIMATED_PREFIXES.getOrDefault(highestPermission, Arrays.asList("","",""));

        UUID playerId = player.getUniqueId();
        int frameIndex = playerFrameIndex.getOrDefault(playerId, 0);

        String currentFrame = frames.get(frameIndex);

        frameIndex = (frameIndex + 1) % frames.size();
        playerFrameIndex.put(playerId, frameIndex);

        if(currentFrame.isEmpty()) return "";
        return currentFrame + " ";
    }
}
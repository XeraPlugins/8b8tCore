package me.txmc.core.customexperience.util;

import org.bukkit.entity.Player;
import me.txmc.core.Main;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.util.GradientAnimator;

import java.util.HashMap;
import java.util.Map;
import java.util.*;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
 */
public class PrefixManager {

    private static final Map<String, String> PREFIXES = new HashMap<>();
    private final GeneralDatabase database;

    private static final List<String> PREFIX_HIERARCHY = Arrays.asList(
            "8b8tcore.prefix.owner",
            "8b8tcore.prefix.dev",
            "8b8tcore.prefix.bot",
            "8b8tcore.prefix.youtuber",
            "8b8tcore.prefix.thetroll2001",
            "8b8tcore.prefix.qtdonkey",
            "8b8tcore.prefix.orasan080",
            "8b8tcore.prefix.lucky2007",
            "8b8tcore.prefix.xmas2025",
            "8b8tcore.prefix.donator6",
            "8b8tcore.prefix.donator5",
            "8b8tcore.prefix.donator4",
            "8b8tcore.prefix.donator3",
            "8b8tcore.prefix.donator2",
            "8b8tcore.prefix.donator1",
            "8b8tcore.prefix.custom"
    );

    private static final Map<String, String> RANK_NAMES = new HashMap<>();

    public PrefixManager() {
        this.database = GeneralDatabase.getInstance();
        
        PREFIXES.put("8b8tcore.prefix.owner", "<gradient:#a860ff:#743ad5:#d0a2ff:%s>[OWNER<green>✔</green>]</gradient>");
        PREFIXES.put("8b8tcore.prefix.dev", "<gradient:#00d2ff:#3a7bd5:#00d2ff:%s>[DEV<green>✔</green>]</gradient>");
        PREFIXES.put("8b8tcore.prefix.bot", "<gradient:#11998e:#38ef7d:#11998e:%s>[BOT]</gradient>");
        PREFIXES.put("8b8tcore.prefix.youtuber", "<gradient:#cb2d3e:#ef473a:#cb2d3e:%s>[Youtuber]</gradient>");
        PREFIXES.put("8b8tcore.prefix.thetroll2001", "<gradient:#FF0000:#FF7F00:#FFFF00:#00FF00:#0000FF:#4B0082:#8F00FF:%s>[Troll]</gradient>");
        PREFIXES.put("8b8tcore.prefix.qtdonkey", "<gradient:#f8ff00:#3ad59f:%s>[Television]</gradient>");
        PREFIXES.put("8b8tcore.prefix.orasan080", "<gradient:#9bc4e2:#e9eff5:#9bc4e2:%s>[lurker]</gradient>");
        PREFIXES.put("8b8tcore.prefix.lucky2007", "<gradient:#1f4037:#99f2c8:%s>[Addict]</gradient>");
        PREFIXES.put("8b8tcore.prefix.xmas2025", "<gradient:#8B0000:#FFD700:#006400:%s>[XMAS2025]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator6", "<gradient:#8e2de2:#4a00e0:#8e2de2:%s>[Ultra]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator5", "<gradient:#f2994a:#f2c94c:#f2994a:%s>[Pro+]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator4", "<gradient:#ee9ca7:#ffdde1:#ee9ca7:%s>[Pro]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator3", "<gradient:#bdc3c7:#2c3e50:#bdc3c7:%s>[Mini]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator2", "<gradient:#833ab4:#fd1d1d:#fcb045:%s>[SE]</gradient>");
        PREFIXES.put("8b8tcore.prefix.donator1", "<gradient:#434343:#000000:#434343:%s>[Basic]</gradient>");
        PREFIXES.put("8b8tcore.prefix.custom", "<gradient:%g:%s>[Custom]</gradient>");

        RANK_NAMES.put("8b8tcore.prefix.owner", "Owner");
        RANK_NAMES.put("8b8tcore.prefix.dev", "Dev");
        RANK_NAMES.put("8b8tcore.prefix.bot", "Bot");
        RANK_NAMES.put("8b8tcore.prefix.youtuber", "Youtuber");
        RANK_NAMES.put("8b8tcore.prefix.thetroll2001", "Troll");
        RANK_NAMES.put("8b8tcore.prefix.qtdonkey", "Television");
        RANK_NAMES.put("8b8tcore.prefix.lucky2007", "Addict");
        RANK_NAMES.put("8b8tcore.prefix.donator6", "Ultra");
        RANK_NAMES.put("8b8tcore.prefix.donator5", "Pro+");
        RANK_NAMES.put("8b8tcore.prefix.donator4", "Pro");
        RANK_NAMES.put("8b8tcore.prefix.donator3", "Mini");
        RANK_NAMES.put("8b8tcore.prefix.donator2", "SE");
        RANK_NAMES.put("8b8tcore.prefix.donator1", "Basic");
        RANK_NAMES.put("8b8tcore.prefix.custom", "Custom");
    }

    public String getRankDisplayName(String permission) {
        return RANK_NAMES.getOrDefault(permission, permission.replace("8b8tcore.prefix.", ""));
    }

    public String getPrefix(Player player) {
        if (database != null && database.getPlayerHidePrefix(player.getName())) return "";

        String highestPermission = "";
        String selectedRank = database.getSelectedRank(player.getName());

        if (selectedRank != null && (player.hasPermission(selectedRank) || player.isOp())) {
            highestPermission = selectedRank;
        } else {
            for (String permission : PREFIX_HIERARCHY) {
                if (player.hasPermission(permission) || (player.isOp() && !permission.equals("8b8tcore.prefix.custom"))) {
                    highestPermission = permission;
                    break;
                }
            }
        }

        if (highestPermission.isEmpty()) return "";

        String basePrefix = PREFIXES.get(highestPermission);
        if (basePrefix == null) return "";
        long tick = GradientAnimator.getAnimationTick();
        String customGradient = database.getPrefixGradient(player.getName());

        if (customGradient != null && !customGradient.isEmpty()) {
            String animationType = database.getPrefixAnimation(player.getName());
            int speed = database.getPrefixSpeed(player.getName());
            String finalGradient = GradientAnimator.applyAnimation(customGradient, animationType, speed, tick);

            String body = basePrefix;
            if (basePrefix.contains("<gradient:")) {
                int firstClose = basePrefix.indexOf('>');
                if (firstClose != -1) body = basePrefix.substring(firstClose + 1).replace("</gradient>", "");
            }

            String decorationsStr = database.getPrefixDecorations(player.getName());
            StringBuilder result = new StringBuilder();
            if (decorationsStr != null && !decorationsStr.isEmpty()) {
                for (String decoration : decorationsStr.split(",")) result.append("<").append(decoration.trim()).append(">");
            }

            boolean isGradient = finalGradient.contains(":") && finalGradient.indexOf('#') != finalGradient.lastIndexOf('#');
            if (isGradient && finalGradient.indexOf('#') == finalGradient.lastIndexOf('#')) isGradient = false;

            if (isGradient) {
                result.append("<gradient:").append(finalGradient).append(">").append(body).append("</gradient>");
            } else {
                result.append("<color:").append(finalGradient.split(":")[0]).append(">").append(body).append("</color>");
            }

            if (decorationsStr != null && !decorationsStr.isEmpty()) {
                String[] decorations = decorationsStr.split(",");
                for (int i = decorations.length - 1; i >= 0; i--) result.append("</").append(decorations[i].trim()).append(">");
            }

            return result.toString().replace("%s", "0.0").replace("%g", "") + " ";
        }

        // improved SDR color blending
        double t = (tick * 0.05) % 2.0;
        if (t > 1.0) t = 2.0 - t;
        double phase = t * t * (3 - 2 * t);
        String phaseStr = String.format("%.2f", phase);
        
        return basePrefix.replace("%s", phaseStr).replace("%g", "#FFFFFF:#AAAAAA:#FFFFFF") + " ";
    }

    public List<String> getAvailableRanks(Player player) {
        List<String> available = new ArrayList<>();
        boolean isOp = player.isOp();
        for (String permission : PREFIX_HIERARCHY) {
            if (isOp || player.hasPermission(permission)) available.add(permission);
        }
        return available;
    }

    public boolean hasRank(Player player) {
        if (player.isOp()) return true;
        for (String permission : PREFIX_HIERARCHY) {
            if (player.hasPermission(permission)) return true;
        }
        return false;
    }
}
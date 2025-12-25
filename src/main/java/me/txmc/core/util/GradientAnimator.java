package me.txmc.core.util;

import org.bukkit.Bukkit;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/

public class GradientAnimator {
    public static String applyAnimation(String baseGradient, String animationType, int speed, long tick) {
        if (animationType == null || animationType.equalsIgnoreCase("none") || animationType.isEmpty()) {
            return baseGradient;
        }

        double phase = 0.0;
        double normalizedSpeed = speed / 5.0;

        switch (animationType.toLowerCase()) {
            case "wave":
                phase = Math.sin(tick * 0.1 * normalizedSpeed);
                break;
            case "pulse":
                phase = Math.sin(tick * 0.05 * normalizedSpeed);
                break;
            case "rainbow":
                phase = (tick * 0.05 * normalizedSpeed) % 2.0 - 1.0;
                break;
            case "shift":
                phase = Math.cos(tick * 0.08 * normalizedSpeed);
                break;
            default:
                return baseGradient;
        }

        if (phase < -1.0)
            phase = -1.0;
        if (phase > 1.0)
            phase = 1.0;

        return String.format("%s:%.2f", baseGradient, phase);
    }

    public static long getAnimationTick() {
        return System.currentTimeMillis() / 50;
    }
}

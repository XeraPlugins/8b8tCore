package me.txmc.core.util;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
 */
public class GradientAnimator {
    public static String applyAnimation(String baseGradient, String animationType, int speed, long tick) {
        if (animationType == null || animationType.equalsIgnoreCase("none") || animationType.isEmpty() || !baseGradient.contains(":") || baseGradient.toLowerCase().contains("tobias:")) {
            return baseGradient;
        }

        double phase = 0.0;
        double effectiveSpeed = Math.min(5, speed);
        double normalizedSpeed = effectiveSpeed / 5.0;
        double t = tick * normalizedSpeed;

        switch (animationType.toLowerCase()) {
            case "wave":
                phase = (Math.sin(t * 0.15) + 1.0) / 2.0;
                break;
            case "pulse":
                phase = (Math.sin(t * 0.05) + 1.0) / 2.0;
                break;
            case "smooth":
                double st = (t * 0.06) % 2.0;
                if (st > 1.0) st = 2.0 - st;
                phase = st < 0.5 ? 2 * st * st : 1 - Math.pow(-2 * st + 2, 2) / 2;
                break;
            case "saturate":
                phase = (Math.sin(t * 0.12) + Math.sin(t * 0.24)) / 4.0 + 0.5;
                break;
            case "bounce":
                phase = Math.abs(Math.sin(t * 0.2));
                break;
            case "billboard":
                phase = Math.floor(((t * 0.05) % 1.0) * 5) / 4.0;
                break;
            case "sweep":
                double sw = (t * 0.07) % 2.0;
                if (sw > 1.0) sw = 2.0 - sw;
                phase = sw * sw * (3 - 2 * sw);
                break;
            case "shimmer":
                double sh = (t * 0.15) % 3.0;
                phase = Math.min(1.0, Math.max(0.0, sh - 1.0));
                break;
            default:
                return baseGradient;
        }

        phase = Math.max(0.0, Math.min(1.0, phase));
        double rounded = Math.round(phase * 100.0) / 100.0;
        return baseGradient + ":" + rounded;
    }

    public static long getAnimationTick() {
        return System.currentTimeMillis() / 50;
    }
}

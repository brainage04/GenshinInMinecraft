package io.github.brainage04.genshininminecraft.ui;

/** Loader- and Minecraft-independent display rules; cooldowns never claim readiness early. */
public final class HudFormatting {
    private HudFormatting() {}

    public static String cooldown(int remainingFrames) {
        if (remainingFrames <= 0) return "";
        long tenths = (remainingFrames + 5L) / 6;
        if (tenths >= 100) return Long.toString((remainingFrames + 59L) / 60);
        return tenths / 10 + "." + tenths % 10;
    }

    /** HP and energy bars use this same clamped fraction, including an empty/invalid maximum. */
    public static double fraction(double current, double maximum) {
        if (!Double.isFinite(current) || !Double.isFinite(maximum) || maximum <= 0) return 0;
        return Math.clamp(current / maximum, 0, 1);
    }

    /** Three billboard-pixel lanes separate a talent hit, reaction and absorbed hit. */
    public static int damageLane(int sequence) {
        return (Math.floorMod(sequence, 3) - 1) * 32;
    }
}

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

    /** The covered (unavailable) share of a cooldown, not the elapsed/ready share. */
    public static double sweepFraction(int remainingFrames, int durationFrames) {
        if (remainingFrames <= 0) return 0;
        return durationFrames <= 0 ? 1 : fraction(remainingFrames, durationFrames);
    }

    /** Clockwise from twelve o'clock; used to build original scanline sweep masks once. */
    public static boolean radialCovered(double x, double y, double fraction) {
        if (fraction <= 0) return false;
        if (fraction >= 1) return true;
        double turn = Math.atan2(x, -y) / (2 * Math.PI);
        if (turn < 0) turn += 1;
        return turn < fraction;
    }

    public static boolean partyUnavailable(boolean active, double hp, int remainingFrames, boolean blocked) {
        return hp <= 0 || !active && (remainingFrames > 0 || blocked);
    }

    /** Three billboard-pixel lanes separate a talent hit, reaction and absorbed hit. */
    public static int damageLane(int sequence) {
        return (Math.floorMod(sequence, 3) - 1) * 32;
    }
}

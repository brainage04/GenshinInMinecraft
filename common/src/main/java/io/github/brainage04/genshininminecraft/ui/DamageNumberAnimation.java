package io.github.brainage04.genshininminecraft.ui;

/** Pure original combat-text motion, in seconds and billboard pixels; never affects damage. */
public final class DamageNumberAnimation {
    public static final double LIFETIME_SECONDS = 1.1;
    public static final double POP_SECONDS = .15;
    public static final double CRITICAL_SCALE = 1.4;
    public static final double REACTION_SCALE = .85;
    public static final double REACTION_Y = -16;
    public static final double STACK_SPACING = 48;
    private DamageNumberAnimation() {}

    /** Fast expansion, one overshoot, then settle by 150 ms. */
    public static double pop(double age) {
        double t = Math.clamp(age / POP_SECONDS, 0, 1);
        if (t < .5) {
            double ease = 1 - (1 - 2 * t) * (1 - 2 * t);
            return .6 + .68 * ease;
        }
        double settle = (t - .5) * 2;
        return 1 + .28 * (1 - settle) * (1 - settle);
    }
    public static double alpha(double age) {
        double t = Math.clamp((age - .65) / (LIFETIME_SECONDS - .65), 0, 1);
        return 1 - t * t * (3 - 2 * t);
    }
    public static double rise(double age) {
        double t = Math.clamp(age / LIFETIME_SECONDS, 0, 1);
        return 22 * (2 * t - t * t);
    }
    /** Integer hash gives repeatable, independent cosmetic drift, without touching combat RNG. */
    public static double drift(int sequence) {
        int hash = sequence * 0x9e3779b9;
        hash ^= hash >>> 16;
        return ((hash & 0xffff) / 65535.0 * 2 - 1) * 7;
    }
    public static double horizontal(double age, double drift) {
        if (age <= 0) return 0;
        return drift * Math.clamp(age / LIFETIME_SECONDS, 0, 1);
    }
    /** Every live hit on one target has a unique vertical slot; no three-hit wrap/overlap. */
    public static double stackY(int slot) { return Math.max(0, slot) * STACK_SPACING; }
    public static double stackX(int slot) { return (Math.floorMod(slot, 3) - 1) * 12; }
    /** Constant angular text size from near camera to the existing 64-block feedback cutoff. */
    public static float distanceScale(double distance) { return (float) (.0055 * Math.max(1, distance)); }
}

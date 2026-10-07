package io.github.brainage04.genshininminecraft.rules;

/** Ordinary club Fighter references; rounded level-indexed inputs are sourced in hilichurls.md. */
public final class HilichurlProfile {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 20;
    public static final int DEFAULT_CAMP_LEVEL = 8; // WL0 adaptation, awaiting owner camp observations.
    private static final double[] HP_BY_LEVEL = {
        72.917, 92.628, 114.394, 138.215, 164.093, 192.050, 222.075, 237.820, 261.734, 286.604,
        326.883, 368.759, 412.257, 460.521, 510.583, 562.470, 624.878, 679.928, 736.002, 885.200
    };
    public static final double RESISTANCE = .10;
    public static final double PLACEHOLDER_ENDURANCE = 100;
    public static final double ADAPTED_ATK = 120; // Enemy ATK curve has not been sourced.
    public static final double CLUB_MULTIPLIER = 1; // hilichurls.md: ordinary Fighter club hit 100%.
    public static final double STARTER_PLAYER_RESISTANCE = 0;
    public static double maxHp(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) throw new IllegalArgumentException("Sourced hilichurl levels are 1–20");
        return HP_BY_LEVEL[level - 1];
    }
    public static double defense(int level) { return 5 * level + 500; }
    private HilichurlProfile() {}
}

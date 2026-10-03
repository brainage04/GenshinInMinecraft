package io.github.brainage04.genshininminecraft.rules;

/** Level-20 ordinary club Fighter reference; unknown/rounded inputs are recorded in fidelity.md. */
public final class HilichurlProfile {
    public static final int LEVEL = 20;
    public static final double MAX_HP = 885.200; // hilichurls.md: rounded Type-1 Lv20 baseline.
    public static final double DEF = 5 * LEVEL + 500;
    public static final double RESISTANCE = .10;
    public static final double PLACEHOLDER_ENDURANCE = 100;
    public static final double ADAPTED_ATK = 120; // Enemy ATK curve has not been sourced.
    public static final double CLUB_MULTIPLIER = 1; // hilichurls.md: ordinary Fighter club hit 100%.
    public static final double STARTER_PLAYER_RESISTANCE = 0;
    private HilichurlProfile() {}
}

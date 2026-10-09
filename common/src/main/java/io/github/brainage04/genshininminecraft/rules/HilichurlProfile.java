package io.github.brainage04.genshininminecraft.rules;

/** Ordinary club Fighter numeric profile: pinned 7.1 inputs are sourced in datamine.md. */
public final class HilichurlProfile {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 20;
    public static final int DEFAULT_CAMP_LEVEL = 8; // WL0 adaptation: dump lacks identifiable camp spawns.
    public static final double BASE_HP = 13.584;
    public static final double BASE_ATK = 22.608;
    public static final double BASE_DEF = 500;
    public static final double RESISTANCE = .10;
    public static final double PLACEHOLDER_ENDURANCE = 100;
    public static final double CLUB_MULTIPLIER = 1; // hilichurls.md: ordinary Fighter club hit 100%.
    public static final double STARTER_PLAYER_RESISTANCE = 0;
    private static void checkLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) throw new IllegalArgumentException("Camp levels are 1–20");
    }
    public static double maxHp(int level) {
        checkLevel(level);
        return MonsterCurve.hp(BASE_HP, level);
    }
    public static double attack(int level) {
        checkLevel(level);
        return MonsterCurve.attack(BASE_ATK, level);
    }
    public static double defense(int level) {
        checkLevel(level);
        return MonsterCurve.defense(BASE_DEF, level);
    }
    private HilichurlProfile() {}
}

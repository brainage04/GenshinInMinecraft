package io.github.brainage04.genshininminecraft.rules;

import java.util.Map;

/** Named level-20/20, refinement-1 weapons; published inputs are in damage.md. No artifacts. */
public final class StarterLoadout {
    public static final int LEVEL = 20;
    public static final int REFINEMENT = 1;
    public static final Weapon HARBINGER_OF_DAWN = new Weapon("Harbinger of Dawn", 94, 0, 0, .18, 0);
    public static final Weapon SLINGSHOT = new Weapon("Slingshot", 86, 0, .12, 0, 0);
    public static final Weapon THRILLING_TALES = new Weapon("Thrilling Tales of Dragon Slayers", 94, .135, 0, 0, .24);
    public static final double HARBINGER_HP_THRESHOLD = .90;
    public static final double HARBINGER_CRIT_RATE = .14;
    public static final long SLINGSHOT_CLOSE_FRAMES = 18; // 0.3 seconds at 60 fps, from firing to impact.
    public static final double SLINGSHOT_CLOSE_BONUS = .36;
    public static final double SLINGSHOT_DISTANT_BONUS = -.10;
    public static final long THRILLING_TALES_DURATION_FRAMES = 600;
    public static final long THRILLING_TALES_COOLDOWN_FRAMES = 1200;

    public record Weapon(String name, double baseAtk, double hpPercent, double critRate,
            double critDamage, double switchAtkPercent) {}

    private StarterLoadout() {}
    public static Weapon weapon(CharacterBaseStats.Character character) {
        return switch (character) {
            case TRAVELER_ANEMO, KAEYA -> HARBINGER_OF_DAWN;
            case AMBER -> SLINGSHOT;
            case LISA -> THRILLING_TALES;
        };
    }
    public static double normalChargedBonus(CharacterBaseStats.Character character, long flightFrames) {
        if (flightFrames < 0) throw new IllegalArgumentException("Negative arrow flight time");
        return weapon(character) == SLINGSHOT
                ? (flightFrames <= SLINGSHOT_CLOSE_FRAMES ? SLINGSHOT_CLOSE_BONUS : SLINGSHOT_DISTANT_BONUS) : 0;
    }
    public static Stats stats(CharacterBaseStats.Character character, boolean highHp, double atkPercent, double attackBonus) {
        var base = CharacterBaseStats.at(character, LEVEL);
        var weapon = weapon(character);
        double passiveCrit = weapon == HARBINGER_OF_DAWN && highHp ? HARBINGER_CRIT_RATE : 0;
        return new Stats(LEVEL, base.hp(), base.atk(), base.def(), weapon.baseAtk(),
                weapon.hpPercent(), atkPercent, 0, 0, 0, 0,
                .05 + weapon.critRate() + passiveCrit, .5 + weapon.critDamage(), 0, attackBonus, Map.of());
    }
}

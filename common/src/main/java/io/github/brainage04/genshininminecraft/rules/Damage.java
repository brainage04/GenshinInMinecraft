package io.github.brainage04.genshininminecraft.rules;

import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Character talent, incoming enemy talent, and ordinary transformative damage formulas. */
public final class Damage {
    public enum ScalingStat { ATK, DEF, HP, EM }
    public enum CritMode { EXPECTED, ROLL, NON_CRIT, CRIT }

    // spec/mechanics/damage.md: Character Level Multiplier K(L), never enemy/environment K(L).
    private static final double[] LEVEL_BASE = {
        17.165605, 18.535048, 19.904854, 21.274903, 22.6454,
        24.649613, 26.640643, 28.868587, 31.367679, 34.143343,
        37.201, 40.66, 44.446668, 48.563519, 53.74848,
        59.081897, 64.420047, 69.724455, 75.123137, 80.584775
    };
    public static final double LEVEL_90_BASE = 1446.853458;
    public static final double INCOMING_DEF_BASE = 500; // damage.md records the source's 500/501 conflict.
    public static final double INCOMING_DEF_PER_LEVEL = 5;

    private Damage() {}

    public static double levelBase(int level) {
        if (level == 90) return LEVEL_90_BASE;
        if (level < 1 || level > 20) {
            throw new IllegalArgumentException("Only sourced levels 1–20 and 90 are available");
        }
        return LEVEL_BASE[level - 1];
    }

    public static double defenseMultiplier(int characterLevel, int enemyLevel,
            double reduction, double ignore) {
        double character = characterLevel + 100.0;
        return character / (character + (enemyLevel + 100.0)
                * (1 - Math.min(reduction, 0.9)) * (1 - ignore));
    }

    /** Incoming attacks use the defender's actual DEF, not the outgoing equal-level multiplier. */
    public static double enemyDamage(double attack, double multiplier, int enemyLevel,
            double characterDefense, double characterResistance) {
        double defenseScale = INCOMING_DEF_PER_LEVEL * enemyLevel + INCOMING_DEF_BASE;
        return attack * multiplier * defenseScale / (characterDefense + defenseScale)
                * resistanceMultiplier(characterResistance);
    }

    public static double resistanceMultiplier(double resistance) {
        if (resistance < 0) return 1 - resistance / 2;
        if (resistance < 0.75) return 1 - resistance;
        return 1 / (4 * resistance + 1);
    }

    public static double critMultiplier(double rate, double damage, CritMode mode,
            RandomGenerator random) {
        double chance = Math.clamp(rate, 0, 1);
        return switch (mode) {
            case EXPECTED -> 1 + chance * damage;
            case NON_CRIT -> 1;
            case CRIT -> 1 + damage;
            case ROLL -> Objects.requireNonNull(random, "random").nextDouble() < chance
                    ? 1 + damage : 1;
        };
    }

    public static double amplifyingMultiplier(Reaction reaction, double em, double reactionBonus) {
        if (reaction == null || !reaction.amplifying()) return 1;
        return reaction.amplificationBase() * (1 + 2.78 * em / (1400 + em) + reactionBonus);
    }

    /** ATK-scaled default. Resistance is the effective resistance of this hit's element. */
    public static double talentDamage(Stats stats, double talentMultiplier, Element element,
            int enemyLevel, double defReduction, double defIgnore, double resistance,
            Reaction reaction, double reactionBonus, CritMode critMode, RandomGenerator random) {
        return talentDamage(stats, talentMultiplier, ScalingStat.ATK, element, enemyLevel,
                defReduction, defIgnore, resistance, reaction, reactionBonus, critMode, random);
    }

    public static double talentDamage(Stats stats, double talentMultiplier, ScalingStat scaling,
            Element element, int enemyLevel, double defReduction, double defIgnore,
            double resistance, Reaction reaction, double reactionBonus,
            CritMode critMode, RandomGenerator random) {
        double stat = switch (scaling) {
            case ATK -> stats.atk();
            case DEF -> stats.def();
            case HP -> stats.hp();
            case EM -> stats.elementalMastery();
        };
        return talentMultiplier * stat * (1 + stats.damageBonus(element))
                * critMultiplier(stats.critRate(), stats.critDamage(), critMode, random)
                * defenseMultiplier(stats.level(), enemyLevel, defReduction, defIgnore)
                * resistanceMultiplier(resistance)
                * amplifyingMultiplier(reaction, stats.elementalMastery(), reactionBonus);
    }

    /** Caller looks up RES using Reaction.damageElement(type, swirledElement); no ordinary bonuses. */
    public static double transformativeDamage(Reaction.Type type, int level, double em,
            double reactionBonus, double reactionElementResistance) {
        return Reaction.transformativeCoefficient(type) * levelBase(level)
                * (1 + 16 * em / (2000 + em) + reactionBonus)
                * resistanceMultiplier(reactionElementResistance);
    }

    /** Select the reaction's RES element, independent of the triggering talent's element. */
    public static double transformativeDamage(Reaction reaction, Stats owner, double reactionBonus,
            Map<Element, Double> effectiveResistances) {
        Element damageElement = Reaction.damageElement(reaction.type(), reaction.auraElement());
        return transformativeDamage(reaction.type(), owner.level(), owner.elementalMastery(),
                reactionBonus, effectiveResistances.getOrDefault(damageElement, 0.0));
    }
}

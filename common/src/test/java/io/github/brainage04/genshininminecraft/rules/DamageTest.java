package io.github.brainage04.genshininminecraft.rules;

import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageTest {
    private static final double EPS = 1e-9;

    @ParameterizedTest
    @CsvSource({"1,1,0,0,0.5", "20,20,0,0,0.5", "90,90,0,0,0.5",
            "1,20,0,0,0.4570135746606335", "20,1,0,0,0.5429864253393665",
            "20,20,0.5,0.5,0.8", "90,90,1,0,0.9090909090909091"})
    void defenseGolden(int character, int enemy, double reduction, double ignore, double expected) {
        // spec/mechanics/damage.md: Enemy DEF multiplier, including 90% reduction cap.
        assertEquals(expected, Damage.defenseMultiplier(character, enemy, reduction, ignore), EPS);
    }

    @ParameterizedTest
    @CsvSource({"-0.4,1.2", "0,1", "0.1,0.9", "0.749,0.251", "0.75,0.25", "1,0.2", "1.5,0.14285714285714285"})
    void resistanceBoundaries(double resistance, double expected) {
        // spec/mechanics/damage.md: Enemy RES multiplier piecewise table.
        assertEquals(expected, Damage.resistanceMultiplier(resistance), EPS);
    }

    @ParameterizedTest
    @CsvSource({"-0.1,0.5,1", "0,0.5,1", "0.05,0.5,1.025", "0.5,1,1.5", "1,0.5,1.5", "1.2,0.5,1.5"})
    void expectedCrit(double rate, double damage, double expected) {
        // spec/mechanics/damage.md: Crit expectation with effective probability clamped [0,1].
        assertEquals(expected, Damage.critMultiplier(rate, damage, Damage.CritMode.EXPECTED, null), EPS);
    }

    @Test
    void seededRollProducesIndividualCritsRatherThanExpectedValue() {
        // spec/mechanics/damage.md: Crit multiplier is either 1 or 1+CD. Java Random(0)'s
        // first six uniform draws are .7309,.2405,.6374,.5504,.5975,.3332.
        Random random = new Random(0);
        double[] expected = {1, 1.5, 1, 1, 1, 1.5};
        for (double value : expected) {
            assertEquals(value, Damage.critMultiplier(0.5, 0.5, Damage.CritMode.ROLL, random), EPS);
        }
        assertEquals(1, Damage.critMultiplier(-1, 0.5, Damage.CritMode.ROLL, random), EPS);
        assertEquals(1.5, Damage.critMultiplier(2, 0.5, Damage.CritMode.ROLL, random), EPS);
    }

    @ParameterizedTest
    @CsvSource({"MELT,PYRO,CRYO,2", "MELT,CRYO,PYRO,1.5",
            "VAPORIZE,HYDRO,PYRO,2", "VAPORIZE,PYRO,HYDRO,1.5"})
    void amplifyingDirections(Reaction.Type type, Element trigger, Element aura, double factor) {
        // spec/mechanics/damage.md: Amplifying directions and EM formula. At EM=1400
        // the bonus is 1.39; reaction bonus remains separate from ordinary DMG Bonus.
        Reaction reaction = new Reaction(type, 0, trigger, aura);
        assertEquals(factor, Damage.amplifyingMultiplier(reaction, 0, 0), EPS);
        assertEquals(factor * 2.59, Damage.amplifyingMultiplier(reaction, 1400, 0.2), EPS);
    }

    @ParameterizedTest
    @CsvSource({"SWIRL,1,17.165605,0.6", "OVERLOADED,1,17.165605,2.75",
            "SUPERCONDUCT,1,17.165605,1.5", "ELECTRO_CHARGED,1,17.165605,2", "SHATTER,1,17.165605,3",
            "SWIRL,20,80.584775,0.6", "OVERLOADED,20,80.584775,2.75",
            "SUPERCONDUCT,20,80.584775,1.5", "ELECTRO_CHARGED,20,80.584775,2", "SHATTER,20,80.584775,3",
            "SWIRL,90,1446.853458,0.6", "OVERLOADED,90,1446.853458,2.75",
            "SUPERCONDUCT,90,1446.853458,1.5", "ELECTRO_CHARGED,90,1446.853458,2", "SHATTER,90,1446.853458,3"})
    void transformativeGoldenWithResistance(Reaction.Type type, int level, double publishedBase, double coefficient) {
        // spec/mechanics/damage.md: Ordinary transformative coefficients + K(L) table.
        // Independently evaluate the sourced inputs, never rounded reference-display DMG.
        assertEquals(coefficient * publishedBase * 0.9,
                Damage.transformativeDamage(type, level, 0, 0, 0.1), EPS);
        // EM=2000 -> +8; reaction bonus +.25; negative RES=-.4 -> 1.2.
        assertEquals(coefficient * publishedBase * 9.25 * 1.2,
                Damage.transformativeDamage(type, level, 2000, 0.25, -0.4), EPS);
    }

    @ParameterizedTest
    @CsvSource({"1,17.165605", "2,18.535048", "3,19.904854", "4,21.274903", "5,22.6454",
            "6,24.649613", "7,26.640643", "8,28.868587", "9,31.367679", "10,34.143343",
            "11,37.201", "12,40.66", "13,44.446668", "14,48.563519", "15,53.74848",
            "16,59.081897", "17,64.420047", "18,69.724455", "19,75.123137", "20,80.584775", "90,1446.853458"})
    void publishedLevelBases(int level, double expected) {
        // spec/mechanics/damage.md: K(L) (not rounded reaction damage or enemy K(L)).
        assertEquals(expected, Damage.levelBase(level), EPS);
    }

    @Test
    void talentDamageUsesWeaponInsideAtkPercentAndRetainsAmpCritDefRes() {
        // spec/mechanics/damage.md: Stat construction + outgoing talent formula. Synthetic
        // exact stats avoid the explicitly unreliable rounded starter base-stat decimals.
        Stats stats = new Stats(20, 1000, 100, 100, 100, 0.1, 0.5, 0.2,
                50, 20, 10, 1, 0.5, 0, 0.1, Map.of(Element.PYRO, 0.4, Element.PHYSICAL, 0.2));
        assertEquals(320, stats.atk(), EPS);
        assertEquals(1150, stats.hp(), EPS);
        assertEquals(130, stats.def(), EPS);
        Reaction melt = new Reaction(Reaction.Type.MELT, 0.8, Element.PYRO, Element.CRYO);
        assertEquals(648, Damage.talentDamage(stats, 1, Element.PYRO, 20, 0, 0, 0.1,
                melt, 0, Damage.CritMode.ROLL, new Random(0)), EPS);
        assertEquals(29.25, Damage.talentDamage(stats, 0.1, Damage.ScalingStat.DEF,
                Element.PYRO, 20, 0, 0, 0, melt, 0, Damage.CritMode.EXPECTED, null), EPS);
        assertEquals(112.125, Damage.talentDamage(stats, 0.1, Damage.ScalingStat.HP,
                Element.PHYSICAL, 20, 0, 0, 0, null, 0, Damage.CritMode.EXPECTED, null), EPS);
    }

    @ParameterizedTest
    @CsvSource({"SWIRL,HYDRO,HYDRO", "SWIRL,PYRO,PYRO", "OVERLOADED,ELECTRO,PYRO",
            "SUPERCONDUCT,ELECTRO,CRYO", "ELECTRO_CHARGED,HYDRO,ELECTRO", "SHATTER,CRYO,PHYSICAL"})
    void transformativeResistanceElement(Reaction.Type type, Element swirled, Element expected) {
        // spec/mechanics/damage.md: Ordinary transformative reactions Damage / RES element.
        assertEquals(expected, Reaction.damageElement(type, swirled));
    }

    @Test
    void transformativeDamageUsesReactionResAndIgnoresOwnerAttackBonusesAndCrit() {
        // spec/mechanics/damage.md: Overloaded uses Pyro RES even with an Electro trigger;
        // ordinary transformative damage ignores ATK, ordinary bonuses, crit, and enemy DEF.
        Stats owner = new Stats(1, 1000, 9999, 9999, 9999, 0, 9, 9,
                0, 9999, 9999, 1, 9, 0, 9, Map.of(Element.PYRO, 9.0));
        Reaction overloaded = new Reaction(Reaction.Type.OVERLOADED, 0.8, Element.ELECTRO, Element.PYRO);
        assertEquals(17.165605 * 2.75 * 0.9, Damage.transformativeDamage(overloaded, owner, 0,
                Map.of(Element.PYRO, 0.1, Element.ELECTRO, 1.0)), EPS);
    }

    @Test
    void unsupportedLevelIsNotInterpolatedAndFrozenHasNoIntrinsicDamage() {
        assertThrows(IllegalArgumentException.class, () -> Damage.levelBase(21));
        assertThrows(IllegalArgumentException.class, () -> Damage.transformativeDamage(
                Reaction.Type.FROZEN, 20, 0, 0, 0));
    }
}

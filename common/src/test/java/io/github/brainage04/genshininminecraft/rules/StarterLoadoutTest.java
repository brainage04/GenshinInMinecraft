package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StarterLoadoutTest {
    private static final double EPS = 1e-7;

    @Test void travelerFullNormalChainKillsLevel8And20() {
        // datamine.md + traveler-anemo.md: (17.808*2.569 + 38.7413*2.42)*talent*120/(220+Le)*.9.
        // Decimal arithmetic, no crit/reaction/TTDS/artifacts: Lv8 seven236.5011265936 < 237.81997824 <= eight271.5236460388.
        // Lv20: 26 hits875.4143056245 < 885.2000016 <= 27 hits902.6591825439. Repeat N1→N5.
        chain(0, 8, 8, new int[]{23, 32, 40, 49, 81},
                new double[]{29.4057002889, 28.6788178099, 35.0225194453, 38.5247713898, 46.7847995608});
        chain(0, 20, 27, new int[]{23, 32, 40, 49, 81},
                new double[]{27.9354152745, 27.2448769194, 33.2713934730, 36.5985328203, 44.4455595828});
    }
    @Test void amberFullNormalChainKillsLevel8And20() {
        // datamine.md + amber.md: (18.6984*2.569 + 37.6075*2.275)*talent*1.36*120/(220+Le)*.9.
        // Slingshot R1 +36% normal/charged bonus: existing hitscan flight0s; no crit/TTDS/artifacts.
        // Decimal arithmetic: Lv8 six225.0009614249 < 237.81997824 <= seven256.0866205691.
        // Lv20: 24 hits872.5818535258 < 885.2000016 <= 25 hits921.0976858331.
        chain(1, 8, 7, new int[]{26, 22, 37, 34, 60},
                new double[]{31.0856591442, 31.0856591442, 39.9672760426, 40.7074107841, 51.0692971655});
        chain(1, 20, 25, new int[]{26, 22, 37, 34, 60},
                new double[]{29.5313761870, 29.5313761870, 37.9689122404, 38.6720402449, 48.5158323072});
    }
    @Test void kaeyaFullNormalChainKillsLevel8And20() {
        // datamine.md + kaeya.md: (18.6984*2.569 + 38.7413*2.42)*talent*120/(220+Le)*.9.
        // Decimal arithmetic, no crit/reaction/TTDS/artifacts: Lv8 five221.5127587369 < 237.81997824 <= six257.6132735245.
        // Lv20: 21 hits876.0439722485 < 885.2000016 <= 22 hits909.0250667397.
        chain(2, 8, 6, new int[]{27, 27, 47, 46, 74},
                new double[]{36.1005147876, 34.7169415697, 43.8377786082, 47.5922321461, 59.2652916253});
        chain(2, 20, 22, new int[]{27, 27, 47, 46, 74},
                new double[]{34.2954890482, 32.9810944912, 41.6458896778, 45.2126205388, 56.3020270440});
    }
    @Test void lisaFullNormalChainKillsLevel8And20() {
        // datamine.md + lisa.md: (19.41072*2.569 + 38.7413*2.42)*talent*120/(220+Le)*.9.
        // TTDS13.53522% HP adds no self ATK; no crit/reaction/artifacts. Repeat N1→N4.
        // Decimal arithmetic: Lv8 eight235.7667326523 < 237.81997824 <= nine262.7068371451.
        // Lv20: 31 hits860.3934644883 < 885.2000016 <= 32 hits895.9135840787.
        chain(3, 8, 9, new int[]{30, 20, 34, 57},
                new double[]{26.9401044928, 24.4365796309, 29.1170826337, 37.3895995688});
        chain(3, 20, 32, new int[]{30, 20, 34, 57},
                new double[]{25.5930992682, 23.2147506493, 27.6612285020, 35.5201195904});
    }
    private static void chain(int slot, int enemyLevel, int expectedHits, int[] recovery, double[] golden) {
        double[] hp = {enemyLevel == 8 ? 237.81997824 : 885.2000016}; // datamine.md decimal base×curve, independent of implementation.
        int[] landed = {0};
        var party = new Party(new EventTimeline(), (kit, hit) -> {
            assertEquals(CharacterKit.Kind.NORMAL, hit.kind());
            var stats = kit.state().normalChargedStats(hit.frame(), 0);
            double damage = Damage.talentDamage(stats, hit.multiplier(), hit.element(), enemyLevel,
                    0, 0, .10, null, 0, Damage.CritMode.NON_CRIT, null);
            assertEquals(golden[landed[0] % golden.length], damage, EPS, "Each sourced normal hit");
            assertTrue(hp[0] > 0, "No extra hit may be needed after the independently computed kill");
            hp[0] -= damage;
            landed[0]++;
        });
        if (slot != 0) assertTrue(party.switchTo(slot, 0));
        long start = 0;
        for (int hit = 0; hit < expectedHits; hit++) {
            assertTrue(party.activeKit().intent(Intent.ATTACK_PRESS, start));
            assertTrue(party.activeKit().intent(Intent.ATTACK_RELEASE, start));
            start += recovery[hit % recovery.length];
            party.advanceTo(start);
            if (hit < expectedHits - 1) assertTrue(hp[0] > 0, "The preceding landed hit is not lethal");
        }
        assertEquals(expectedHits, landed[0]);
        assertTrue(hp[0] <= 0, "The exact hand-computed hit count kills the hilichurl");
    }
    @Test void harbingerStrictHpThresholdUpdatesBothSwordUsersWithoutAllocatingPerHit() {
        var party = new Party(new EventTimeline(), (kit, hit) -> {});
        for (int slot : new int[]{0, 2}) {
            var kit = party.kit(slot);
            assertEquals(.19, kit.stats().critRate(), EPS); // Base5% + R1 passive14%.
            assertEquals(.680234, kit.stats().critDamage(), EPS); // Base50% + .102*1.767 secondary.
            assertSame(kit.stats(), kit.stats());
            kit.setHp(kit.maxHp() * .90);
            assertEquals(.05, kit.stats().critRate(), EPS); // Above90%, NOT at90%.
            kit.setHp(Math.nextUp(kit.maxHp() * .90));
            assertEquals(.19, kit.stats().critRate(), EPS);
        }
    }
    @Test void slingshotUsesFlightTimeNotChargeWindupAndDoesNotBoostTalents() {
        var party = new Party(new EventTimeline(), (kit, hit) -> {});
        var amber = party.kit(1);
        assertEquals(48.0361896 + 85.5570625, amber.stats().atk(), EPS);
        assertEquals(.05 + .120156, amber.stats().critRate(), EPS); // .068*1.767 secondary.
        assertEquals(0, amber.stats().damageBonus(Element.PYRO), EPS); // No E/Q bonus.
        assertEquals(.36, amber.state().normalChargedStats(0, 0).damageBonus(Element.PYRO), EPS);
        assertEquals(.36, amber.state().normalChargedStats(0, 18).damageBonus(Element.PHYSICAL), EPS);
        assertEquals(-.10, amber.state().normalChargedStats(0, 19).damageBonus(Element.PHYSICAL), EPS);
        assertThrows(IllegalArgumentException.class, () -> amber.state().normalChargedStats(0, -1));
    }
    @Test void thrillingTalesBuffsOnlyIncomingMemberForTenSecondsWithTwentySecondCooldown() {
        var party = new Party(new EventTimeline(), (kit, hit) -> {});
        assertEquals(2061.3042009 * 1.1353522, party.kit(3).maxHp(), EPS); // (802.3761*2.569)*(1+.0766*1.767).
        assertTrue(party.switchTo(3, 0));
        assertFalse(party.switchTo(0, 59)); // Rejected switch cannot proc.
        assertEquals(45.748752 + 93.753946, party.kit(0).stats().atk(), EPS);
        assertTrue(party.switchTo(0, 60));
        assertEquals((45.748752 + 93.753946) * 1.24, party.kit(0).stats().atk(), EPS);
        assertEquals(49.86613968 + 93.753946, party.kit(3).stats().atk(), EPS); // No self buff.
        assertTrue(party.switchTo(3, 120));
        assertTrue(party.switchTo(2, 180));
        assertEquals(48.0361896 + 93.753946, party.kit(2).stats().atk(), EPS); // Proc still on cooldown.
        party.advanceTo(659);
        assertEquals((45.748752 + 93.753946) * 1.24, party.kit(0).stats().atk(), EPS); // Buff follows its recipient off-field.
        party.advanceTo(660);
        assertEquals(45.748752 + 93.753946, party.kit(0).stats().atk(), EPS); // Exact expiry boundary.
        assertTrue(party.switchTo(3, 1200));
        assertTrue(party.switchTo(1, 1260)); // Exactly20s after prior proc.
        assertEquals((48.0361896 + 85.5570625) * 1.24, party.kit(1).stats().atk(), EPS);
        party.advanceTo(1860);
        assertEquals(48.0361896 + 85.5570625, party.kit(1).stats().atk(), EPS);
    }
    @Test void hilichurlCurveUsesEveryDataminedRowWithoutInterpolation() {
        double[] golden = {72.916996656, 92.628005520, 114.394001904, 138.21502656, 164.09295408, 192.05004864,
                222.07503552, 237.81997824, 261.73406688, 286.60406160, 326.88306672, 368.75900688,
                412.25701248, 460.52096448, 510.58303056, 562.4699712, 624.87799152, 679.92796656, 736.00203936, 885.2000016};
        // datamine.md: Decimal("13.584")*GROW_CURVE_HP[L], all20 rows; DEF500*GROW_CURVE_DEFENSE[L]=5L+500.
        for (int level = 1; level <= 20; level++) {
            assertEquals(golden[level - 1], HilichurlProfile.maxHp(level), EPS);
            assertEquals(5 * level + 500, HilichurlProfile.defense(level), EPS);
        }
        assertThrows(IllegalArgumentException.class, () -> HilichurlProfile.maxHp(0));
        assertThrows(IllegalArgumentException.class, () -> HilichurlProfile.maxHp(21));
    }
}

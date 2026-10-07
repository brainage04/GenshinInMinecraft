package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StarterLoadoutTest {
    private static final double EPS = 1e-7;

    @Test void travelerFullNormalChainKillsLevel8And20() {
        // damage.md + traveler-anemo.md: (45.75+94)*[.445,.434,.530,.583,.708]*120/(220+Le)*.9.
        // No crit/reaction/TTDS/artifacts. Lv8 HP237.820: seven hits236.92038158 < HP <= eight272.00498684.
        // Lv20 HP885.200: 26 hits876.96618750 < HP <= 27 hits904.25936250. Repeat N1→N5.
        chain(0, 8, 8, new int[]{23, 32, 40, 49, 81},
                new double[]{29.45782895, 28.72965789, 35.08460526, 38.59306579, 46.86773684});
        chain(0, 20, 27, new int[]{23, 32, 40, 49, 81},
                new double[]{27.98493750, 27.29317500, 33.33037500, 36.66341250, 44.52435000});
    }
    @Test void amberFullNormalChainKillsLevel8And20() {
        // damage.md + amber.md: (48.04+86)*[.3612,.3612,.4644,.473,.5934]*1.36*120/(220+Le)*.9.
        // Slingshot R1 +36% normal/charged bonus: existing hitscan flight0s; no crit/TTDS/artifacts.
        // Lv8: six hits225.75338496 < 237.820 <= seven256.94299736.
        // Lv20: 24 hits875.49984605 < 885.200 <= 25 hits924.17791968. Repeat N1→N5.
        chain(1, 8, 7, new int[]{26, 22, 37, 34, 60},
                new double[]{31.18961240, 31.18961240, 40.10093022, 40.84354004, 51.24007751});
        chain(1, 20, 25, new int[]{26, 22, 37, 34, 60},
                new double[]{29.63013178, 29.63013178, 38.09588371, 38.80136304, 48.67807363});
    }
    @Test void kaeyaFullNormalChainKillsLevel8And20() {
        // damage.md + kaeya.md: (48.04+94)*[.5375,.5169,.6527,.7086,.8824]*120/(220+Le)*.9.
        // No crit/reaction/TTDS/artifacts. Lv8: five hits221.90311137 < 237.820 <= six258.06724295.
        // Lv20: 21 hits877.58774820 < 885.200 <= 22 hits910.62696240. Repeat N1→N5.
        chain(2, 8, 6, new int[]{27, 27, 47, 46, 74},
                new double[]{36.16413158, 34.77812021, 43.91503011, 47.67609979, 59.36972968});
        chain(2, 20, 22, new int[]{27, 27, 47, 46, 74},
                new double[]{34.35592500, 33.03921420, 41.71927860, 45.29229480, 56.40124320});
    }
    @Test void lisaFullNormalChainKillsLevel8And20() {
        // damage.md + lisa.md: (49.87+94)*[.396,.3592,.428,.5496]*120/(220+Le)*.9.
        // TTDS13.5% HP adds no self ATK; no crit/reaction/artifacts. Repeat N1→N4.
        // Lv8: eight hits236.17699200 < 237.820 <= nine263.16397516.
        // Lv20: 31 hits861.89064120 < 885.200 <= 32 hits897.47256960.
        chain(3, 8, 9, new int[]{30, 20, 34, 57},
                new double[]{26.98698316, 24.47910189, 29.16774947, 37.45466147});
        chain(3, 20, 32, new int[]{30, 20, 34, 57},
                new double[]{25.63763400, 23.25514680, 27.70936200, 35.58192840});
    }
    private static void chain(int slot, int enemyLevel, int expectedHits, int[] recovery, double[] golden) {
        double[] hp = {enemyLevel == 8 ? 237.820 : 885.200}; // hilichurls.md; independent of implementation curve.
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
            assertEquals(.68, kit.stats().critDamage(), EPS); // Base50% + level20 secondary18%.
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
        assertEquals(48.04 + 86, amber.stats().atk(), EPS);
        assertEquals(.05 + .12, amber.stats().critRate(), EPS);
        assertEquals(0, amber.stats().damageBonus(Element.PYRO), EPS); // No E/Q bonus.
        assertEquals(.36, amber.state().normalChargedStats(0, 0).damageBonus(Element.PYRO), EPS);
        assertEquals(.36, amber.state().normalChargedStats(0, 18).damageBonus(Element.PHYSICAL), EPS);
        assertEquals(-.10, amber.state().normalChargedStats(0, 19).damageBonus(Element.PHYSICAL), EPS);
        assertThrows(IllegalArgumentException.class, () -> amber.state().normalChargedStats(0, -1));
    }
    @Test void thrillingTalesBuffsOnlyIncomingMemberForTenSecondsWithTwentySecondCooldown() {
        var party = new Party(new EventTimeline(), (kit, hit) -> {});
        assertEquals(2061.30 * 1.135, party.kit(3).maxHp(), EPS); // Published13.5% HP secondary.
        assertTrue(party.switchTo(3, 0));
        assertFalse(party.switchTo(0, 59)); // Rejected switch cannot proc.
        assertEquals(45.75 + 94, party.kit(0).stats().atk(), EPS);
        assertTrue(party.switchTo(0, 60));
        assertEquals((45.75 + 94) * 1.24, party.kit(0).stats().atk(), EPS);
        assertEquals(49.87 + 94, party.kit(3).stats().atk(), EPS); // No self buff.
        assertTrue(party.switchTo(3, 120));
        assertTrue(party.switchTo(2, 180));
        assertEquals(48.04 + 94, party.kit(2).stats().atk(), EPS); // Proc still on cooldown.
        party.advanceTo(659);
        assertEquals((45.75 + 94) * 1.24, party.kit(0).stats().atk(), EPS); // Buff follows its recipient off-field.
        party.advanceTo(660);
        assertEquals(45.75 + 94, party.kit(0).stats().atk(), EPS); // Exact expiry boundary.
        assertTrue(party.switchTo(3, 1200));
        assertTrue(party.switchTo(1, 1260)); // Exactly20s after prior proc.
        assertEquals((48.04 + 86) * 1.24, party.kit(1).stats().atk(), EPS);
        party.advanceTo(1860);
        assertEquals(48.04 + 86, party.kit(1).stats().atk(), EPS);
    }
    @Test void hilichurlCurveUsesEveryPublishedRowWithoutInterpolation() {
        double[] golden = {72.917, 92.628, 114.394, 138.215, 164.093, 192.050, 222.075, 237.820, 261.734, 286.604,
                326.883, 368.759, 412.257, 460.521, 510.583, 562.470, 624.878, 679.928, 736.002, 885.200};
        // hilichurls.md Type1 rounded public table, every level1–20; DEF5L+500, no domain/co-op scaling.
        for (int level = 1; level <= 20; level++) {
            assertEquals(golden[level - 1], HilichurlProfile.maxHp(level), EPS);
            assertEquals(5 * level + 500, HilichurlProfile.defense(level), EPS);
        }
        assertThrows(IllegalArgumentException.class, () -> HilichurlProfile.maxHp(0));
        assertThrows(IllegalArgumentException.class, () -> HilichurlProfile.maxHp(21));
    }
}

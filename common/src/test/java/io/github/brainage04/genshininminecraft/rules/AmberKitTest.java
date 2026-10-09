package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.kit.AmberKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Hit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Kind;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** amber.md talent1, sourced hold-normal frames and resource tables; geometry/wave times are adaptations. */
class AmberKitTest {
    @Test void tapKeepsNormalButHoldReplacesItWithPhysicalAimedShot() {
        var hits = new ArrayList<Hit>();
        var kit = new AmberKit(hits::add);
        kit.intent(Intent.ATTACK_PRESS, 0);
        kit.intent(Intent.ATTACK_RELEASE, 0);
        kit.advanceTo(14);
        assertEquals(.3612, hits.getFirst().multiplier());
        assertEquals(Kind.NORMAL, hits.getFirst().kind());
        kit.intent(Intent.ATTACK_PRESS, 26);
        kit.advanceTo(34);
        assertFalse(kit.aiming());
        kit.advanceTo(35);
        assertTrue(kit.aiming());
        kit.intent(Intent.ATTACK_RELEASE, 36);
        kit.advanceTo(40);
        assertEquals(1, hits.size());
        kit.advanceTo(41); // Uncharged hold-normal earliest release15, not a second normal arrow.
        Hit aim = hits.getLast();
        assertEquals(Kind.CHARGED, aim.kind());
        assertEquals(.4386, aim.multiplier());
        assertEquals(Element.PHYSICAL, aim.element());
        assertEquals(0, aim.gauge());
        assertFalse(kit.intent(Intent.ATTACK_PRESS, 50));
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 51)); //15+10 total25 original action frames.
    }
    @Test void fullChargeBoundaryUsesHoldNormal86FramesAndTwoGaugePyro() {
        for (int release : new int[]{85, 86, 180}) {
            var hits = new ArrayList<Hit>();
            var kit = new AmberKit(hits::add);
            kit.intent(Intent.ATTACK_PRESS, 0);
            kit.advanceTo(release);
            assertEquals(release >= 86, kit.fullyCharged());
            assertTrue(hits.isEmpty());
            assertTrue(kit.intent(Intent.ATTACK_RELEASE, release));
            kit.advanceTo(release);
            assertEquals(1, hits.size());
            Hit aim = hits.getFirst();
            assertEquals(release >= 86 ? 1.24 : .4386, aim.multiplier());
            assertEquals(release >= 86 ? Element.PYRO : Element.PHYSICAL, aim.element());
            assertEquals(release >= 86 ? 2 : 0, aim.gauge());
            assertEquals(release >= 86 ? "Charged Attack" : null, aim.icdTag());
            assertFalse(kit.canSwitch(release + 9));
            assertTrue(kit.canSwitch(release + 10));
            assertEquals(100, kit.stamina().current());
        }
    }
    @Test void exhaustedBowCanChargeAndLeavingAimCancelsTheArrow() {
        var hits = new ArrayList<Hit>();
        var kit = new AmberKit(hits::add);
        assertTrue(kit.stamina().consume(100, 0));
        kit.intent(Intent.ATTACK_PRESS, 0);
        kit.intent(Intent.ATTACK_RELEASE, 86);
        kit.advanceTo(86);
        assertEquals(1.24, hits.getFirst().multiplier());
        assertEquals(0, kit.stamina().current()); // Bow costs0, no rejected consume or regen deadline mutation.
        kit.intent(Intent.ATTACK_PRESS, 96);
        kit.advanceTo(110);
        kit.leaveField(110);
        assertFalse(kit.aiming());
        kit.intent(Intent.ATTACK_RELEASE, 190);
        kit.advanceTo(200);
        assertEquals(1, hits.size());
    }
    @Test void bunnyHpTimerAndExplosionAreFromLandingNotCooldownStart() {
        var hits = new ArrayList<Hit>();
        var kit = new AmberKit(hits::add);
        assertTrue(kit.intent(Intent.SKILL_PRESS, 0));
        assertEquals(5 + 15 * 60, kit.skillReadyFrame());
        assertFalse(kit.canSwitch(22));
        assertTrue(kit.canSwitch(23));
        kit.advanceTo(44);
        assertTrue(hits.isEmpty());
        kit.advanceTo(45);
        assertEquals(Kind.BUNNY_LAND, hits.getFirst().kind());
        assertEquals(2037.8803158 * .4136, kit.puppetHp(), 1e-12);
        kit.advanceTo(524);
        assertTrue(kit.puppetAlive());
        assertEquals(1, hits.size());
        kit.advanceTo(525);
        assertFalse(kit.puppetAlive());
        Hit explosion = hits.getLast();
        assertEquals(525, explosion.frame());
        assertEquals(Kind.BUNNY_EXPLODE, explosion.kind());
        assertEquals(1.232, explosion.multiplier());
        assertEquals(Element.PYRO, explosion.element());
        assertEquals(2, explosion.gauge());
        assertNull(explosion.icdTag());
        assertEquals(4, explosion.particles());
        assertEquals(0, kit.energy()); // Placement/lifetime are not particle collection.
        assertFalse(kit.intent(Intent.SKILL_PRESS, 904));
        assertTrue(kit.intent(Intent.SKILL_PRESS, 905)); // Single C0 charge; no second cast while cooling down.
    }
    @Test void destroyingBunnyExplodesOnceAndDoesNotWaitForOrReplayExpiry() {
        var hits = new ArrayList<Hit>();
        var kit = new AmberKit(hits::add);
        kit.intent(Intent.SKILL_PRESS, 0);
        kit.advanceTo(45);
        kit.damagePuppet(100, 60);
        assertEquals(2037.8803158 * .4136 - 100, kit.puppetHp(), 1e-12);
        assertEquals(1, hits.size());
        kit.damagePuppet(1000, 61);
        assertFalse(kit.puppetAlive());
        assertEquals(61, hits.getLast().frame());
        kit.damagePuppet(1000, 62);
        kit.advanceTo(600);
        assertEquals(List.of(Kind.BUNNY_LAND, Kind.BUNNY_EXPLODE), hits.stream().map(Hit::kind).toList());
    }
    @Test void puppetAndRainRetainAmberOwnershipAfterSwitchOrNewNormals() {
        var owners = new ArrayList<Object>();
        var hits = new ArrayList<Hit>();
        var party = new Party(new EventTimeline(), (owner, hit) -> { owners.add(owner); hits.add(hit); });
        party.switchTo(1, 0);
        var amber = (AmberKit) party.activeKit();
        amber.intent(Intent.SKILL_PRESS, 0);
        assertTrue(amber.intent(Intent.ATTACK_PRESS, 32));
        amber.intent(Intent.ATTACK_RELEASE, 32);
        amber.grantEnergy(40);
        assertTrue(amber.intent(Intent.BURST_PRESS, 58));
        assertTrue(party.switchTo(2, 119));
        party.advanceTo(525);
        assertEquals(18, hits.stream().filter(hit -> hit.kind() == Kind.RAIN_INNER || hit.kind() == Kind.RAIN_OUTER).count());
        assertEquals(Kind.BUNNY_EXPLODE, hits.getLast().kind());
        assertTrue(owners.stream().allMatch(owner -> owner == amber));
    }
    @Test void burstReservesFortyRejectsEarlyRecastAndUsesMeasuredCooldownAndRecovery() {
        var hits = new ArrayList<Hit>();
        var kit = new AmberKit(hits::add);
        kit.grantEnergy(39);
        assertFalse(kit.intent(Intent.BURST_PRESS, 0));
        assertEquals(0, kit.burstReadyFrame());
        assertEquals(39, kit.energy());
        kit.grantEnergy(1);
        assertTrue(kit.intent(Intent.BURST_PRESS, 0));
        assertEquals(0, kit.energy());
        assertEquals(56 + 12 * 60, kit.burstReadyFrame());
        assertFalse(kit.canSwitch(60));
        assertTrue(kit.canSwitch(61));
        assertFalse(kit.intent(Intent.ATTACK_PRESS, 110));
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 111));
        kit.intent(Intent.ATTACK_RELEASE, 111);
        kit.grantEnergy(40);
        assertFalse(kit.intent(Intent.BURST_PRESS, 775));
        assertEquals(40, kit.energy());
        assertTrue(kit.intent(Intent.BURST_PRESS, 776));
    }
    @Test void eighteenRainWavesSplitInnerOuterWithinTwoSecondsWithSharedAmberIcd() {
        var hits = new ArrayList<Hit>();
        var kit = new AmberKit(hits::add);
        kit.grantEnergy(40);
        kit.intent(Intent.BURST_PRESS, 0);
        kit.advanceTo(71);
        assertTrue(hits.isEmpty());
        kit.advanceTo(72);
        assertEquals(1, hits.size());
        kit.advanceTo(192);
        assertEquals(18, hits.size());
        assertEquals(9, hits.stream().filter(hit -> hit.kind() == Kind.RAIN_INNER).count());
        assertEquals(9, hits.stream().filter(hit -> hit.kind() == Kind.RAIN_OUTER).count());
        assertEquals(5.0544, hits.stream().mapToDouble(Hit::multiplier).sum(), 1e-12);
        assertTrue(hits.stream().allMatch(hit -> hit.frame() >= 72 && hit.frame() < 72 + 120
                && hit.element() == Element.PYRO && hit.gauge() == 1 && "Elemental Burst".equals(hit.icdTag())));
        kit.advanceTo(600);
        assertEquals(18, hits.size());
    }
}

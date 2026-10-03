package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.kit.LisaKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Expectations from spec/mechanics/lisa.md, not generated from kit constants. */
class LisaKitTest {
    @Test void conductiveHasThreeIndependentFifteenSecondExpiriesAndConsumesAll() {
        var marks = new ConductiveState();
        marks.add(0); marks.add(60); marks.add(120);
        assertEquals(3, marks.stacks());
        marks.advanceTo(899); assertEquals(3, marks.stacks());
        marks.advanceTo(900); assertEquals(2, marks.stacks());
        marks.advanceTo(960); assertEquals(1, marks.stacks());
        assertEquals(1, marks.consume(1000));
        assertEquals(0, marks.stacks());
        marks.add(1000); marks.add(1001); marks.add(1002); marks.add(1003);
        assertEquals(3, marks.stacks());
        marks.advanceTo(1901); assertEquals(2, marks.stacks()); // Named oldest-refresh adaptation.
        marks.advanceTo(1903); assertEquals(0, marks.stacks());
    }
    @Test void tapLaunchCooldownRecoveryAndSharedElectroIcd() {
        var hits = new ArrayList<Hit>();
        var kit = new LisaKit(hits::add);
        assertTrue(kit.intent(Intent.SKILL_PRESS, 0));
        assertTrue(kit.intent(Intent.SKILL_RELEASE, 0));
        kit.advanceTo(16); assertTrue(hits.isEmpty());
        kit.advanceTo(17);
        var tap = hits.getFirst();
        assertEquals(Kind.VIOLET_ORB, tap.kind());
        assertEquals(.8, tap.multiplier()); assertEquals(1, tap.gauge());
        assertEquals("Lisa Electro DMG", tap.icdTag()); assertEquals(0, tap.particles());
        assertEquals(77, kit.skillReadyFrame());
        assertFalse(kit.canSwitch(19)); assertTrue(kit.canSwitch(20));
        assertFalse(kit.intent(Intent.ATTACK_PRESS, 37));
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 38)); kit.intent(Intent.ATTACK_RELEASE, 38);
        assertFalse(kit.intent(Intent.SKILL_PRESS, 76));
        assertTrue(kit.intent(Intent.SKILL_PRESS, 77));
    }
    @Test void releasingBeforeChargeThresholdStillProducesTap() {
        var hits = new ArrayList<Hit>();
        var kit = new LisaKit(hits::add);
        kit.intent(Intent.SKILL_PRESS, 0);
        assertTrue(kit.intent(Intent.SKILL_RELEASE, 113));
        kit.advanceTo(113);
        assertEquals(Kind.VIOLET_ORB, hits.getFirst().kind());
        assertEquals(173, kit.skillReadyFrame());
        assertFalse(kit.intent(Intent.SKILL_RELEASE, 114));
    }
    @Test void holdReleaseUsesSourcedThresholdGaugeFiveParticlesAndReleaseCooldown() {
        var hits = new ArrayList<Hit>();
        var kit = new LisaKit(hits::add);
        kit.intent(Intent.SKILL_PRESS, 0);
        assertTrue(kit.intent(Intent.SKILL_RELEASE, 114));
        assertEquals(114 + 16 * 60, kit.skillReadyFrame());
        kit.advanceTo(116); assertTrue(hits.isEmpty());
        kit.advanceTo(117);
        var hold = hits.getFirst();
        assertEquals(Kind.VIOLET_HOLD, hold.kind()); assertEquals(2, hold.gauge());
        assertNull(hold.icdTag()); assertEquals(5, hold.particles());
        assertFalse(kit.canSwitch(117)); assertTrue(kit.canSwitch(118));
        assertFalse(kit.intent(Intent.ATTACK_PRESS, 140));
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 141)); kit.intent(Intent.ATTACK_RELEASE, 141);
        assertFalse(kit.intent(Intent.SKILL_PRESS, 1073));
        assertTrue(kit.intent(Intent.SKILL_PRESS, 1074));
        assertArrayEquals(new double[]{3.2, 3.68, 4.24, 4.872},
                java.util.stream.IntStream.range(0, 4).mapToDouble(LisaKit::holdMultiplier).toArray());
    }
    @Test void holdAutoReleasesAtFourSecondsAndCannotDoubleRelease() {
        var hits = new ArrayList<Hit>();
        var kit = new LisaKit(hits::add);
        kit.intent(Intent.SKILL_PRESS, 0);
        kit.advanceTo(239); assertTrue(hits.isEmpty());
        kit.advanceTo(243);
        assertEquals(1, hits.size()); assertEquals(243, hits.getFirst().frame());
        assertEquals(1200, kit.skillReadyFrame());
        assertFalse(kit.intent(Intent.SKILL_RELEASE, 244));
    }
    @Test void cryoSlowedHoldUsesDocumentedApproximateTwoPointTwoSeconds() {
        var hits = new ArrayList<Hit>();
        var kit = new LisaKit(hits::add); kit.setCryoSlowed(true);
        kit.intent(Intent.SKILL_PRESS, 0); kit.intent(Intent.SKILL_RELEASE, 131); kit.advanceTo(131);
        assertEquals(Kind.VIOLET_ORB, hits.getFirst().kind());
        kit.intent(Intent.SKILL_PRESS, 191); kit.intent(Intent.SKILL_RELEASE, 323); kit.advanceTo(326);
        assertEquals(Kind.VIOLET_HOLD, hits.getLast().kind());
    }
    @Test void roseRejectsBelowEightyWithoutMutationsThenSchedulesPlacementAndTwentyNineArcs() {
        var hits = new ArrayList<Hit>();
        var kit = new LisaKit(hits::add); kit.grantEnergy(79);
        assertFalse(kit.intent(Intent.BURST_PRESS, 0));
        assertEquals(79, kit.energy()); assertEquals(0, kit.burstReadyFrame());
        kit.advanceTo(20); assertTrue(hits.isEmpty()); kit.grantEnergy(1);
        assertTrue(kit.intent(Intent.BURST_PRESS, 20));
        assertEquals(0, kit.energy()); assertEquals(20 + 53 + 20 * 60, kit.burstReadyFrame());
        kit.advanceTo(75); assertTrue(hits.isEmpty()); kit.advanceTo(76);
        assertEquals(.1, hits.getFirst().multiplier()); assertEquals(0, hits.getFirst().gauge());
        assertFalse(kit.canSwitch(75)); assertTrue(kit.canSwitch(76));
        kit.advanceTo(138); assertEquals(1, hits.size()); kit.advanceTo(139);
        assertEquals(.3656, hits.getLast().multiplier()); assertEquals(1, hits.getLast().gauge());
        assertEquals("Elemental Burst", hits.getLast().icdTag());
        kit.advanceTo(1039); assertEquals(30, hits.size());
        for (int arc = 0; arc < 29; arc++) assertEquals(20 + 119 + arc * 30, hits.get(arc + 1).frame());
        assertFalse(kit.roseActive(1039));
        kit.grantEnergy(80); assertFalse(kit.intent(Intent.BURST_PRESS, 1272));
        assertTrue(kit.intent(Intent.BURST_PRESS, 1273));
    }
    @Test void rosePersistsOffFieldButNotAfterLisaFalls() {
        var hits = new ArrayList<Hit>();
        var party = new Party(new EventTimeline(), (kit, hit) -> hits.add(hit));
        assertTrue(party.switchTo(3, 0)); party.activeKit().grantEnergy(80);
        assertTrue(party.activeKit().intent(Intent.BURST_PRESS, 0));
        assertTrue(party.switchTo(1, 60)); party.advanceTo(149);
        assertEquals(3, hits.size()); assertEquals(149, hits.getLast().frame());
        party.kit(3).setHp(0); party.advanceTo(1019);
        assertEquals(3, hits.size());
    }
    @Test void fieldExitCancelsUnreleasedHoldAndUnlaunchedTapNotRose() {
        var hits = new ArrayList<Hit>(); var kit = new LisaKit(hits::add);
        kit.intent(Intent.SKILL_PRESS, 0); kit.leaveField(10); kit.advanceTo(243);
        assertTrue(hits.isEmpty()); assertEquals(0, kit.skillReadyFrame());
        kit.intent(Intent.SKILL_PRESS, 244); kit.intent(Intent.SKILL_RELEASE, 244); kit.leaveField(245);
        kit.advanceTo(300); assertTrue(hits.isEmpty());
    }
}

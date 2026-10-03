package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Hit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Kind;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TravelerAnemoKitTest {
    @Test void fiveHitAetherStringUsesSourcedHitmarksAndTalentOneScalings() {
        List<Hit> hits = new ArrayList<>();
        var kit = new TravelerAnemoKit(hits::add);
        // traveler-anemo.md: original Aether hitmarks and TCL next-normal intervals.
        long[] starts = {0, 23, 55, 95, 144};
        for (long start : starts) {
            assertTrue(kit.intent(Intent.ATTACK_PRESS, start));
            assertTrue(kit.intent(Intent.ATTACK_RELEASE, start));
        }
        kit.advanceTo(170);
        assertEquals(List.of(13L, 36L, 71L, 125L, 169L), hits.stream().map(Hit::frame).toList());
        assertEquals(List.of(.445, .434, .530, .583, .708), hits.stream().map(Hit::multiplier).toList());
        assertTrue(hits.stream().allMatch(h -> h.element() == Element.PHYSICAL && h.gauge() == 0));
        assertEquals(0, kit.comboIndex());
    }

    @Test void resetWindowAndEarlyInputCannotSkipRecovery() {
        List<Hit> hits = new ArrayList<>();
        var kit = new TravelerAnemoKit(hits::add);
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 0));
        kit.intent(Intent.ATTACK_RELEASE, 0);
        assertFalse(kit.intent(Intent.ATTACK_PRESS, 22));
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 23));
        kit.intent(Intent.ATTACK_RELEASE, 23);
        long reset = 23 + 32 + 90; // Sourced N2 recovery, then documented 90-frame reset adaptation.
        assertEquals(reset, kit.comboResetFrame());
        assertTrue(kit.intent(Intent.ATTACK_PRESS, reset));
        kit.intent(Intent.ATTACK_RELEASE, reset);
        kit.advanceTo(reset + 13);
        assertEquals(.445, hits.getLast().multiplier());
    }

    @Test void holdingAttackPaysTwentyStaminaOnceForBothAetherChargedHits() {
        List<Hit> hits = new ArrayList<>();
        var kit = new TravelerAnemoKit(hits::add);
        kit.intent(Intent.ATTACK_PRESS, 0);
        kit.advanceTo(49);
        assertEquals(List.of(13L, 38L, 49L), hits.stream().map(Hit::frame).toList());
        assertEquals(List.of(.445, .559, .607), hits.stream().map(Hit::multiplier).toList());
        assertEquals(Kind.CHARGED, hits.getLast().kind());
        assertEquals(80, kit.stamina().current()); // stamina.md: ordinary sword charge20, once not per hit.
    }

    @Test void insufficientStaminaRejectsTheChargedFollowUpButNotItsPrecedingNormal() {
        List<Hit> hits = new ArrayList<>();
        var kit = new TravelerAnemoKit(hits::add);
        assertTrue(kit.stamina().consume(81, 0));
        kit.intent(Intent.ATTACK_PRESS, 0);
        kit.advanceTo(49);
        assertEquals(List.of(Kind.NORMAL), hits.stream().map(Hit::kind).toList());
        assertEquals(19, kit.stamina().current());
        assertTrue(kit.intent(Intent.ATTACK_RELEASE, 49));
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 49), "Rejected charge must not install charged recovery");
    }

    @Test void tapStormCooldownStartsAtSourcedFrame27AndOnlyHitGrantsSixEnergy() {
        List<Hit> hits = new ArrayList<>();
        var kit = new TravelerAnemoKit(hits::add);
        assertTrue(kit.intent(Intent.SKILL_PRESS, 0));
        assertFalse(kit.intent(Intent.SKILL_PRESS, 1));
        assertTrue(kit.intent(Intent.SKILL_RELEASE, 3));
        kit.advanceTo(31);
        assertTrue(hits.isEmpty());
        kit.advanceTo(32);
        assertEquals(1.76, hits.getFirst().multiplier());
        assertEquals(2, hits.getFirst().particles());
        assertEquals(0, kit.energy());
        kit.grantParticles(2);
        assertEquals(6, kit.energy()); // elements.md: same-element on-field particle = 3.
        assertEquals(327, kit.skillReadyFrame());
        assertFalse(kit.intent(Intent.SKILL_PRESS, 326));
        assertTrue(kit.intent(Intent.SKILL_PRESS, 327));
    }

    @Test void fullHoldKeepsDiscreteCuttingScheduleAndEightSecondCooldown() {
        List<Hit> hits = new ArrayList<>();
        var kit = new TravelerAnemoKit(hits::add);
        kit.intent(Intent.SKILL_PRESS, 0);
        kit.advanceTo(113);
        assertEquals(List.of(21L, 30L, 51L, 60L, 81L, 90L, 113L), hits.stream().map(Hit::frame).toList());
        assertEquals(List.of(.12, .12, .168, .168, .168, .168, 1.92), hits.stream().map(Hit::multiplier).toList());
        assertEquals("Elemental Skill Anemo", hits.getFirst().icdTag());
        assertFalse(hits.getFirst().absorbedHit());
        assertTrue(hits.get(1).absorbedHit());
        assertNull(hits.getLast().icdTag());
        assertEquals(588, kit.skillReadyFrame());
    }

    @Test void burstNeedsSixtyEnergyDrainsItAndKeepsNineIndependentTicks() {
        List<Hit> hits = new ArrayList<>();
        var kit = new TravelerAnemoKit(hits::add);
        assertFalse(kit.intent(Intent.BURST_PRESS, 0));
        kit.grantEnergy(59);
        assertFalse(kit.intent(Intent.BURST_PRESS, 0));
        kit.grantEnergy(10);
        assertEquals(60, kit.energy());
        assertTrue(kit.intent(Intent.BURST_PRESS, 0));
        assertEquals(0, kit.energy());
        kit.advanceTo(360);
        assertEquals(List.of(96L,126L,156L,186L,216L,246L,276L,306L,336L), hits.stream().map(Hit::frame).toList());
        assertTrue(hits.stream().allMatch(h -> h.multiplier() == .808 && h.icdTag().equals("Elemental Burst")));
        kit.grantEnergy(60);
        assertFalse(kit.intent(Intent.BURST_PRESS, 899));
        assertTrue(kit.intent(Intent.BURST_PRESS, 900));
    }

    @Test void multiplePlayersShareSubTickHitOrderRatherThanPlayerIterationOrder() {
        var timeline = new EventTimeline();
        List<Hit> hits = new ArrayList<>();
        var first = new TravelerAnemoKit(timeline, hits::add);
        var second = new TravelerAnemoKit(timeline, hits::add);
        first.intent(Intent.SKILL_PRESS, 0);
        first.intent(Intent.SKILL_RELEASE, 0);
        second.intent(Intent.ATTACK_PRESS, 18);
        second.intent(Intent.ATTACK_RELEASE, 18);
        first.advanceTo(33);
        // Aether normal hit18+13=31 precedes Palm hit32, inside the same tick11.
        assertEquals(List.of(31L, 32L), hits.stream().map(Hit::frame).toList());
        assertEquals(List.of(.445, 1.76), hits.stream().map(Hit::multiplier).toList());
    }

    @Test void deadCharacterCannotStartActionsOrDealPendingHits() {
        List<Hit> hits = new ArrayList<>();
        var kit = new TravelerAnemoKit(hits::add);
        assertEquals(2342.39, kit.maxHp()); // damage.md: Lv20/20 Traveler.
        kit.intent(Intent.ATTACK_PRESS, 0);
        kit.setHp(0);
        kit.advanceTo(50);
        assertTrue(hits.isEmpty());
        assertFalse(kit.intent(Intent.SKILL_PRESS, 50));
    }
}

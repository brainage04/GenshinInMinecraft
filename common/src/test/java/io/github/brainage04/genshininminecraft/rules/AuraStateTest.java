package io.github.brainage04.genshininminecraft.rules;

import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuraStateTest {
    private static final double EPS = 1e-10;

    @ParameterizedTest
    @CsvSource({"1,0.8,570", "2,1.6,720"})
    void freshTaxAndLinearDecay(double source, double taxed, long durationFrames) {
        // spec/mechanics/elements.md: Fresh aura table (1U=9.5s, 2U=12s, tax=.8).
        AuraState aura = new AuraState();
        assertTrue(aura.applyHit(Element.CRYO, source, 0).isEmpty());
        assertEquals(taxed, aura.gauge(Element.CRYO), EPS);
        assertEquals(durationFrames, aura.remainingAuraFrames(Element.CRYO), EPS);
        aura.advanceTo(durationFrames / 2);
        assertEquals(taxed / 2, aura.gauge(Element.CRYO), EPS);
        aura.advanceTo(durationFrames);
        assertEquals(0, aura.gauge(Element.CRYO), EPS);
    }

    @Test
    void fourUnitTaxIsKnownButConflictingDurationIsNotAnExactGolden() {
        // spec/mechanics/elements.md: 4U=.8*4=3.2U; conflict row retains 16.8s historical
        // vs current 17s. Assert the published range, not an exact disputed duration.
        AuraState aura = new AuraState();
        aura.applyHit(Element.HYDRO, 4, 0);
        assertEquals(3.2, aura.gauge(Element.HYDRO), EPS);
        double durationSeconds = aura.remainingAuraFrames(Element.HYDRO) / 60;
        assertTrue(durationSeconds >= 16.8 - EPS && durationSeconds <= 17 + EPS);
        aura.advanceTo(1020);
        assertEquals(0, aura.gauge(Element.HYDRO), EPS);
    }

    @ParameterizedTest
    @CsvSource({"PYRO,720", "CRYO,1140", "HYDRO,1140", "ELECTRO,1140"})
    void strongerRefreshIsMaximumNotAdditionAndPyroChangesDecayRate(Element element, double remainingFrames) {
        // spec/mechanics/elements.md: non-additive refill and Pyro Version 3.0 exception.
        AuraState aura = new AuraState();
        aura.applyHit(element, 1, 0);
        aura.applyHit(element, 2, 60);
        assertEquals(1.6, aura.gauge(element), EPS);
        assertEquals(remainingFrames, aura.remainingAuraFrames(element), EPS);
        aura.applyHit(element, 1, 60);
        assertEquals(1.6, aura.gauge(element), EPS);
        assertEquals(remainingFrames, aura.remainingAuraFrames(element), EPS);
    }

    @Test
    void unchangedPyroGaugeRetainsItsPreviousDecayRate() {
        // spec/mechanics/elements.md: Pyro changes rate only when the amount changes.
        AuraState aura = new AuraState();
        aura.applyHit(Element.PYRO, 2, 0);
        aura.applyHit(Element.PYRO, 1, 60);
        assertEquals(22.0 / 15, aura.gauge(Element.PYRO), EPS);
        assertEquals(660, aura.remainingAuraFrames(Element.PYRO), EPS);
    }

    @ParameterizedTest
    @CsvSource({"CRYO,PYRO,MELT,1.6,0", "PYRO,CRYO,MELT,0.5,1.1",
            "PYRO,HYDRO,VAPORIZE,1.6,0", "HYDRO,PYRO,VAPORIZE,0.5,1.1",
            "PYRO,ELECTRO,OVERLOADED,1,0.6", "ELECTRO,PYRO,OVERLOADED,1,0.6",
            "CRYO,ELECTRO,SUPERCONDUCT,1,0.6", "ELECTRO,CRYO,SUPERCONDUCT,1,0.6"})
    void directionalGaugeConsumption(Element existing, Element trigger, Reaction.Type type,
            double consumed, double remaining) {
        // spec/mechanics/elements.md: Ordinary reaction consumption pair table; triggers
        // are untaxed, over-consumption clears without installing unused trigger gauge.
        AuraState aura = new AuraState();
        aura.applyHit(existing, 2, 0);
        List<Reaction> reactions = aura.applyHit(trigger, 1, 0);
        assertEquals(List.of(new Reaction(type, consumed, trigger, existing)), reactions);
        assertEquals(remaining, aura.gauge(existing), EPS);
        assertEquals(0, aura.gauge(trigger), EPS);
    }

    @ParameterizedTest
    @CsvSource({"PYRO", "CRYO", "HYDRO", "ELECTRO"})
    void swirlConsumesHalfUntaxedGaugeAndAnemoCannotBeAnAura(Element existing) {
        // spec/mechanics/elements.md: pair table, Swirl c=.5; Anemo has no aura/reverse Swirl.
        AuraState aura = new AuraState();
        aura.applyHit(Element.ANEMO, 4, 0);
        assertEquals(0, aura.gauge(Element.ANEMO), EPS);
        assertTrue(aura.applyHit(existing, 1, 0).isEmpty());
        Reaction swirl = aura.applyHit(Element.ANEMO, 1, 0).getFirst();
        assertEquals(new Reaction(Reaction.Type.SWIRL, 0.5, Element.ANEMO, existing), swirl);
        assertEquals(0.3, aura.gauge(existing), EPS);
        aura.applyHit(Element.ANEMO, 4, 0);
        assertEquals(0, aura.gauge(existing), EPS);
    }

    @ParameterizedTest
    @CsvSource({"HYDRO,ELECTRO", "ELECTRO,HYDRO"})
    void electroChargedCoexistsThenConsumesBothOnSourcedTicks(Element existing, Element trigger) {
        // spec/mechanics/elements.md: EC initial tick then 1s=60f, .4U from EACH aura.
        AuraState aura = new AuraState();
        aura.applyHit(existing, 2, 0);
        assertEquals(Reaction.Type.ELECTRO_CHARGED, aura.applyHit(trigger, 2, 0).getFirst().type());
        assertEquals(1.6, aura.gauge(existing), EPS);
        assertEquals(1.6, aura.gauge(trigger), EPS);
        assertEquals(OptionalLong.of(0), aura.nextElectroChargedTickFrame());
        AuraState.ElectroChargedTick first = aura.tickElectroCharged(0, true);
        assertTrue(first.dealsDamage());
        assertEquals(0.4, first.hydroConsumed(), EPS);
        assertEquals(0.4, first.electroConsumed(), EPS);
        assertEquals(OptionalLong.of(60), first.nextTickFrame());
        AuraState.ElectroChargedTick second = aura.tickElectroCharged(60, true);
        assertTrue(second.dealsDamage());
        assertEquals(0.4, second.hydroConsumed(), EPS);
        assertEquals(0.4, second.electroConsumed(), EPS);
        assertEquals(2.0 / 3, aura.gauge(Element.HYDRO), EPS);
        assertEquals(2.0 / 3, aura.gauge(Element.ELECTRO), EPS);
        assertFalse(aura.tickElectroCharged(60, true).dealsDamage());
    }

    @Test
    void rejectedElectroChargedDamageConsumesNoGaugeButNaturalDecayContinues() {
        // spec/mechanics/elements.md: EC consumes gauge only on accepted damage.
        AuraState aura = new AuraState();
        aura.applyHit(Element.HYDRO, 1, 0);
        aura.applyHit(Element.ELECTRO, 1, 0);
        assertFalse(aura.tickElectroCharged(0, false).dealsDamage());
        assertEquals(0.8, aura.gauge(Element.HYDRO), EPS);
        assertEquals(0.8, aura.gauge(Element.ELECTRO), EPS);
        aura.tickElectroCharged(60, false);
        assertEquals(68.0 / 95, aura.gauge(Element.HYDRO), EPS);
        assertEquals(68.0 / 95, aura.gauge(Element.ELECTRO), EPS);
    }

    @Test
    void electroChargedCanLeaveTheOtherAuraAndZeroGaugeCannotTriggerIt() {
        // spec/mechanics/elements.md: baseline surviving-other-aura rule; do not import
        // the unresolved historical Electro-trigger residue bug.
        AuraState aura = new AuraState();
        aura.applyHit(Element.HYDRO, 0.5, 0);
        assertTrue(aura.applyHit(Element.ELECTRO, 0, 0).isEmpty());
        assertTrue(aura.nextElectroChargedTickFrame().isEmpty());
        aura.applyHit(Element.ELECTRO, 2, 0);
        aura.tickElectroCharged(0, true);
        assertEquals(0, aura.gauge(Element.HYDRO), EPS);
        assertEquals(1.2, aura.gauge(Element.ELECTRO), EPS);
        assertTrue(aura.nextElectroChargedTickFrame().isEmpty());
    }

    @Test
    void doubleSwirlUsesElectroThresholdNotIndependentPairEvaluation() {
        // spec/mechanics/elements.md: multiple auras; double Swirl only if .5u > Electro U.
        AuraState single = new AuraState();
        single.applyHit(Element.HYDRO, 2, 0);
        single.applyHit(Element.ELECTRO, 1, 0);
        List<Reaction> one = single.applyHit(Element.ANEMO, 1, 0);
        assertEquals(1, one.size());
        assertEquals(Element.ELECTRO, one.getFirst().auraElement());
        assertEquals(1.6, single.gauge(Element.HYDRO), EPS);
        AuraState both = new AuraState();
        both.applyHit(Element.HYDRO, 2, 0);
        both.applyHit(Element.ELECTRO, 1, 0);
        List<Reaction> two = both.applyHit(Element.ANEMO, 2, 0);
        assertEquals(List.of(Element.ELECTRO, Element.HYDRO),
                two.stream().map(Reaction::auraElement).toList());
        assertEquals(0.6, both.gauge(Element.HYDRO), EPS);
    }

    @ParameterizedTest
    @CsvSource({"HYDRO,CRYO", "CRYO,HYDRO"})
    void frozenFormationKeepsUnderlyingAuraAndDecaysQuadratically(Element existing, Element trigger) {
        // spec/mechanics/elements.md: Frozen F=2min(a,u), no additional tax; F(t)=F-.4t-.05t².
        AuraState aura = new AuraState();
        aura.applyHit(existing, 2, 0);
        Reaction freeze = aura.applyHit(trigger, 1, 0).getFirst();
        assertEquals(new Reaction(Reaction.Type.FROZEN, 1, trigger, existing), freeze);
        assertEquals(2, aura.frozenGauge(), EPS);
        assertEquals(0.6, aura.gauge(existing), EPS);
        assertEquals(0, aura.gauge(trigger), EPS);
        assertEquals(2 * Math.sqrt(14) - 4, aura.remainingFrozenSeconds(), EPS);
        aura.advanceTo(60);
        assertEquals(1.55, aura.frozenGauge(), EPS);
        aura.advanceTo(210);
        assertFalse(aura.isFrozen());
    }

    @Test
    void freezingUsesActuallyDecayedGaugeAndResistanceChangesOnlyDuration() {
        // spec/mechanics/elements.md: actual remaining aura and corrected freeze-resistance formula.
        AuraState decayed = new AuraState();
        decayed.applyHit(Element.HYDRO, 1, 0);
        decayed.applyHit(Element.CRYO, 1, 285);
        assertEquals(0.8, decayed.frozenGauge(), EPS);
        AuraState resistant = new AuraState(new AuraState.Parameters(30, 0.5));
        resistant.applyHit(Element.HYDRO, 2, 0);
        resistant.applyHit(Element.CRYO, 1, 0);
        assertEquals(2, resistant.frozenGauge(), EPS);
        assertEquals(2, resistant.remainingFrozenSeconds(), EPS);
        resistant.advanceTo(120);
        assertFalse(resistant.isFrozen());
    }

    @Test
    void nonBluntPyroMeltsFrozenRatherThanVaporizingUnderlyingHydro() {
        // spec/mechanics/elements.md: underlying aura priority for non-blunt Pyro.
        AuraState aura = new AuraState();
        aura.applyHit(Element.HYDRO, 2, 0);
        aura.applyHit(Element.CRYO, 1, 0);
        List<Reaction> reactions = aura.applyHit(Element.PYRO, 1, 0);
        assertEquals(1, reactions.size());
        assertEquals(Reaction.Type.MELT, reactions.getFirst().type());
        assertEquals(0.6, aura.gauge(Element.HYDRO), EPS);
        assertFalse(aura.isFrozen());
    }

    @Test
    void frozenHydroSwirlUsesHydroThreshold() {
        // spec/mechanics/elements.md: Frozen with Hydro swirls Hydro alone below its threshold.
        AuraState aura = new AuraState();
        aura.applyHit(Element.HYDRO, 2, 0);
        aura.applyHit(Element.CRYO, 1, 0);
        List<Reaction> reactions = aura.applyHit(Element.ANEMO, 1, 0);
        assertEquals(1, reactions.size());
        assertEquals(Element.HYDRO, reactions.getFirst().auraElement());
        assertEquals(2, aura.frozenGauge(), EPS);
        reactions = aura.applyHit(Element.ANEMO, 1, 0);
        assertEquals(List.of(Element.HYDRO, Element.CRYO),
                reactions.stream().map(Reaction::auraElement).toList());
    }

    @Test
    void shatterRequiresFrozenAfterCallerSuppliedPreConsumptionAndExposesUnderlyingAura() {
        // spec/mechanics/elements.md: Shatter removes 8U, but only AFTER the unknown
        // poise-to-gauge pre-step. Explicit zero-loss input is not a claimed universal coefficient.
        final double noPreShatterLoss = 0;
        AuraState aura = new AuraState();
        aura.applyHit(Element.HYDRO, 2, 0);
        aura.applyHit(Element.CRYO, 1, 0);
        Reaction shatter = aura.shatter(0, noPreShatterLoss).orElseThrow();
        assertEquals(Reaction.Type.SHATTER, shatter.type());
        assertEquals(2, shatter.gaugeConsumed(), EPS);
        assertFalse(aura.isFrozen());
        assertEquals(Reaction.Type.VAPORIZE, aura.applyHit(Element.PYRO, 1, 0).getFirst().type());
        AuraState preConsumed = new AuraState();
        preConsumed.applyHit(Element.HYDRO, 1, 0);
        preConsumed.applyHit(Element.CRYO, 1, 0);
        assertTrue(preConsumed.shatter(0, 2).isEmpty());
        AuraState strongFreeze = new AuraState();
        strongFreeze.applyHit(Element.HYDRO, 6, 0);
        strongFreeze.applyHit(Element.CRYO, 6, 0);
        assertEquals(8, strongFreeze.shatter(0, noPreShatterLoss).orElseThrow().gaugeConsumed(), EPS);
        assertTrue(strongFreeze.isFrozen());
    }

    @Test
    void superconductPhysicalDebuffPersistsIndependentOfDamageIcdAndRefreshes() {
        // spec/mechanics/damage.md: Superconduct lowers Physical RES by .4 for 12s=720f.
        AuraState aura = new AuraState();
        aura.applyHit(Element.CRYO, 2, 0);
        aura.applyHit(Element.ELECTRO, 1, 0);
        assertEquals(0.4, aura.physicalResistanceReduction(), EPS);
        assertEquals(720, aura.physicalResistanceReductionUntilFrame());
        aura.applyHit(Element.ELECTRO, 1, 1);
        assertEquals(721, aura.physicalResistanceReductionUntilFrame());
        aura.advanceTo(720);
        assertEquals(0.4, aura.physicalResistanceReduction(), EPS);
        aura.advanceTo(721);
        assertEquals(0, aura.physicalResistanceReduction(), EPS);
    }

    @Test
    void electroChargedExpiryUsesTheSourcedFinalTickExclusionWindow() {
        // spec/mechanics/elements.md: a naturally expiring gauge can have an early final
        // tick only outside the .5s exclusion window. Exact expiry/cooldown boundary
        // ordering is unknown: use inputs safely away from boundaries, not exact timestamps.
        AuraState early = new AuraState();
        early.applyHit(Element.HYDRO, 0.54, 0);
        early.applyHit(Element.ELECTRO, 2, 0);
        early.tickElectroCharged(0, true);
        long earlyFrame = early.nextElectroChargedTickFrame().orElseThrow();
        assertTrue(earlyFrame > 30 && earlyFrame < 60);
        assertTrue(early.tickElectroCharged(earlyFrame, true).dealsDamage());
        assertTrue(early.nextElectroChargedTickFrame().isEmpty());
        AuraState excluded = new AuraState();
        excluded.applyHit(Element.HYDRO, 0.51, 0);
        excluded.applyHit(Element.ELECTRO, 2, 0);
        excluded.tickElectroCharged(0, true);
        assertEquals(OptionalLong.of(60), excluded.nextElectroChargedTickFrame());
        assertFalse(excluded.tickElectroCharged(60, true).dealsDamage());
        assertTrue(excluded.gauge(Element.ELECTRO) > 0);
    }

    @Test
    void electroChargedTicksRunAtSourceFramesWhenServerDrainsEveryThreeFrames() {
        // elements.md: initial +60f EC tick; brief Hard problem 5: preserve source frames.
        AuraState aura = new AuraState();
        EventTimeline timeline = new EventTimeline();
        aura.applyHit(Element.HYDRO, 2, 23);
        aura.applyHit(Element.ELECTRO, 2, 23);
        timeline.schedule(23, frame -> {
            assertTrue(aura.tickElectroCharged(frame, true).dealsDamage());
            timeline.schedule(aura.nextElectroChargedTickFrame().orElseThrow(), next -> {
                assertEquals(83, next);
                assertTrue(aura.tickElectroCharged(next, true).dealsDamage());
            });
        });
        timeline.advanceTo(24);
        assertEquals(23, aura.frame());
        timeline.advanceTo(84);
        assertEquals(83, aura.frame());
        assertEquals(2.0 / 3, aura.gauge(Element.HYDRO), EPS);
    }
}

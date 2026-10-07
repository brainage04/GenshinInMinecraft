package io.github.brainage04.genshininminecraft.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

class HudFormattingTest {
    @Test void cooldownRoundsUpWithoutShowingZeroBeforeReady() {
        assertEquals("", HudFormatting.cooldown(0));
        assertEquals("", HudFormatting.cooldown(-1));
        assertEquals("0.1", HudFormatting.cooldown(1));
        assertEquals("0.1", HudFormatting.cooldown(6));
        assertEquals("0.2", HudFormatting.cooldown(7));
        assertEquals("1.0", HudFormatting.cooldown(60));
        assertEquals("1.1", HudFormatting.cooldown(61));
        assertEquals("9.9", HudFormatting.cooldown(594));
        assertEquals("10", HudFormatting.cooldown(595));
        assertEquals("10", HudFormatting.cooldown(599));
        assertEquals("10", HudFormatting.cooldown(600));
        assertEquals("11", HudFormatting.cooldown(601));
        assertEquals("15", HudFormatting.cooldown(900));
        assertEquals("35791395", HudFormatting.cooldown(Integer.MAX_VALUE));
    }

    @Test void sweepMeasuresRemainingShareWithoutEarlyReadinessOrUnknownDivision() {
        assertEquals(1, HudFormatting.sweepFraction(60, 60));
        assertEquals(.5, HudFormatting.sweepFraction(30, 60));
        assertEquals(1.0 / 60, HudFormatting.sweepFraction(1, 60));
        assertEquals(0, HudFormatting.sweepFraction(0, 60));
        assertEquals(0, HudFormatting.sweepFraction(-1, 60));
        assertEquals(1, HudFormatting.sweepFraction(87, 60)); // Cast startup before CD's start.
        assertEquals(1, HudFormatting.sweepFraction(1, 0));
        assertEquals(0, HudFormatting.sweepFraction(0, 0));
    }

    @Test void radialMaskStartsAtTwelveAndSweepsClockwiseThroughTheFourQuadrants() {
        assertFalse(HudFormatting.radialCovered(0, -1, 0));
        assertTrue(HudFormatting.radialCovered(0, -1, .25));
        assertTrue(HudFormatting.radialCovered(1, -1, .25));
        assertFalse(HudFormatting.radialCovered(1, 0, .25));
        assertTrue(HudFormatting.radialCovered(1, 0, .5));
        assertFalse(HudFormatting.radialCovered(0, 1, .5));
        assertTrue(HudFormatting.radialCovered(0, 1, .75));
        assertFalse(HudFormatting.radialCovered(-1, 0, .75));
        assertTrue(HudFormatting.radialCovered(-1, -1, 1));
    }

    @Test void partyDimmingSeparatesCurrentFallenCooldownAndIndefiniteActionLocks() {
        assertFalse(HudFormatting.partyUnavailable(false, 100, 0, false));
        assertTrue(HudFormatting.partyUnavailable(false, 100, 60, false));
        assertTrue(HudFormatting.partyUnavailable(false, 100, 0, true));
        assertFalse(HudFormatting.partyUnavailable(true, 100, 60, true));
        assertTrue(HudFormatting.partyUnavailable(false, 0, 0, false));
        assertTrue(HudFormatting.partyUnavailable(true, 0, 0, false));
    }

    @Test void simultaneousDamageUsesDistinctBillboardLanesAndWrapsSafely() {
        assertEquals(-32, HudFormatting.damageLane(0));
        assertEquals(0, HudFormatting.damageLane(1));
        assertEquals(32, HudFormatting.damageLane(2));
        assertEquals(-32, HudFormatting.damageLane(3));
        assertEquals(32, HudFormatting.damageLane(-1));
        assertEquals(0, HudFormatting.damageLane(Integer.MIN_VALUE));
    }

    @Test void resourceFractionClampsEmptyFullOverfullAndInvalidState() {
        assertEquals(0, HudFormatting.fraction(0, 60));
        assertEquals(.5, HudFormatting.fraction(30, 60));
        assertEquals(1, HudFormatting.fraction(60, 60));
        assertEquals(1, HudFormatting.fraction(61, 60));
        assertEquals(0, HudFormatting.fraction(-1, 60));
        assertEquals(0, HudFormatting.fraction(1, 0));
        assertEquals(0, HudFormatting.fraction(1, -1));
        assertEquals(0, HudFormatting.fraction(Double.NaN, 60));
        assertEquals(0, HudFormatting.fraction(1, Double.POSITIVE_INFINITY));
    }
}

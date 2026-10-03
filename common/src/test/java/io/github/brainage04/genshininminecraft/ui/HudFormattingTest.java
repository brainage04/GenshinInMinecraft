package io.github.brainage04.genshininminecraft.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

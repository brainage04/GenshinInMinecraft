package io.github.brainage04.genshininminecraft.ui;

import static org.junit.jupiter.api.Assertions.*;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Reaction;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class DamageNumberAnimationTest {
    @Test void popOvershootsThenSettlesWithinOneHundredFiftyMilliseconds() {
        assertEquals(.6, DamageNumberAnimation.pop(0), 1e-12);
        assertEquals(1.28, DamageNumberAnimation.pop(.075), 1e-12);
        assertEquals(1, DamageNumberAnimation.pop(.15), 1e-12);
        assertEquals(1, DamageNumberAnimation.pop(1), 1e-12);
        assertEquals(.6, DamageNumberAnimation.pop(-1), 1e-12);
        for (int frame = 1; frame <= 150; frame++) {
            double age = frame / 1000.0;
            double value = DamageNumberAnimation.pop(age);
            assertTrue(value >= .6 && value <= 1.28);
            assertTrue(Math.abs(value - DamageNumberAnimation.pop(age - .001)) < .02);
        }
        assertTrue(DamageNumberAnimation.CRITICAL_SCALE > 1);
    }
    @Test void floatAndLateFadeAreMonotonicAndClamped() {
        assertEquals(0, DamageNumberAnimation.rise(-1));
        assertEquals(0, DamageNumberAnimation.rise(0));
        assertEquals(22, DamageNumberAnimation.rise(1.1));
        assertEquals(22, DamageNumberAnimation.rise(2));
        assertEquals(1, DamageNumberAnimation.alpha(0));
        assertEquals(1, DamageNumberAnimation.alpha(.65));
        assertEquals(.5, DamageNumberAnimation.alpha(.875), 1e-12);
        assertEquals(0, DamageNumberAnimation.alpha(1.1));
        assertEquals(0, DamageNumberAnimation.alpha(2));
        for (int frame = 1; frame <= 110; frame++) {
            double age = frame / 100.0;
            assertTrue(DamageNumberAnimation.rise(age) >= DamageNumberAnimation.rise(age - .01));
            assertTrue(DamageNumberAnimation.alpha(age) <= DamageNumberAnimation.alpha(age - .01));
        }
    }
    @Test void cosmeticDriftIsDeterministicBoundedAndBothDirections() {
        var drifts = new HashSet<Double>();
        for (int index = -100; index <= 100; index++) {
            double drift = DamageNumberAnimation.drift(index);
            assertEquals(drift, DamageNumberAnimation.drift(index));
            assertTrue(Math.abs(drift) <= 7);
            assertEquals(0, DamageNumberAnimation.horizontal(0, drift));
            assertEquals(drift, DamageNumberAnimation.horizontal(1.1, drift));
            assertEquals(drift, DamageNumberAnimation.horizontal(10, drift));
            drifts.add(drift);
        }
        assertTrue(drifts.size() > 150);
        assertTrue(drifts.stream().anyMatch(drift -> drift > 0));
        assertTrue(drifts.stream().anyMatch(drift -> drift < 0));
        assertTrue(Math.abs(DamageNumberAnimation.drift(Integer.MIN_VALUE)) <= 7);
        assertTrue(Math.abs(DamageNumberAnimation.drift(Integer.MAX_VALUE)) <= 7);
    }
    @Test void simultaneousSlotsNeverWrapAndLeaveRoomForCritAndReaction() {
        var offsets = new HashSet<Double>();
        double combinedHeight = 9 * 1.28 * DamageNumberAnimation.CRITICAL_SCALE
                - DamageNumberAnimation.REACTION_Y + 9 * 1.28 * DamageNumberAnimation.REACTION_SCALE;
        assertTrue(DamageNumberAnimation.STACK_SPACING > combinedHeight);
        for (int slot = 0; slot < 100; slot++) {
            assertTrue(offsets.add(DamageNumberAnimation.stackY(slot)));
            assertTrue(Math.abs(DamageNumberAnimation.stackX(slot)) <= 12);
            assertEquals(slot * DamageNumberAnimation.STACK_SPACING, DamageNumberAnimation.stackY(slot));
        }
        assertEquals(0, DamageNumberAnimation.stackY(-1));
        assertEquals(-12, DamageNumberAnimation.stackX(0));
        assertEquals(0, DamageNumberAnimation.stackX(1));
        assertEquals(12, DamageNumberAnimation.stackX(2));
    }
    @Test void cameraDistancePreservesAngularSizeInsteadOfShrinkingFarHits() {
        assertEquals(.0055, DamageNumberAnimation.distanceScale(0), 1e-8);
        double angularSize = DamageNumberAnimation.distanceScale(4) / 4;
        for (double distance : new double[] {4, 8, 16, 32, 64})
            assertEquals(angularSize, DamageNumberAnimation.distanceScale(distance) / distance, 1e-8);
    }
    @Test void allDamageElementsHaveTheRequestedOpaquePalette() {
        assertEquals(0xff72e2c3, ElementPalette.color(Element.ANEMO));
        assertEquals(0xffff784c, ElementPalette.color(Element.PYRO));
        assertEquals(0xffa2e8f5, ElementPalette.color(Element.CRYO));
        assertEquals(0xffcf9bff, ElementPalette.color(Element.ELECTRO));
        assertEquals(0xff69baff, ElementPalette.color(Element.HYDRO));
        assertEquals(0xffffffff, ElementPalette.color(Element.PHYSICAL));
        for (Element element : Element.values()) assertEquals(255, ElementPalette.color(element) >>> 24);
    }
    @Test void reactionColoursAreIndependentOfTheirDamageElement() {
        assertEquals(ElementPalette.color(Element.ANEMO), ElementPalette.reactionColor(Reaction.Type.SWIRL));
        assertEquals(0xffffcb73, ElementPalette.reactionColor(Reaction.Type.MELT));
        assertEquals(0xffffcb73, ElementPalette.reactionColor(Reaction.Type.VAPORIZE));
        assertEquals(0xffff6372, ElementPalette.reactionColor(Reaction.Type.OVERLOADED));
        assertEquals(0xffb5a4ff, ElementPalette.reactionColor(Reaction.Type.SUPERCONDUCT));
        assertEquals(ElementPalette.color(Element.ELECTRO), ElementPalette.reactionColor(Reaction.Type.ELECTRO_CHARGED));
        assertEquals(ElementPalette.color(Element.CRYO), ElementPalette.reactionColor(Reaction.Type.FROZEN));
        assertEquals(ElementPalette.color(Element.PHYSICAL), ElementPalette.reactionColor(Reaction.Type.SHATTER));
        assertNotEquals(ElementPalette.color(Element.PYRO), ElementPalette.reactionColor(Reaction.Type.SWIRL));
        for (Reaction.Type reaction : Reaction.Type.values()) {
            assertEquals(255, ElementPalette.reactionColor(reaction) >>> 24);
            assertFalse(ElementPalette.reaction(reaction).isBlank());
        }
    }
}

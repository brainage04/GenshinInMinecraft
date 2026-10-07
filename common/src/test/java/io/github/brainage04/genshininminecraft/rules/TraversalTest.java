package io.github.brainage04.genshininminecraft.rules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TraversalTest {
    @Test void climbingRequiresFiveAndValidAttachment() {
        var stamina = new Stamina();
        var traversal = new Traversal(stamina);
        stamina.consume(96, 0);
        assertFalse(traversal.attach(0));
        stamina.advanceTo(93);
        assertTrue(traversal.attach(93)); // 5.25, threshold sourced5.
        traversal.tick(true, false, 96);
        assertEquals(Traversal.Mode.FREE, traversal.mode());
        assertFalse(traversal.attach(96));
    }
    @Test void movingClimbDrainsOwnerCalibratedFivePointThreeSixPerSecondButStationaryAttachmentNeverRegenerates() {
        var stamina = new Stamina();
        var traversal = new Traversal(stamina);
        assertTrue(traversal.attach(0));
        traversal.tick(true, true, 0);
        traversal.tick(false, true, 60);
        assertEquals(94.64, stamina.current(), 1e-9);
        traversal.tick(false, true, 600);
        assertEquals(94.64, stamina.current(), 1e-9);
        traversal.detach(600);
        stamina.advanceTo(690);
        assertEquals(94.64, stamina.current(), 1e-9);
        stamina.advanceTo(750);
        assertEquals(100, stamina.current());
    }
    @Test void climbJumpCostsTwentyFiveOnceAndHasFiniteDirectionalBurst() {
        var stamina = new Stamina();
        var traversal = new Traversal(stamina);
        traversal.attach(0);
        assertTrue(traversal.climbJump(1, 0));
        assertEquals(75, stamina.current());
        assertTrue(traversal.jumping(17));
        assertEquals(1, traversal.jumpSide());
        assertFalse(traversal.jumping(18));
        stamina.consume(55, 0);
        assertFalse(traversal.climbJump(0, 18));
        assertEquals(20, stamina.current());
    }
    @Test void climbExhaustionDetachesWithoutRegeneratingOnTheWall() {
        var stamina = new Stamina();
        var traversal = new Traversal(stamina);
        stamina.consume(95, 0);
        assertTrue(traversal.attach(0));
        traversal.tick(true, true, 0);
        traversal.tick(true, true, 57);
        assertEquals(0, stamina.current());
        assertEquals(Traversal.Mode.FREE, traversal.mode());
        assertTrue(stamina.exhausted());
        assertFalse(traversal.attach(90));
    }
    @Test void spendingLastClimbJumpCostAlsoForcesFall() {
        var stamina = new Stamina();
        var traversal = new Traversal(stamina);
        stamina.consume(75, 0);
        traversal.attach(0);
        assertTrue(traversal.climbJump(0, 0));
        traversal.tick(false, true, 0);
        assertEquals(Traversal.Mode.FREE, traversal.mode());
    }
    @Test void glideNeedsTwoMetresAndDrainsApproximateSourcedThreePerSecondEvenWithoutInput() {
        var stamina = new Stamina();
        var traversal = new Traversal(stamina);
        assertFalse(traversal.openGlider(1.99, 0));
        assertTrue(traversal.openGlider(2, 0));
        traversal.tick(false, true, 60);
        assertEquals(97, stamina.current(), 1e-9);
        traversal.tick(false, false, 63); // Landing/interruption closes.
        assertEquals(Traversal.Mode.FREE, traversal.mode());
    }
    @Test void gliderExhaustionClosesAndPreventsRedeployUntilRecovered() {
        var stamina = new Stamina();
        var traversal = new Traversal(stamina);
        stamina.consume(97, 0);
        assertTrue(traversal.openGlider(10, 0));
        traversal.tick(false, true, 60);
        assertEquals(0, stamina.current());
        assertEquals(Traversal.Mode.FREE, traversal.mode());
        assertFalse(traversal.openGlider(10, 63));
        assertTrue(traversal.openGlider(10, 187)); // API advances recovery itself:90-frame delay + >15 recovery.
    }
    @Test void fallCurveHasSafeDamagingAndLethalBoundaries() {
        assertEquals(0, Traversal.fallHpFraction(4, 0, false));
        assertEquals(0, Traversal.fallHpFraction(5, 0, false));
        assertEquals(.5, Traversal.fallHpFraction(15, 0, false));
        assertEquals(1, Traversal.fallHpFraction(25, 0, false));
        assertEquals(1, Traversal.fallHpFraction(100, 0, false));
    }
    @Test void horizontalImpactEnergyAddsRiskButDeepWaterNegatesIt() {
        assertEquals(.625, Traversal.fallHpFraction(15, 10, false)); // 10²/(2*20)=2.5 equivalent metres.
        assertEquals(0, Traversal.fallHpFraction(100, 10, true));
        assertEquals(0, Traversal.fallHpFraction(0, 10, false));
    }
}

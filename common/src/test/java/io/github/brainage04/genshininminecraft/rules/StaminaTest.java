package io.github.brainage04.genshininminecraft.rules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaminaTest {
    @Test void newPlayerCapacityIsNotTheStatueUpgradedCap() {
        assertEquals(100, new Stamina().maximum()); // stamina.md: new-account base, not 240.
        assertEquals(240, new Stamina(240).maximum());
    }

    @Test void sprintStartAndOneSecondDrainUseCurrentWikiEighteen() {
        var stamina = new Stamina();
        assertTrue(stamina.startSprint(0));
        assertEquals(82, stamina.current());
        for (int tick = 1; tick <= 20; tick++) stamina.advanceTo(tick * 3L);
        assertEquals(64, stamina.current(), 1e-9); // 100 - 18 start - 18/s.
        assertTrue(stamina.sprinting());
    }

    @Test void regenerationWaitsNinetyFramesThenRestoresTwentyFivePerSecond() {
        var stamina = new Stamina();
        assertTrue(stamina.consume(50, 0));
        stamina.advanceTo(89);
        assertEquals(50, stamina.current());
        stamina.advanceTo(90);
        assertEquals(50, stamina.current());
        stamina.advanceTo(150);
        assertEquals(75, stamina.current(), 1e-9); // wiki 1.5s delay and 25/s, not KQM 1s/30.
        stamina.advanceTo(300);
        assertEquals(100, stamina.current());
    }

    @Test void stoppingSprintRestartsRecoveryDelayAndRejectedSpendDoesNotDelayIt() {
        var stamina = new Stamina();
        stamina.startSprint(0);
        stamina.stopSprint(60);
        stamina.advanceTo(149);
        assertEquals(64, stamina.current(), 1e-9);
        assertFalse(stamina.consume(100, 149));
        stamina.advanceTo(210);
        assertEquals(89, stamina.current(), 1e-9);
    }

    @Test void exhaustionLocksSprintDashAndChargeUntilPastNamedRecoveryThreshold() {
        var stamina = new Stamina();
        assertTrue(stamina.consume(100, 0));
        assertTrue(stamina.exhausted());
        assertFalse(stamina.startSprint(0));
        assertFalse(stamina.dash(0));
        assertFalse(stamina.chargedAttack(0));
        stamina.advanceTo(126); // 90 + 36; 25/s restores exactly 15.
        assertEquals(15, stamina.current(), 1e-9);
        assertTrue(stamina.exhausted());
        assertFalse(stamina.dash(126));
        stamina.advanceTo(127);
        assertFalse(stamina.exhausted());
        assertFalse(stamina.dash(127)); // unlocked but still cannot pay the full 18.
        stamina.advanceTo(138);
        assertEquals(20, stamina.current(), 1e-9);
        assertTrue(stamina.chargedAttack(138)); // Sword charge costs 20.
        assertEquals(0, stamina.current(), 1e-9);
        assertTrue(stamina.exhausted());
    }

    @Test void longAdvanceThroughSprintExhaustionAlsoAccountsForDelayedRecovery() {
        var stamina = new Stamina();
        stamina.startSprint(0);
        stamina.advanceTo(274); // ceil(82 / .3) frames: continuous drain clamps to zero.
        assertEquals(0, stamina.current());
        assertFalse(stamina.sprinting());
        assertTrue(stamina.exhausted());
        stamina.advanceTo(364);
        assertEquals(0, stamina.current());
        stamina.advanceTo(400);
        assertEquals(15, stamina.current(), 1e-9);
        assertTrue(stamina.exhausted());
        stamina.advanceTo(401);
        assertFalse(stamina.exhausted());
        var oneAdvance = new Stamina();
        oneAdvance.startSprint(0);
        oneAdvance.advanceTo(401);
        assertEquals(stamina.current(), oneAdvance.current(), 1e-9);
    }

    @Test void dashCostsEighteenOnceAndHeldDrainBeginsAfterTheAdaptedBurst() {
        var stamina = new Stamina();
        assertTrue(stamina.dash(0));
        assertEquals(82, stamina.current());
        assertFalse(stamina.dash(3));
        stamina.advanceTo(18);
        assertEquals(82, stamina.current());
        stamina.advanceTo(78);
        assertEquals(64, stamina.current(), 1e-9);
        stamina.stopSprint(78);
        stamina.advanceTo(168);
        assertEquals(64, stamina.current(), 1e-9);
        stamina.advanceTo(228);
        assertEquals(89, stamina.current(), 1e-9);
    }

    @Test void dashProtectionHasOneTickStartupSixTickWindowAndNoPermanentSprintImmunity() {
        var stamina = new Stamina();
        assertTrue(stamina.dash(0));
        assertFalse(stamina.dashInvulnerable(0));
        assertFalse(stamina.dashInvulnerable(2));
        assertTrue(stamina.dashInvulnerable(3));
        assertTrue(stamina.dashInvulnerable(20));
        assertFalse(stamina.dashInvulnerable(21));
        assertFalse(stamina.dashInvulnerable(60));
    }

    @Test void costsAreAllOrNothingAndInvalidTimeCannotRefillStamina() {
        var stamina = new Stamina();
        assertTrue(stamina.consume(81, 0));
        assertFalse(stamina.chargedAttack(0));
        assertEquals(19, stamina.current());
        assertThrows(IllegalArgumentException.class, () -> stamina.consume(Double.NaN, 0));
        stamina.advanceTo(3);
        assertThrows(IllegalArgumentException.class, () -> stamina.advanceTo(2));
    }
}

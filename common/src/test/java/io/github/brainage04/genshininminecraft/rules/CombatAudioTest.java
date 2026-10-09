package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.CombatAudio.Cue;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatAudioTest {
    @Test void roseAndElectroDoNotDoubleInOneTick() {
        var budget = new CombatAudio.Budget();
        assertTrue(budget.allow(Cue.ROSE, 300, 100));
        assertFalse(budget.allow(Cue.ELECTRO, 301, 100));
        assertFalse(budget.allow(Cue.ELECTRO_CHARGED, 302, 100));
        assertTrue(budget.allow(Cue.ELECTRO, 303, 101));
    }
    @Test void electroAndReactionDoNotDoubleInEitherOrder() {
        var budget = new CombatAudio.Budget();
        assertTrue(budget.allow(Cue.ELECTRO, 300, 100));
        assertFalse(budget.allow(Cue.ELECTRO_CHARGED, 302, 100));
        budget = new CombatAudio.Budget();
        assertTrue(budget.allow(Cue.ELECTRO_CHARGED, 300, 100));
        assertFalse(budget.allow(Cue.ELECTRO, 302, 100));
    }
    @Test void catchupHitmarksUseTheActualPlaybackTick() {
        var budget = new CombatAudio.Budget();
        assertTrue(budget.allow(Cue.ROSE, 300, 200));
        assertFalse(budget.allow(Cue.ELECTRO, 330, 200));
        assertTrue(budget.allow(Cue.CRIT, 330, 200));
    }
    @Test void existingCueIntervalsStillCoalesceAoe() {
        var budget = new CombatAudio.Budget();
        assertTrue(budget.allow(Cue.CRYO, 300, 100));
        assertFalse(budget.allow(Cue.CRYO, 305, 101));
        assertTrue(budget.allow(Cue.CRYO, 306, 102));
        assertTrue(budget.allow(Cue.ELECTRO_CHARGED, 306, 102));
        assertFalse(budget.allow(Cue.ELECTRO_CHARGED, 335, 111));
        assertTrue(budget.allow(Cue.ELECTRO_CHARGED, 336, 112));
    }
    @Test void enemyHurtIsAtMostOnePerEnemyPerTenTicksAndOneAcrossEnemiesPerTick() {
        var budget = new CombatAudio.EnemyHurtBudget();
        long[] last = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
        int count = 0;
        for (long tick = 100; tick < 120; tick++) {
            for (int enemy = 0; enemy < 3; enemy++) {
                for (int hit = 0; hit < 3; hit++) if (budget.allow(last[enemy], tick)) {
                    assertTrue(last[enemy] == Long.MIN_VALUE || tick - last[enemy] >= 10);
                    last[enemy] = tick; count++;
                }
            }
        }
        assertEquals(6, count);
        assertArrayEquals(new long[]{110, 111, 112}, last);
    }
    @Test void vanillaDamagingFallDoesNotGetAnotherModLanding() {
        assertFalse(CombatAudio.landing(40, false, true));
        assertFalse(CombatAudio.landing(4, false, true));
        assertTrue(CombatAudio.landing(2, false, false));
        assertTrue(CombatAudio.landing(40, false, false)); // Damage-immune fall still needs a landing cue.
        assertFalse(CombatAudio.landing(40, true, false));
        assertFalse(CombatAudio.landing(.15, false, false));
        assertFalse(CombatAudio.playerHurt(true, true)); // Later server fall HP receipt must not add a second oof.
        assertTrue(CombatAudio.playerHurt(true, false)); // Ordinary club damage remains audible.
        assertTrue(CombatAudio.playerHurt(false, true)); // Unmanaged vanilla behavior is untouched.
    }
    @Test void rejectionIsBurstOnlyAndAtMostOncePerSecond() {
        var budget = new CombatAudio.Budget();
        for (Intent intent : Intent.values()) if (intent != Intent.BURST_PRESS) assertFalse(budget.rejected(intent, 300));
        assertTrue(budget.rejected(Intent.BURST_PRESS, 300));
        assertFalse(budget.rejected(Intent.BURST_PRESS, 359));
        assertTrue(budget.rejected(Intent.BURST_PRESS, 360));
    }
    @Test void onlyActiveMemberReadinessRisingEdgePlaysAndSyncNeverReplaysIt() {
        var budget = new CombatAudio.Budget();
        int count = 0;
        for (int slot = 0; slot < 4; slot++) if (budget.burstReady(slot, 2, true, 100)) count++;
        assertEquals(1, count);
        for (int slot = 0; slot < 4; slot++) assertFalse(budget.burstReady(slot, 2, true, 100));
        assertFalse(budget.burstReady(0, 0, true, 101)); // Switching to already-ready off-field member is not an edge.
        assertFalse(budget.burstReady(2, 2, false, 101));
        assertTrue(budget.burstReady(2, 2, true, 101));
        assertFalse(budget.burstReady(2, 2, false, 101));
        assertFalse(budget.burstReady(2, 2, true, 101)); // Even two readiness edges cannot double this tick.
        assertFalse(budget.burstReady(2, 2, true, 102)); // Suppressed edge is still latched, not delayed.
    }
    @Test void replacementMappingsAreDistinctAndProjectileImpactIsNotMelee() {
        assertNotEquals(Cue.FROSTGNAW_CAST.event, Cue.FROSTGNAW_HIT.event);
        assertNotEquals(Cue.FALLEN.event, "entity.player.hurt");
        assertNotEquals(Cue.SWIRL.event, Cue.ANEMO.event);
        assertNotEquals(Cue.FROZEN.event, Cue.CRYO.event);
        assertNotEquals(Cue.BURST_CAST.event, Cue.SKILL_CAST.event);
        assertEquals(Cue.ARROW_HIT, CombatAudio.element(Element.PHYSICAL, true, false));
        assertEquals(Cue.PHYSICAL, CombatAudio.element(Element.PHYSICAL, false, false));
        for (Element element : Element.values()) assertNull(CombatAudio.element(element, false, true));
    }
}

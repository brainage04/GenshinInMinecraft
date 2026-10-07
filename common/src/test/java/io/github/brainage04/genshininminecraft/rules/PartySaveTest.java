package io.github.brainage04.genshininminecraft.rules;

import java.util.List;
import java.util.Map;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartySaveTest {
    private static Party party(EventTimeline clock) { return new Party(clock, (kit, hit) -> {}); }

    @Test void roundTripRebasesAbsoluteHpEnergyCooldownsSlotAndStaminaWithoutOfflineProgress() {
        var original = party(new EventTimeline());
        original.activeMember().setHp(1234.5);
        original.activeKit().intent(Intent.SKILL_PRESS, 0);
        original.activeKit().intent(Intent.SKILL_RELEASE, 0);
        original.activeMember().grantEnergy(60);
        original.activeKit().intent(Intent.BURST_PRESS, 74);
        original.advanceTo(180);
        assertTrue(original.switchTo(2, 180));
        original.activeMember().setHp(1700);
        original.activeMember().grantEnergy(23);
        original.members().get(1).setHp(0);
        assertTrue(original.stamina().consume(100, 180));
        PartySave saved = original.save();
        assertEquals(saved, PartySave.fromRecord(saved.toRecord()));
        var clock = new EventTimeline();
        clock.advanceTo(12000);
        var restored = party(clock);
        restored.restore(PartySave.fromRecord(saved.toRecord()));
        assertEquals(2, restored.activeSlot());
        assertEquals(1234.5, restored.members().getFirst().hp());
        assertEquals(1700, restored.activeMember().hp());
        assertEquals(23, restored.activeMember().energy());
        assertFalse(restored.members().get(1).alive());
        assertEquals(0, restored.members().get(1).energy());
        assertEquals(147, restored.kit(0).skillRemaining()); // tap starts27+300, saved at180.
        assertEquals(794, restored.kit(0).burstRemaining()); // burst74+900, saved at180.
        assertEquals(300, restored.members().getFirst().skillCooldownFrames());
        assertEquals(0, restored.stamina().current());
        assertTrue(restored.stamina().exhausted());
        assertEquals(90, restored.stamina().regenRemaining());
        assertEquals(60, restored.switchReadyFrame() - restored.frame());
        restored.advanceTo(12003);
        assertEquals(144, restored.kit(0).skillRemaining());
        assertEquals(791, restored.kit(0).burstRemaining());
        assertEquals(0, restored.stamina().current());
        restored.advanceTo(12126);
        assertEquals(15, restored.stamina().current());
        assertTrue(restored.stamina().exhausted());
        restored.advanceTo(12127);
        assertFalse(restored.stamina().exhausted());
    }
    @Test void v0MissingFieldsMigrateToStarterDefaultsAndInferFallenMembers() {
        var saved = PartySave.fromRecord(Map.of("members", List.of(Map.of("hp", 321.25, "energy", 7), Map.of("hp", 0))));
        assertEquals(PartySave.CURRENT_VERSION, saved.version());
        assertEquals(0, saved.activeSlot());
        assertEquals(100, saved.stamina());
        assertEquals(0, saved.staminaRegenRemaining());
        assertEquals(321.25, saved.members().getFirst().hp());
        assertEquals(7, saved.members().getFirst().energy());
        assertEquals(0, saved.members().getFirst().skillRemaining());
        assertTrue(saved.members().get(1).fallen());
        assertEquals(2506.36, saved.members().get(2).hp());
        assertEquals(saved, PartySave.fromRecord(saved.toRecord()));
    }
    @Test void loadClampsAbsoluteHpAndEnergyToCurrentMaximaAndKeepsFallenState() {
        var saved = PartySave.fromRecord(Map.of("version", 0, "members", List.of(
                Map.of("hp", 999999, "energy", 999), Map.of("hp", 800, "fallen", true))));
        var restored = party(new EventTimeline());
        restored.restore(saved);
        assertEquals(restored.activeMember().maxHp(), restored.activeMember().hp());
        assertEquals(60, restored.activeMember().energy());
        assertEquals(0, restored.members().get(1).hp());
        assertEquals(0, restored.members().get(1).energy());
    }
    @Test void futureVersionIsRejectedRatherThanResettingResources() {
        assertThrows(IllegalArgumentException.class, () -> PartySave.fromRecord(Map.of("version", 2)));
    }
    @Test void wipeRevivesEveryMemberToSourcedRounded35PercentWithoutClearingCooldowns() {
        var original = party(new EventTimeline());
        original.activeKit().intent(Intent.SKILL_PRESS, 0);
        original.activeKit().intent(Intent.SKILL_RELEASE, 0);
        for (var member : original.members()) member.setHp(0);
        original.reviveAfterWipe();
        for (var member : original.members()) {
            assertEquals(Math.round(member.maxHp() * .35), member.hp());
            assertEquals(0, member.energy());
        }
        assertEquals(327, original.activeKit().skillRemaining());
    }
}

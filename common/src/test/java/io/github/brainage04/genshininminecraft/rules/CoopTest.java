package io.github.brainage04.genshininminecraft.rules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoopTest {
    @Test void sourcedAllocationsAndHpMultipliers() {
        int[][] expected = {{4}, {2, 2}, {2, 1, 1}, {1, 1, 1, 1}};
        for (int count = 1; count <= 4; count++) {
            for (int player = 0; player < count; player++) assertEquals(expected[count - 1][player], Coop.capacity(count, player));
            assertEquals(new double[]{1, 1.5, 2, 2.5}[count - 1], Coop.enemyHpMultiplier(count));
        }
        assertThrows(IllegalArgumentException.class, () -> Coop.capacity(5, 0));
    }
    @Test void allocatedEnergyTableUsesOwnRosterAndRechargeAndLeavesDormantSlotsUntouched() {
        for (Energy.Item item : Energy.Item.values()) for (Element element : new Element[]{Element.ANEMO, Element.CRYO, null}) {
            var party = new Party(new EventTimeline(), (kit, hit) -> {}); party.allocate(5, 0);
            party.collect(item, element, 1);
            double orb = item == Energy.Item.ORB ? 3 : 1;
            assertEquals((element == null ? 2 : element == Element.ANEMO ? 3 : 1) * orb, party.kit(0).energy(), 1e-12);
            assertEquals((element == null ? 1.6 : element == Element.CRYO ? 2.4 : .8) * orb, party.kit(2).energy(), 1e-12);
            assertEquals(0, party.kit(1).energy()); assertEquals(0, party.kit(3).energy());
        }
    }
    @Test void allocationRestoresFourMembersWithoutBuffTriggerOrDormantResourceReset() {
        var party = new Party(new EventTimeline(), (kit, hit) -> {});
        party.kit(2).setHp(1234.5); party.kit(2).grantEnergy(17);
        party.allocate(3, 0); assertFalse(party.switchTo(2, 0));
        party.activeMember().setHp(0); assertTrue(party.forceSwitch(0));
        party.activeMember().setHp(0); assertFalse(party.forceSwitch(60)); assertTrue(party.wiped());
        party.reviveAfterWipe(); assertEquals(1234.5, party.kit(2).hp()); assertEquals(17, party.kit(2).energy());
        party.allocate(15, 120); assertEquals(4, party.allocatedCount()); assertTrue(party.switchTo(2, 120));
    }
    @Test void thrillingTalesSwitchBuffCannotTransferToAnIndependentlyActivePlayer() {
        var owner = new Party(new EventTimeline(), (kit, hit) -> {});
        var guest = new Party(new EventTimeline(), (kit, hit) -> {});
        owner.allocate(9, 0); guest.allocate(3, 0);
        assertTrue(owner.switchTo(3, 0)); assertTrue(owner.switchTo(0, 60));
        assertEquals((45.748752 + 93.753946) * 1.24, owner.kit(0).stats().atk(), 1e-12);
        assertEquals(45.748752 + 93.753946, guest.kit(0).stats().atk(), 1e-12);
    }
}

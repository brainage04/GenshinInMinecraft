package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartyTest {
    private static Party party() { return new Party(new EventTimeline(), (kit, hit) -> {}); }

    @Test void oneSecondSwitchCooldownRejectsEarlyRequestsAndDoesNotRefreshOnRejection() {
        var party = party();
        assertEquals(4, party.members().size()); // party.md solo capacity.
        assertTrue(party.switchTo(1, 0));
        assertFalse(party.switchTo(2, 59));
        assertEquals(1, party.activeSlot());
        assertTrue(party.switchTo(2, 60));
        assertFalse(party.switchTo(2, 120)); // Selecting active slot isn't a new switch.
        assertTrue(party.switchTo(3, 120));
    }
    @Test void fallenSelectionIsRejectedAndHpEnergyAndStaminaBelongToTheirCorrectOwners() {
        var party = party();
        var traveler = party.activeMember();
        traveler.setHp(1000);
        traveler.grantEnergy(20);
        party.stamina().consume(20, 0);
        party.members().get(1).setHp(0);
        assertFalse(party.switchTo(1, 0));
        assertFalse(party.switchTo(-1, 0));
        assertFalse(party.switchTo(4, 0));
        assertTrue(party.switchTo(2, 0));
        assertEquals(2506.36, party.activeMember().hp()); // damage.md level20/20 Kaeya.
        assertEquals(80, party.stamina().current());
        assertSame(party.stamina(), party.activeKit().stamina());
        assertTrue(party.switchTo(0, 60));
        assertSame(traveler, party.activeMember());
        assertEquals(1000, traveler.hp());
        assertEquals(20, traveler.energy());
        assertEquals(80, party.stamina().current()); // 90-frame recovery delay, no switch refill.
    }
    @Test void deathResetsOnlyFallenEnergyAndForcesCyclicNextAliveDespiteManualLock() {
        var party = party();
        party.members().get(1).setHp(0);
        party.activeMember().grantEnergy(60);
        assertTrue(party.activeKit().intent(Intent.SKILL_PRESS, 0));
        assertFalse(party.switchTo(2, 0), "Held Palm is non-cancellable");
        party.activeMember().setHp(0);
        assertEquals(0, party.members().getFirst().energy()); // party.md death reset.
        assertTrue(party.forceSwitch(0));
        assertEquals(2, party.activeSlot()); // Recorded cyclic-order adaptation skips fallen Amber.
        party.activeMember().setHp(0);
        assertTrue(party.forceSwitch(1)); // Bypasses existing 60-frame switch cooldown.
        assertEquals(3, party.activeSlot());
        party.activeMember().setHp(0);
        assertFalse(party.forceSwitch(2));
        assertTrue(party.members().stream().noneMatch(member -> member.alive()));
    }
    @Test void offFieldSkillAndBurstCooldownsContinueAndTravelerTornadoKeepsItsOwner() {
        var hits = new ArrayList<CharacterKit.Hit>();
        var party = new Party(new EventTimeline(), (kit, hit) -> hits.add(hit));
        var traveler = party.kit(0);
        traveler.grantEnergy(60);
        assertTrue(traveler.intent(Intent.SKILL_PRESS, 0));
        assertTrue(traveler.intent(Intent.SKILL_RELEASE, 0));
        assertFalse(party.switchTo(1, 65)); // traveler-anemo.md tap-to-swap66.
        assertTrue(party.switchTo(1, 66));
        party.advanceTo(126);
        assertEquals(201, traveler.skillRemaining()); // source27+5s, minus126; not paused off-field.
        assertTrue(party.switchTo(0, 126));
        assertTrue(traveler.intent(Intent.BURST_PRESS, 126));
        assertFalse(party.switchTo(1, 225)); // sourced first original burst swap100.
        assertTrue(party.switchTo(1, 226));
        party.advanceTo(300);
        assertEquals(726, traveler.burstRemaining()); // source15s from burst cast126.
        assertTrue(hits.stream().anyMatch(hit -> hit.kind() == CharacterKit.Kind.TORNADO && hit.frame() == 252));
    }
    @Test void particleEnergyUsesEachRecipientsElementAndFieldStatusNotCollectorsFinalGain() {
        var party = party();
        party.collect(Energy.Item.PARTICLE, Element.ANEMO, 2);
        assertEquals(6, party.kit(0).energy()); // elements.md on-field same3 each.
        for (int slot = 1; slot < 4; slot++) assertEquals(1.2, party.kit(slot).energy(), 1e-12); // off-field different.6.
        assertTrue(party.switchTo(3, 0));
        party.collect(Energy.Item.PARTICLE, Element.ANEMO, 2);
        assertEquals(9.6, party.kit(0).energy(), 1e-12); // off-field same1.8 each.
        assertEquals(3.2, party.kit(3).energy(), 1e-12); // on-field different1 each.
        assertEquals(2.4, party.kit(1).energy(), 1e-12);
        assertEquals(2.4, party.kit(2).energy(), 1e-12);
    }
    @Test void fullPartyParticleAndOrbTableIsNotDividedAmongMembers() {
        for (Energy.Item item : Energy.Item.values()) {
            for (Element element : new Element[]{Element.ELECTRO, null}) {
                var party = party();
                party.collect(item, element, 1);
                double orb = item == Energy.Item.ORB ? 3 : 1;
                assertEquals((element == null ? 2 : 1) * orb, party.kit(0).energy(), 1e-12);
                assertEquals((element == null ? 1.2 : .6) * orb, party.kit(1).energy(), 1e-12);
                assertEquals((element == null ? 1.2 : .6) * orb, party.kit(2).energy(), 1e-12);
                assertEquals((element == null ? 1.2 : 1.8) * orb, party.kit(3).energy(), 1e-12);
            }
        }
    }
    @Test void leavingNormalBeforeHitCancelsDamageAndResetsItsComboRatherThanTheWholeMember() {
        var hits = new ArrayList<CharacterKit.Hit>();
        var party = new Party(new EventTimeline(), (kit, hit) -> hits.add(hit));
        party.activeKit().intent(Intent.ATTACK_PRESS, 0);
        assertTrue(party.switchTo(1, 1));
        party.advanceTo(30);
        assertTrue(hits.isEmpty());
        assertEquals(0, party.kit(0).comboIndex());
    }
}

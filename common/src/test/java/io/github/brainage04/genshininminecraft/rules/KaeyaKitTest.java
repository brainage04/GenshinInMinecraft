package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Hit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Kind;
import io.github.brainage04.genshininminecraft.rules.kit.KaeyaKit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** kaeya.md talent1/frame trials; disputed lock, particles and sampling are named adaptations. */
class KaeyaKitTest {
    @Test void frostgnawHitsAtOriginalFrame28WithTwoGaugeAndNoIcd() {
        var hits = new ArrayList<Hit>();
        var kit = new KaeyaKit(hits::add);
        assertTrue(kit.intent(Intent.SKILL_PRESS, 0));
        assertFalse(kit.intent(Intent.SKILL_RELEASE, 0)); // No hold variant.
        kit.advanceTo(27);
        assertTrue(hits.isEmpty());
        kit.advanceTo(28);
        assertEquals(1, hits.size());
        Hit hit = hits.getFirst();
        assertEquals(28, hit.frame());
        assertEquals(Kind.FROSTGNAW, hit.kind());
        assertEquals(1.912, hit.multiplier());
        assertEquals(Element.CRYO, hit.element());
        assertEquals(2, hit.gauge());
        assertNull(hit.icdTag());
        assertTrue(hit.particles() == 2 || hit.particles() == 3); // Empirical possible integer outcomes.
        assertEquals(0, kit.energy()); // Only an adapter's actual enemy hit can collect particles.
    }
    @Test void skillCooldownStartsAt25AndRecoveryAndSwapUseSeparateOriginalTrials() {
        var kit = new KaeyaKit(hit -> {});
        assertTrue(kit.intent(Intent.SKILL_PRESS, 0));
        assertEquals(25 + 6 * 60, kit.skillReadyFrame());
        assertFalse(kit.canSwitch(48));
        assertTrue(kit.canSwitch(49));
        assertFalse(kit.intent(Intent.ATTACK_PRESS, 52));
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 53));
        assertTrue(kit.intent(Intent.ATTACK_RELEASE, 53));
        assertFalse(kit.intent(Intent.SKILL_PRESS, 384));
        assertTrue(kit.intent(Intent.SKILL_PRESS, 385));
        assertEquals(385 + 25 + 360, kit.skillReadyFrame());
    }
    @Test void frostgnawUsesExistingPartyEnergyReceipts() {
        var timeline = new EventTimeline();
        var hits = new ArrayList<Hit>();
        var party = new Party(timeline, (owner, hit) -> hits.add(hit));
        assertTrue(party.switchTo(2, 0));
        assertTrue(party.activeKit().intent(Intent.SKILL_PRESS, 0));
        party.advanceTo(28);
        party.collect(Energy.Item.PARTICLE, Element.CRYO, hits.getFirst().particles());
        double count = hits.getFirst().particles();
        assertEquals(count * 3, party.kit(2).energy());
        for (int slot : new int[]{0, 1, 3}) assertEquals(count * .6, party.kit(slot).energy(), 1e-12);
    }
    @Test void burstRejectsInsufficientEnergyWithoutCooldownOrScheduleThenReservesSixty() {
        var hits = new ArrayList<Hit>();
        var kit = new KaeyaKit(hits::add);
        kit.grantEnergy(59);
        assertFalse(kit.intent(Intent.BURST_PRESS, 0));
        assertEquals(59, kit.energy());
        assertEquals(0, kit.burstReadyFrame());
        kit.advanceTo(51);
        assertTrue(hits.isEmpty());
        kit.grantEnergy(1);
        assertTrue(kit.intent(Intent.BURST_PRESS, 51));
        assertEquals(0, kit.energy());
        assertEquals(51 + 48 + 15 * 60, kit.burstReadyFrame());
        kit.grantEnergy(60);
        assertFalse(kit.intent(Intent.BURST_PRESS, 51 + 48 + 900 - 1));
        assertEquals(60, kit.energy());
        assertTrue(kit.intent(Intent.BURST_PRESS, 51 + 48 + 900));
    }
    @Test void burstSamplesContactsFromOriginal52ForEightSecondsThenFinalShatter() {
        var hits = new ArrayList<Hit>();
        var kit = new KaeyaKit(hits::add);
        kit.grantEnergy(60);
        assertTrue(kit.intent(Intent.BURST_PRESS, 0));
        kit.advanceTo(51);
        assertTrue(hits.isEmpty());
        assertFalse(kit.burstActive(51));
        kit.advanceTo(52);
        assertTrue(kit.burstActive(52));
        assertEquals(.776, hits.getFirst().multiplier());
        assertEquals(1, hits.getFirst().gauge());
        assertEquals("Elemental Burst", hits.getFirst().icdTag());
        kit.advanceTo(532);
        assertFalse(kit.burstActive(532));
        assertEquals(161, hits.size()); // 20TPS sampling is not a claim of 161 damage hits.
        assertEquals(529, hits.get(hits.size() - 2).frame());
        assertEquals(532, hits.getLast().frame());
        assertEquals(Kind.ICICLES_END, hits.getLast().kind());
        kit.advanceTo(600);
        assertEquals(161, hits.size());
    }
    @Test void chosenSeventyFiveFrameDamageLockIsPerIcicleNotAnApplicationIcd() {
        var kit = new KaeyaKit(hit -> {});
        kit.grantEnergy(60);
        kit.intent(Intent.BURST_PRESS, 0);
        assertFalse(kit.connectIcicle(0, 51));
        assertTrue(kit.connectIcicle(0, 52));
        assertTrue(kit.connectIcicle(1, 52));
        assertTrue(kit.connectIcicle(2, 52));
        assertFalse(kit.connectIcicle(0, 52 + 74));
        assertTrue(kit.connectIcicle(0, 52 + 75));
        assertFalse(kit.connectIcicle(0, 532));
    }
    @Test void glacialWaltzPersistsAfterSwitchWithKaeyasOwnerStatsAndCooldown() {
        var hits = new ArrayList<Hit>();
        var owners = new ArrayList<CharacterKit>();
        var party = new Party(new EventTimeline(), (owner, hit) -> { owners.add(owner); hits.add(hit); });
        assertTrue(party.switchTo(2, 0));
        var kaeya = (KaeyaKit) party.activeKit();
        kaeya.grantEnergy(60);
        assertTrue(kaeya.intent(Intent.BURST_PRESS, 0));
        assertFalse(party.switchTo(1, 75));
        assertTrue(party.switchTo(1, 76));
        party.advanceTo(100);
        assertTrue(hits.stream().anyMatch(hit -> hit.frame() > 76));
        assertTrue(owners.stream().allMatch(owner -> owner == kaeya));
        assertEquals(48.04 + 23, owners.getLast().stats().atk(), 1e-12);
        assertEquals(948 - 100, kaeya.burstRemaining());
        party.advanceTo(532);
        assertEquals(Kind.ICICLES_END, hits.getLast().kind());
    }
    @Test void subsequentNormalsAndSkillDoNotCancelAnExistingWaltz() {
        var hits = new ArrayList<Hit>();
        var kit = new KaeyaKit(hits::add);
        kit.grantEnergy(60);
        kit.intent(Intent.BURST_PRESS, 0);
        assertFalse(kit.intent(Intent.ATTACK_PRESS, 76));
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 77));
        kit.intent(Intent.ATTACK_RELEASE, 77);
        assertTrue(kit.intent(Intent.SKILL_PRESS, 104));
        kit.advanceTo(132);
        assertTrue(hits.stream().anyMatch(hit -> hit.kind() == Kind.NORMAL && hit.frame() == 91));
        assertTrue(hits.stream().anyMatch(hit -> hit.kind() == Kind.FROSTGNAW && hit.frame() == 132));
        assertTrue(hits.stream().anyMatch(hit -> hit.kind() == Kind.ICICLES && hit.frame() > 104));
    }
    @Test void fieldExitCancelsUnlandedSkillButNotCooldownAndDeathStopsWaltz() {
        var hits = new ArrayList<Hit>();
        var kit = new KaeyaKit(hits::add);
        kit.intent(Intent.SKILL_PRESS, 0);
        kit.leaveField(10);
        kit.advanceTo(28);
        assertTrue(hits.isEmpty());
        assertEquals(385, kit.skillReadyFrame());
        kit.grantEnergy(60);
        kit.intent(Intent.BURST_PRESS, 28);
        kit.advanceTo(80);
        assertEquals(List.of(Kind.ICICLES), hits.stream().map(Hit::kind).toList());
        kit.setHp(0);
        kit.advanceTo(600);
        assertEquals(1, hits.size());
    }
}

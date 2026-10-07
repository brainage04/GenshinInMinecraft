package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.kit.*;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StarterKitTest {
    @Test void kaeyaTalentOnePhysicalStringUsesOriginalHitmarksAndTclRecovery() {
        var hits = new ArrayList<Hit>();
        var kit = new KaeyaKit(hits::add);
        string(kit, new long[]{0, 27, 54, 101, 147});
        kit.advanceTo(177);
        assertEquals(List.of(14L, 36L, 68L, 124L, 177L), hits.stream().map(Hit::frame).toList());
        assertEquals(List.of(.5375, .5169, .6527, .7086, .8824), hits.stream().map(Hit::multiplier).toList());
        assertTrue(hits.stream().allMatch(hit -> hit.element() == Element.PHYSICAL && hit.gauge() == 0));
        assertEquals(48.04 + 94, kit.stats().atk(), 1e-12); // damage.md: Kaeya20/20 + Harbinger20/20.
    }
    @Test void amberTalentOneFivePhysicalArrowsUseFirstOriginalReleaseTrials() {
        var hits = new ArrayList<Hit>();
        var kit = new AmberKit(hits::add);
        string(kit, new long[]{0, 26, 48, 85, 119});
        kit.advanceTo(145);
        assertEquals(List.of(14L, 36L, 75L, 111L, 145L), hits.stream().map(Hit::frame).toList());
        assertEquals(List.of(.3612, .3612, .4644, .473, .5934), hits.stream().map(Hit::multiplier).toList());
        assertTrue(hits.stream().allMatch(hit -> hit.element() == Element.PHYSICAL && hit.gauge() == 0));
        assertEquals(2037.88, kit.maxHp());
    }
    @Test void lisaTalentOneStringIsFourElectroApplicationsWithSharedStandardIcdTag() {
        var hits = new ArrayList<Hit>();
        var kit = new LisaKit(hits::add);
        string(kit, new long[]{0, 30, 50, 84});
        kit.advanceTo(115);
        assertEquals(List.of(26L, 47L, 67L, 115L), hits.stream().map(Hit::frame).toList());
        assertEquals(List.of(.396, .3592, .428, .5496), hits.stream().map(Hit::multiplier).toList());
        assertTrue(hits.stream().allMatch(hit -> hit.element() == Element.ELECTRO && hit.gauge() == 1
                && "Lisa Electro DMG".equals(hit.icdTag()))); // lisa.md standard normal/tap shared group.
        assertEquals(49.87 + 94, kit.stats().atk(), 1e-12); // damage.md: Lisa20/20 + Thrilling Tales20/20.
    }
    @Test void kaeyaChargeSpendsTwentyOnceAndBothHitsLandAtTheSameSourceFrame() {
        var hits = new ArrayList<Hit>();
        var kit = new KaeyaKit(hits::add);
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 0));
        kit.advanceTo(52);
        assertEquals(List.of(14L, 52L, 52L), hits.stream().map(Hit::frame).toList());
        assertEquals(List.of(.5375, .5504, .731), hits.stream().map(Hit::multiplier).toList());
        assertEquals(80, kit.stamina().current());
        assertFalse(kit.canSwitch(69));
        assertTrue(kit.canSwitch(70)); // kaeya.md charge transition36 + swap34.
    }
    @Test void lisaChargeCostsFiftyAndUsesDocumentedSkippedWindupAdaptation() {
        var hits = new ArrayList<Hit>();
        var kit = new LisaKit(hits::add);
        assertTrue(kit.intent(Intent.ATTACK_PRESS, 0));
        kit.advanceTo(89);
        assertEquals(50, kit.stamina().current()); // lisa.md catalyst charged50.
        assertEquals(List.of(.396, 1.7712), hits.stream().map(Hit::multiplier).toList());
        assertEquals(List.of(26L, 89L), hits.stream().map(Hit::frame).toList());
        assertEquals(1, hits.getLast().gauge());
        assertFalse(kit.canSwitch(106));
        assertTrue(kit.canSwitch(107)); // Recorded 31+76 adaptation; not an exact live-game boundary.
    }
    @Test void insufficientChargeStaminaPreservesThePrecedingFreeNormalAndPool() {
        for (int slot : new int[]{2, 3}) {
            var hits = new ArrayList<Hit>();
            var party = new Party(new EventTimeline(), (kit, hit) -> hits.add(hit));
            assertTrue(party.switchTo(slot, 0));
            assertTrue(party.stamina().consume(slot == 2 ? 81 : 51, 0));
            party.activeKit().intent(Intent.ATTACK_PRESS, 0);
            party.advanceTo(89);
            assertEquals(1, hits.size());
            assertEquals(Kind.NORMAL, hits.getFirst().kind());
            assertEquals(slot == 2 ? 19 : 49, party.stamina().current());
        }
    }
    private static void string(CharacterKit kit, long[] starts) {
        for (long frame : starts) {
            assertTrue(kit.intent(Intent.ATTACK_PRESS, frame));
            assertTrue(kit.intent(Intent.ATTACK_RELEASE, frame));
        }
    }
}

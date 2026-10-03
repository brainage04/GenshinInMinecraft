package io.github.brainage04.genshininminecraft.rules;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IcdEnergyTest {
    private static final UUID OWNER = new UUID(0, 1);
    private static final UUID OTHER_OWNER = new UUID(0, 2);
    private static final UUID TARGET = new UUID(0, 3);
    private static final UUID OTHER_TARGET = new UUID(0, 4);

    @Test
    void standardHitsOneAndFourApplyAndHitCountDoesNotResetTimer() {
        // spec/mechanics/elements.md: ICD finite gauge sequence + reset semantics.
        ApplicationIcd icd = new ApplicationIcd();
        assertTrue(icd.allowsApplication(OWNER, "normal", TARGET, 0));
        assertFalse(icd.allowsApplication(OWNER, "normal", TARGET, 10));
        assertFalse(icd.allowsApplication(OWNER, "normal", TARGET, 20));
        assertTrue(icd.allowsApplication(OWNER, "normal", TARGET, 100));
        assertFalse(icd.allowsApplication(OWNER, "normal", TARGET, 149));
        assertTrue(icd.allowsApplication(OWNER, "normal", TARGET, 150));
        assertFalse(icd.allowsApplication(OWNER, "normal", TARGET, 151));
        assertTrue(icd.allowsApplication(OWNER, "normal", TARGET, 300));
    }

    @Test
    void standardFiniteSequenceEndsAfterTwentyFourHitsUntilTimerReset() {
        // spec/mechanics/elements.md: applying indices 1,4,7,10,13,16,19,22; length 24.
        ApplicationIcd icd = new ApplicationIcd();
        int[] applyingIndices = {1, 4, 7, 10, 13, 16, 19, 22};
        int next = 0;
        for (int hit = 1; hit <= 40; hit++) {
            boolean expected = next < applyingIndices.length && hit == applyingIndices[next];
            assertEquals(expected, icd.allowsApplication(OWNER, "normal", TARGET, hit - 1), "hit " + hit);
            if (expected) next++;
        }
        assertTrue(icd.allowsApplication(OWNER, "normal", TARGET, 150));
    }

    @Test
    void ownerTargetTagAndTypeAreIndependentAndUntaggedHasNoIcd() {
        // spec/mechanics/elements.md: same character + tag + type share ICD per target.
        ApplicationIcd icd = new ApplicationIcd();
        assertTrue(icd.allowsApplication(OWNER, "normal", TARGET, 0));
        assertFalse(icd.allowsApplication(OWNER, "normal", TARGET, 1));
        assertTrue(icd.allowsApplication(OWNER, "burst", TARGET, 1));
        assertTrue(icd.allowsApplication(OTHER_OWNER, "normal", TARGET, 1));
        assertTrue(icd.allowsApplication(OWNER, "normal", OTHER_TARGET, 1));
        assertTrue(icd.allowsApplication(OWNER, "normal", TARGET, ApplicationIcd.Rule.AMBER, 1));
        assertFalse(icd.allowsApplication(OWNER, "normal", TARGET, ApplicationIcd.Rule.AMBER, 60));
        assertTrue(icd.allowsApplication(OWNER, "normal", TARGET, ApplicationIcd.Rule.AMBER, 61));
        assertTrue(icd.allowsApplication(OWNER, null, TARGET, 1));
        assertTrue(icd.allowsApplication(OWNER, null, TARGET, 2));
    }

    @Test
    void reactionDamageLimitsAreSeparateFromApplicationAndSeparateSwirlElements() {
        // spec/mechanics/elements.md: Reaction damage ICD table: Overloaded 1/30 frames,
        // Superconduct and each Swirl element 2/30 frames, per character/target.
        ReactionDamageIcd icd = new ReactionDamageIcd();
        assertTrue(icd.allowsDamage(OWNER, TARGET, Reaction.Type.OVERLOADED, null, 0));
        assertFalse(icd.allowsDamage(OWNER, TARGET, Reaction.Type.OVERLOADED, null, 29));
        assertTrue(icd.allowsDamage(OTHER_OWNER, TARGET, Reaction.Type.OVERLOADED, null, 29));
        assertTrue(icd.allowsDamage(OWNER, TARGET, Reaction.Type.OVERLOADED, null, 30));
        assertTrue(icd.allowsDamage(OWNER, TARGET, Reaction.Type.SUPERCONDUCT, null, 0));
        assertTrue(icd.allowsDamage(OWNER, TARGET, Reaction.Type.SUPERCONDUCT, null, 1));
        assertFalse(icd.allowsDamage(OWNER, TARGET, Reaction.Type.SUPERCONDUCT, null, 2));
        assertTrue(icd.allowsDamage(OWNER, TARGET, Reaction.Type.SUPERCONDUCT, null, 30));
        assertFalse(icd.allowsDamage(OWNER, TARGET, Reaction.Type.SUPERCONDUCT, null, 30));
        assertTrue(icd.allowsDamage(OWNER, TARGET, Reaction.Type.SWIRL, Element.PYRO, 0));
        assertTrue(icd.allowsDamage(OWNER, TARGET, Reaction.Type.SWIRL, Element.PYRO, 0));
        assertFalse(icd.allowsDamage(OWNER, TARGET, Reaction.Type.SWIRL, Element.PYRO, 0));
        assertTrue(icd.allowsDamage(OWNER, TARGET, Reaction.Type.SWIRL, Element.HYDRO, 0));
    }

    @Test
    void forgettingRemovedEntitiesReleasesTheirApplicationWindows() {
        ApplicationIcd icd = new ApplicationIcd();
        assertTrue(icd.allowsApplication(OWNER, "normal", TARGET, 0));
        assertFalse(icd.allowsApplication(OWNER, "normal", TARGET, 1));
        icd.forget(TARGET);
        assertTrue(icd.allowsApplication(OWNER, "normal", TARGET, 2));
    }

    @ParameterizedTest
    @CsvSource({"PARTICLE,SAME,true,4,3", "PARTICLE,DIFFERENT,true,4,1", "PARTICLE,NEUTRAL,true,4,2",
            "ORB,SAME,true,4,9", "ORB,DIFFERENT,true,4,3", "ORB,NEUTRAL,true,4,6",
            "PARTICLE,SAME,false,4,1.8", "PARTICLE,DIFFERENT,false,4,0.6", "PARTICLE,NEUTRAL,false,4,1.2",
            "ORB,SAME,false,4,5.4", "ORB,DIFFERENT,false,4,1.8", "ORB,NEUTRAL,false,4,3.6",
            "PARTICLE,SAME,false,3,2.1", "PARTICLE,DIFFERENT,false,3,0.7", "PARTICLE,NEUTRAL,false,3,1.4",
            "ORB,SAME,false,3,6.3", "ORB,DIFFERENT,false,3,2.1", "ORB,NEUTRAL,false,3,4.2",
            "PARTICLE,SAME,false,2,2.4", "PARTICLE,DIFFERENT,false,2,0.8", "PARTICLE,NEUTRAL,false,2,1.6",
            "ORB,SAME,false,2,7.2", "ORB,DIFFERENT,false,2,2.4", "ORB,NEUTRAL,false,2,4.8"})
    void entireEnergyTable(Energy.Item item, Energy.Affinity affinity, boolean onField, int size, double expected) {
        // spec/mechanics/elements.md: Energy particles and orbs (every numeric table cell).
        assertEquals(expected, Energy.baseEnergy(item, affinity, onField, size), 1e-12);
    }

    @Test
    void recipientElementAndRechargeDetermineEnergyNotCollectorElement() {
        // spec/mechanics/elements.md: Energy_received = item_base_energy * recipient_ER.
        assertEquals(3.6, Energy.received(Energy.Item.PARTICLE, Element.PYRO, Element.PYRO, false, 4, 2), 1e-12);
        assertEquals(1.2, Energy.received(Energy.Item.PARTICLE, Element.PYRO, Element.CRYO, false, 4, 2), 1e-12);
        assertEquals(2.4, Energy.received(Energy.Item.PARTICLE, null, Element.CRYO, false, 4, 2), 1e-12);
    }
}

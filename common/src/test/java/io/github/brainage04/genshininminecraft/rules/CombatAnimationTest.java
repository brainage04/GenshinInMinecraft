package io.github.brainage04.genshininminecraft.rules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.brainage04.genshininminecraft.rules.CombatVisual.Action;
import io.github.brainage04.genshininminecraft.rules.kit.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatAnimationTest {
    // Independent source goldens: spec/mechanics/{traveler-anemo,amber,kaeya,lisa}.md frame tables.
    private static final int[][] HITS = {{13,13,16,30,25},{14,10,27,26,26},{14,9,14,23,30},{26,17,17,31}};
    private static final int[][] RECOVERIES = {{23,32,40,49,81},{26,22,37,34,60},{27,27,47,46,74},{30,20,34,57}};
    private static final String[] NAMES = {"aether", "amber", "kaeya", "lisa"};
    @Test void everyActualKitActionHasSourcedStrikeKeysAndRecoveryLength() throws Exception {
        for (int slot = 0; slot < 4; slot++) {
            var animations = assets(slot).getAsJsonObject("animations");
            var distinctNormals = new HashSet<String>();
            for (var action : Action.values()) {
                var timing = CombatAnimations.timing(slot, action);
                if (timing == null) { assertFalse(animations.has(action.clip()), NAMES[slot] + action); continue; }
                var clip = animations.getAsJsonObject(action.clip());
                assertNotNull(clip, NAMES[slot] + action);
                assertEquals(timing.recovery() / 60.0, clip.get("animation_length").getAsDouble(), 1e-12);
                assertEquals(timing.loop(), clip.get("loop").getAsBoolean());
                if (timing.strike() >= 0) {
                    assertMarker(clip, "strike", timing.strike());
                    assertTrue(hasBoneKey(clip, timing.strike() / 60.0), "Strike is an actual authored pose, not only a label: " + action);
                }
                if (timing.secondStrike() >= 0) {
                    assertMarker(clip, "strike.second", timing.secondStrike());
                    assertTrue(hasBoneKey(clip, timing.secondStrike() / 60.0));
                }
                if (action.ordinal() >= Action.N1.ordinal() && action.ordinal() <= Action.N5.ordinal()) {
                    int n = action.ordinal() - Action.N1.ordinal();
                    assertEquals(HITS[slot][n], timing.strike());
                    assertEquals(RECOVERIES[slot][n], timing.recovery());
                    distinctNormals.add(clip.getAsJsonObject("bones").getAsJsonObject("right_arm").toString());
                }
                for (var bone : clip.getAsJsonObject("bones").entrySet()) {
                    if (action != Action.FALLEN) assertFalse(bone.getKey().contains("leg") || bone.getKey().contains("shin") || bone.getKey().equals("root"), "Combat must leave locomotion legs running");
                    for (var channel : bone.getValue().getAsJsonObject().entrySet()) {
                        if (!channel.getValue().isJsonObject()) continue;
                        double previous = -1;
                        for (var key : channel.getValue().getAsJsonObject().entrySet()) {
                            double time = Double.parseDouble(key.getKey());
                            assertTrue(time > previous && time <= timing.recovery() / 60.0);
                            previous = time;
                        }
                    }
                }
            }
            assertEquals(HITS[slot].length, distinctNormals.size(), "Distinct normal choreography");
        }
        assertTiming(0, Action.CHARGED, 10, 21, 55); assertTiming(2, Action.CHARGED, 16, 16, 54);
        assertTiming(3, Action.CHARGED, 58, -1, 77);
        assertTiming(0, Action.SKILL_TAP, 32, -1, 74); assertTiming(0, Action.SKILL_RELEASE, 5, -1, 48);
        assertTiming(2, Action.SKILL_TAP, 28, -1, 53); assertTiming(3, Action.SKILL_TAP, 17, -1, 38);
        assertTiming(3, Action.SKILL_RELEASE, 3, -1, 27);
        assertTiming(0, Action.BURST, 96, -1, 111); assertTiming(1, Action.BURST, 72, -1, 111);
        assertTiming(2, Action.BURST, 52, -1, 77); assertTiming(3, Action.BURST, 56, -1, 85);
        assertTiming(1, Action.AIM_RELEASE, 0, -1, 10);
    }
    @Test void independentFieldImpactsRetainTheirKitAnchorsWithoutStretchingPlayerRecovery() throws Exception {
        var amber = assets(1).getAsJsonObject("genshin_field_events");
        assertEquals(45 / 60.0, amber.get("ADAPTED_PUPPET_LANDING_FRAME").getAsDouble(), 1e-12);
        assertEquals(AmberKit.ADAPTED_PUPPET_LANDING_FRAME / 60.0, amber.get("ADAPTED_PUPPET_LANDING_FRAME").getAsDouble(), 1e-12);
        assertEquals(32 / 60.0, assets(1).getAsJsonObject("animations").getAsJsonObject(Action.SKILL_TAP.clip()).get("animation_length").getAsDouble(), 1e-12);
        var lisa = assets(3).getAsJsonObject("genshin_field_events");
        assertEquals(22 / 60.0, lisa.get("TAP_FIRST_IMPACT_FRAME").getAsDouble(), 1e-12);
        assertEquals(59 / 60.0, lisa.get("ADAPTED_ROSE_FORMATION_FRAME").getAsDouble(), 1e-12);
        assertEquals(119 / 60.0, lisa.get("ROSE_FIRST_DISCHARGE_FRAME").getAsDouble(), 1e-12);
        var hits = new java.util.ArrayList<CharacterKit.Hit>();
        var kit = new AmberKit(hits::add);
        assertTrue(kit.intent(CharacterKit.Intent.SKILL_PRESS, 0));
        kit.advanceTo(32);
        assertEquals(Action.NONE, kit.visual().action(32));
        assertTrue(hits.isEmpty(), "Bunny cannot land during its shorter player cast");
        kit.advanceTo(45);
        assertEquals(CharacterKit.Kind.BUNNY_LAND, hits.getFirst().kind());
        assertEquals(45, hits.getFirst().frame());
    }
    @Test void conditionalLateTapSeeksTheStrikeImmediatelyAndKeepsActualRecovery() {
        assertEquals(17 / 60.0, CombatAnimations.seconds(3, Action.SKILL_TAP, 0, 0, 8), 1e-12);
        assertEquals(27.5 / 60.0, CombatAnimations.seconds(3, Action.SKILL_TAP, 4, 0, 8), 1e-12);
        assertEquals(17 / 60.0, CombatAnimations.seconds(3, Action.SKILL_TAP, 17, 17, 38), 1e-12);
        assertEquals(5 / 60.0, CombatAnimations.seconds(0, Action.SKILL_RELEASE, 5, 5, 48), 1e-12);
        assertEquals(1 / 60.0, CombatAnimations.seconds(0, Action.SKILL_HOLD, 121, -1, -1), 1e-12);
    }
    @Test void acceptedNormalsChargesAndRejectedInputsPublishTheActualActionNotTheNextCombo() {
        CharacterKit[] kits = {new TravelerAnemoKit(hit -> {}), new AmberKit(hit -> {}), new KaeyaKit(hit -> {}), new LisaKit(hit -> {})};
        for (int slot = 0; slot < 4; slot++) {
            var kit = kits[slot]; long frame = 0;
            for (int n = 0; n < HITS[slot].length; n++) {
                assertTrue(kit.intent(CharacterKit.Intent.ATTACK_PRESS, frame));
                assertEquals(Action.normal(n), kit.visual().action(frame));
                assertEquals(frame, kit.visual().startFrame());
                assertEquals(HITS[slot][n], kit.visual().strikeFrame());
                assertEquals(RECOVERIES[slot][n], kit.visual().recoveryFrames());
                int occurrence = kit.visual().occurrence();
                assertFalse(kit.intent(CharacterKit.Intent.ATTACK_PRESS, frame));
                assertEquals(occurrence, kit.visual().occurrence());
                kit.intent(CharacterKit.Intent.ATTACK_RELEASE, frame);
                frame += RECOVERIES[slot][n]; kit.advanceTo(frame);
                assertEquals(Action.NONE, kit.visual().action(frame));
            }
            kit.leaveField(frame);
            assertEquals(Action.NONE, kit.visual().action(frame));
            assertTrue(kit.intent(CharacterKit.Intent.ATTACK_PRESS, frame));
            int normal = kit.visual().occurrence();
            kit.advanceTo(frame + (slot == 0 ? 28 : slot == 1 ? 9 : slot == 2 ? 36 : 31));
            assertEquals(slot == 1 ? Action.AIM_HOLD : Action.CHARGED, kit.visual().action(kit.frame()));
            assertTrue(kit.visual().occurrence() > normal);
        }
    }
    @Test void heldSkillsTapReleaseAutoReleaseAndBurstUseCommittedPhaseClocks() {
        var traveler = new TravelerAnemoKit(hit -> {});
        assertTrue(traveler.intent(CharacterKit.Intent.SKILL_PRESS, 0));
        assertEquals(Action.SKILL_START, traveler.visual().action(0));
        traveler.advanceTo(21); assertEquals(Action.SKILL_HOLD, traveler.visual().action(21));
        traveler.intent(CharacterKit.Intent.SKILL_RELEASE, 30);
        assertEquals(Action.SKILL_RELEASE, traveler.visual().action(30));
        assertEquals(30, traveler.visual().startFrame()); assertEquals(5, traveler.visual().strikeFrame());
        var lisa = new LisaKit(hit -> {});
        lisa.intent(CharacterKit.Intent.SKILL_PRESS, 0); lisa.advanceTo(17);
        assertEquals(Action.SKILL_HOLD, lisa.visual().action(17));
        lisa.intent(CharacterKit.Intent.SKILL_RELEASE, 30);
        assertEquals(Action.SKILL_TAP, lisa.visual().action(30));
        assertEquals(0, lisa.visual().strikeFrame()); assertEquals(8, lisa.visual().recoveryFrames());
        var hold = new LisaKit(hit -> {});
        hold.intent(CharacterKit.Intent.SKILL_PRESS, 0); hold.advanceTo(240);
        assertEquals(Action.SKILL_RELEASE, hold.visual().action(240)); assertEquals(240, hold.visual().startFrame());
        var auto = new TravelerAnemoKit(hit -> {});
        auto.intent(CharacterKit.Intent.SKILL_PRESS, 0); auto.advanceTo(108);
        assertEquals(Action.SKILL_RELEASE, auto.visual().action(108));
        for (CharacterKit kit : new CharacterKit[]{new TravelerAnemoKit(hit -> {}), new AmberKit(hit -> {}), new KaeyaKit(hit -> {}), new LisaKit(hit -> {})}) {
            assertFalse(kit.intent(CharacterKit.Intent.BURST_PRESS, 0)); assertEquals(Action.NONE, kit.visual().action(0));
            kit.grantEnergy(80); assertTrue(kit.intent(CharacterKit.Intent.BURST_PRESS, 0));
            assertEquals(Action.BURST, kit.visual().action(0));
            kit.visual().hurt(3); assertEquals(Action.BURST, kit.visual().action(3));
            kit.leaveField(4); assertEquals(Action.NONE, kit.visual().action(4));
        }
    }
    private static void assertTiming(int slot, Action action, int hit, int second, int recovery) {
        assertEquals(new CombatAnimations.Timing(hit, second, recovery, false), CombatAnimations.timing(slot, action));
    }
    private static void assertMarker(JsonObject clip, String marker, int frame) {
        double time = clip.getAsJsonObject("genshin_markers").entrySet().stream()
                .filter(entry -> java.util.List.of(entry.getValue().getAsString().split(" ")).contains(marker))
                .mapToDouble(entry -> Double.parseDouble(entry.getKey())).findFirst().orElseThrow();
        assertEquals(frame / 60.0, time, 1e-12);
    }
    private static boolean hasBoneKey(JsonObject clip, double time) {
        for (var bone : clip.getAsJsonObject("bones").entrySet()) for (var channel : bone.getValue().getAsJsonObject().entrySet())
            if (channel.getValue().isJsonObject()) for (String key : channel.getValue().getAsJsonObject().keySet())
                if (Math.abs(Double.parseDouble(key) - time) < 1e-12) return true;
        return false;
    }
    private static JsonObject assets(int slot) throws Exception {
        try (var stream = CombatAnimationTest.class.getResourceAsStream("/assets/genshininminecraft/geckolib/animations/character/" + NAMES[slot] + ".animation.json")) {
            assertNotNull(stream); return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}

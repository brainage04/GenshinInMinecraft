package io.github.brainage04.genshininminecraft.rules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnemyAnimationTest {
    @Test void telegraphAndImpactRetainExistingTenTickWindupAndThirtyTickRecovery() throws Exception {
        // Independent accepted AI adaptation: docs/character-rendering.md section3, fidelity.md.
        assertEquals(10, EnemyAnimations.WINDUP_TICKS);
        assertEquals(30, EnemyAnimations.RECOVERY_TICKS);
        var clips = assets("hilichurl");
        var telegraph = clips.getAsJsonObject("enemy.telegraph");
        assertEquals(.5, telegraph.get("animation_length").getAsDouble(), 1e-12);
        assertEquals(EnemyAnimations.WINDUP_TICKS * 3 / 60.0, telegraph.get("animation_length").getAsDouble(), 1e-12);
        assertTrue(telegraph.getAsJsonObject("bones").getAsJsonObject("right_arm").getAsJsonObject("rotation").has("0.5"));
        var strike = clips.getAsJsonObject("enemy.strike");
        assertEquals("strike", strike.getAsJsonObject("genshin_markers").get("0.0").getAsString());
        assertTrue(strike.getAsJsonObject("bones").getAsJsonObject("club").getAsJsonObject("rotation").has("0.0"), "Actual club pose at the strike decision, never a delayed hit");
        assertEquals(30, strike.get("genshin_attack_offset_frames").getAsInt(), "Impact frame relative to telegraph start");
        assertEquals(EnemyAnimations.WINDUP_TICKS * 3, strike.get("genshin_attack_offset_frames").getAsInt());
        assertEquals(1.5, strike.get("animation_length").getAsDouble()
                + clips.getAsJsonObject("enemy.recovery").get("animation_length").getAsDouble(), 1e-12);
        assertEquals(1, clips.getAsJsonObject("enemy.death").get("animation_length").getAsDouble(), 1e-12);
    }
    @Test void bunnyFlightLandingAndInertExplosionAreCompleteDistinctMotions() throws Exception {
        var clips = assets("baron_bunny");
        for (String clip : new String[]{"idle", "walk", "thrown", "land", "hurt", "explode"})
            assertNotNull(clips.getAsJsonObject("enemy." + clip), clip);
        assertEquals(40 / 60.0, clips.getAsJsonObject("enemy.thrown").get("animation_length").getAsDouble(), 1e-12);
        assertEquals(.25, clips.getAsJsonObject("enemy.land").get("animation_length").getAsDouble(), 1e-12);
        assertEquals(.4, clips.getAsJsonObject("enemy.explode").get("animation_length").getAsDouble(), 1e-12);
        assertFalse(clips.has("enemy.strike"), "Passive puppet does not invent an attack");
        assertTrue(clips.getAsJsonObject("enemy.explode").getAsJsonObject("bones").getAsJsonObject("root").getAsJsonObject("scale").has("0.4"));
    }
    @Test void absoluteSeekingLoopsOrHoldsWithoutRestartingAndHurtIsOnlyAdditiveJoints() throws Exception {
        assertEquals(.2, EnemyAnimations.Phase.TELEGRAPH.seconds(.2), 1e-12);
        assertEquals(.5 - .000001, EnemyAnimations.Phase.TELEGRAPH.seconds(3), 1e-12);
        assertEquals(.25, EnemyAnimations.Phase.IDLE.seconds(4.25), 1e-12);
        for (String name : new String[]{"hilichurl", "baron_bunny"}) {
            var hurt = assets(name).getAsJsonObject("enemy.hurt").getAsJsonObject("bones");
            assertEquals(2, hurt.size()); assertTrue(hurt.has("torso")); assertTrue(hurt.has("head"));
        }
    }
    private static JsonObject assets(String name) throws Exception {
        String path = "/assets/genshininminecraft/geckolib/animations/enemy/" + name + ".animation.json";
        try (var stream = EnemyAnimationTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
        }
    }
}

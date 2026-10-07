package io.github.brainage04.genshininminecraft.rules;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CharacterAssetsTest {
    @Test void originalModelsContainArticulatedSkeletonValidUvsAndAllTimedClips() throws Exception {
        for (String name : new String[]{"aether", "amber", "kaeya", "lisa"}) {
            String root = "/assets/genshininminecraft/";
            var geometry = json(root + "geckolib/models/character/" + name + ".geo.json")
                    .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var bones = geometry.getAsJsonArray("bones");
            var names = new HashSet<String>();
            int cubes = 0;
            for (var entry : bones) {
                var bone = entry.getAsJsonObject();
                assertTrue(names.add(bone.get("name").getAsString()), "Duplicate bone");
                if (bone.has("parent")) assertTrue(names.contains(bone.get("parent").getAsString()), "Missing/cyclic parent");
                if (!bone.has("cubes")) continue;
                cubes += bone.getAsJsonArray("cubes").size();
                for (var cubeEntry : bone.getAsJsonArray("cubes")) {
                    var cube = cubeEntry.getAsJsonObject();
                    assertEquals(6, cube.getAsJsonObject("uv").size());
                    for (var uvEntry : cube.getAsJsonObject("uv").entrySet()) {
                        var uv = uvEntry.getValue().getAsJsonObject();
                        for (int i = 0; i < 2; i++) {
                            double origin = uv.getAsJsonArray("uv").get(i).getAsDouble();
                            double size = uv.getAsJsonArray("uv_size").get(i).getAsDouble();
                            assertTrue(origin >= 0 && size > 0 && origin + size <= 128);
                        }
                    }
                }
            }
            assertTrue(cubes >= 65, "Actual detailed geometry, not one tinted vanilla body");
            for (String required : new String[]{"root", "hips", "torso", "head", "hair", "accessory", "right_arm", "right_forearm", "right_hand",
                    "left_arm", "left_forearm", "left_hand", "right_leg", "right_shin", "right_foot", "left_leg", "left_shin", "left_foot",
                    "cape", "weapon", "glider_left", "glider_right"}) assertTrue(names.contains(required), name + ": " + required);
            var animations = json(root + "geckolib/animations/character/" + name + ".animation.json").getAsJsonObject("animations");
            for (var phase : Locomotion.Phase.values()) {
                var clip = animations.getAsJsonObject(phase.clip());
                assertNotNull(clip, name + ": " + phase.clip());
                assertEquals(phase.frames() / 60.0, clip.get("animation_length").getAsDouble(), 1e-12);
                assertEquals(phase.loop(), clip.get("loop").getAsBoolean());
                assertEquals(names, clip.getAsJsonObject("bones").keySet(), "No stale pose inheritance");
                for (var bone : clip.getAsJsonObject("bones").entrySet()) {
                    for (var channel : bone.getValue().getAsJsonObject().entrySet()) {
                        if (!channel.getValue().isJsonObject()) continue;
                        double previous = -1;
                        for (var key : channel.getValue().getAsJsonObject().entrySet()) {
                            double time = Double.parseDouble(key.getKey());
                            assertTrue(time > previous && time <= phase.frames() / 60.0);
                            previous = time;
                        }
                    }
                }
            }
            try (var texture = getClass().getResourceAsStream(root + "textures/entity/character/" + name + ".png")) {
                assertNotNull(texture);
                var header = java.nio.ByteBuffer.wrap(texture.readNBytes(24));
                assertEquals(0x89504e470d0a1a0aL, header.getLong());
                assertEquals(128, header.getInt(16));
                assertEquals(128, header.getInt(20));
            }
        }
    }
    private com.google.gson.JsonObject json(String path) throws Exception {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}

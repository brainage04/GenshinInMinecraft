package io.github.brainage04.genshininminecraft;

import com.geckolib.constant.DataTickets;
import com.geckolib.loading.definition.animation.ActorAnimations;
import com.geckolib.loading.definition.geometry.Geometry;
import com.geckolib.renderer.GeoObjectRenderer;
import com.mojang.authlib.GameProfile;
import io.github.brainage04.genshininminecraft.client.CombatFeedback;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import io.github.brainage04.genshininminecraft.client.character.*;
import io.github.brainage04.genshininminecraft.network.PlayerCharacterPayload;
import io.github.brainage04.genshininminecraft.rules.Locomotion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
final class CharacterRenderingChecks {
    private CharacterRenderingChecks() {}
    static void characters(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.waitFor(client -> PlayerVisuals.usesCharacter(client.player));
        int originalFov = context.computeOnClient(client -> client.options.fov().get());
        context.runOnClient(CharacterRenderingChecks::assetsAndSeeking);
        String[] names = {"aether", "amber", "kaeya", "lisa"};
        for (int slot = 0; slot < names.length; slot++) {
            final int selected = slot;
            context.waitFor(client -> !CombatInput.state().switchBlocked() && CombatInput.state().switchRemainingFrames() == 0);
            context.getInput().pressKey(GLFW.GLFW_KEY_1 + slot);
            context.waitFor(client -> CombatInput.state().activeSlot() == selected
                    && PlayerVisuals.snapshot(client.player.getId()).slot() == selected);
            float heading = selected * 40;
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~ " + heading + " 0");
            // Wait for the actual teleport rotation; client ticks may outrun a busy dedicated server.
            context.waitFor(client -> Math.abs(io.github.brainage04.genshininminecraft.rules.CameraMath
                    .wrap(client.player.getYRot() - heading)) < .5);
            context.waitTicks(20); // Let switch dust retire and interpolated torso facing settle.
            context.runOnClient(client -> {
                var avatar = (net.minecraft.client.renderer.entity.state.AvatarRenderState)
                        client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 0);
                ManagedCamera.setAngles(avatar.bodyRot + 150, 10);
                client.options.fov().set(55);
                client.gui.hud.getChat().clearMessages(true);
                assertPath(client, true);
            });
            long body = context.computeOnClient(client -> PlayerVisuals.bodySubmissions());
            context.waitFor(client -> PlayerVisuals.bodySubmissions() > body && PlayerVisuals.lastBodySlot() == selected);
            context.getInput().pressKey(options -> options.keyToggleGui);
            context.waitFor(client -> client.gui.hud.isHidden());
            context.waitTicks(2);
            context.runOnClient(client -> {
                var avatar = (net.minecraft.client.renderer.entity.state.AvatarRenderState)
                        client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 0);
                float angle = io.github.brainage04.genshininminecraft.rules.CameraMath.wrap(
                        client.gameRenderer.mainCamera().yRot() - avatar.bodyRot);
                if (Math.abs(angle - 150) > 10)
                    throw new AssertionError("Standing capture must be front3/4; actual camera/body angle=" + angle);
            });
            screenshot(context, "genshin-character-" + names[slot]);
            context.getInput().pressKey(options -> options.keyToggleGui);
            context.waitFor(client -> !client.gui.hud.isHidden());
            context.runOnClient(client -> client.options.fov().set(originalFov));
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.waitFor(client -> client.options.getCameraType() == CameraType.FIRST_PERSON);
            long arms = context.computeOnClient(client -> PlayerVisuals.armSubmissions());
            context.waitFor(client -> PlayerVisuals.armSubmissions() > arms && PlayerVisuals.lastArmSlot() == selected);
            screenshot(context, "genshin-character-" + names[slot] + "-arms");
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:stone");
            context.waitFor(client -> !client.player.getMainHandItem().isEmpty());
            long occupied = context.computeOnClient(client -> PlayerVisuals.armSubmissions());
            context.waitFor(client -> PlayerVisuals.armSubmissions() > occupied && PlayerVisuals.lastArmSlot() == selected);
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.waitFor(client -> ManagedCamera.decoupled());
        }
        context.runOnClient(CharacterRenderingChecks::remoteSnapshotFixture);
        var reload = context.computeOnClient(Minecraft::reloadResourcePacks);
        context.waitFor(client -> reload.isDone());
        reload.join();
        context.waitTicks(3);
        context.runOnClient(client -> { assetsAndSeeking(client); assertPath(client, true); });
        context.waitFor(client -> CombatInput.state().switchRemainingFrames() == 0);
        context.getInput().pressKey(GLFW.GLFW_KEY_1);
        context.waitFor(client -> CombatInput.state().activeSlot() == 0 && PlayerVisuals.snapshot(client.player.getId()).slot() == 0);
        context.runOnClient(client -> ManagedCamera.setAngles(0, 0));
        context.waitTicks(20);
    }
    static void dash(ClientGameTestContext context, TestDedicatedServerContext server) {
        // Slow real server ticks only for capture; no injected visual phase or invented movement.
        int fov = context.computeOnClient(client -> client.options.fov().get());
        server.runCommand("tick rate 5");
        try {
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(3);
            context.getInput().holdKey(options -> options.keySprint);
            context.waitFor(client -> PlayerVisuals.snapshot(client.player.getId()).phase() == Locomotion.Phase.DASH);
            // Preserve a real early dash pose during capture, not its held final frame or the following run.
            server.runCommand("tick freeze");
            context.waitFor(client -> client.level.tickRateManager().isFrozen());
            context.runOnClient(client -> {
                ManagedCamera.setAngles(ManagedCamera.yaw() + 35, 12);
                client.options.fov().set(55);
                var state = (CharacterAvatarState) client.getEntityRenderDispatcher().getRenderer(client.player)
                        .createRenderState(client.player, 0);
                var input = state.genshin$characterState().input();
                if (input.phase() != Locomotion.Phase.DASH || input.elapsedSeconds() > .25)
                    throw new AssertionError("Dash capture must seek the active18-frame clip, not its final pose: " + input);
            });
            screenshot(context, "genshin-character-dash");
        } finally {
            server.runCommand("tick unfreeze");
            context.runOnClient(client -> { client.options.fov().set(fov); ManagedCamera.setAngles(0, 0); });
            context.getInput().releaseKey(options -> options.keySprint);
            context.getInput().releaseKey(options -> options.keyUp);
            server.runCommand("tick rate 20");
        }
        context.waitFor(client -> CombatInput.state().stamina() == 100 && !client.player.isSprinting());
    }
    static void traversal(ClientGameTestContext context, String name, boolean glide) {
        context.waitFor(client -> {
            var snapshot = PlayerVisuals.snapshot(client.player.getId());
            return snapshot != null && (glide ? snapshot.phase() == Locomotion.Phase.GLIDE_LOOP
                    : snapshot.phase() == Locomotion.Phase.CLIMB_UP || snapshot.phase() == Locomotion.Phase.CLIMB_IDLE);
        });
        float yaw = context.computeOnClient(client -> ManagedCamera.yaw());
        float pitch = context.computeOnClient(client -> ManagedCamera.pitch());
        context.runOnClient(client -> { assertPath(client, true); ManagedCamera.setAngles(yaw + 25, glide ? 30 : 12); });
        context.getInput().pressKey(options -> options.keyToggleGui);
        context.waitFor(client -> client.gui.hud.isHidden());
        context.waitTicks(2);
        screenshot(context, "genshin-character-" + name);
        context.getInput().pressKey(options -> options.keyToggleGui);
        context.waitFor(client -> !client.gui.hud.isHidden());
        context.runOnClient(client -> ManagedCamera.setAngles(yaw, pitch));
    }
    static void unmanaged(ClientGameTestContext context) {
        context.runOnClient(client -> {
            assertPath(client, false);
            if (!PlayerVisuals.snapshots().isEmpty()) throw new AssertionError("Unmanage must clear public snapshots and character caches");
        });
    }
    private static void assertPath(Minecraft client, boolean managed) {
        var renderer = client.getEntityRenderDispatcher().getRenderer(client.player);
        var state = renderer.createRenderState(client.player, 0);
        var holder = (CharacterAvatarState) state;
        if ((holder.genshin$characterState() != null) != managed)
            throw new AssertionError("Actual Avatar extraction must select " + (managed ? "GeckoLib" : "vanilla"));
        if (managed && !(PlayerVisuals.renderer() instanceof GeoObjectRenderer))
            throw new AssertionError("Managed body must delegate to GeoObjectRenderer");
    }
    private static void assetsAndSeeking(Minecraft client) {
        var model = PlayerVisuals.renderer().getGeoModel();
        for (int slot = 0; slot < 4; slot++) {
            var character = new CharacterAnimatable(slot);
            var input = input(slot, Locomotion.Phase.DASH, 2, .2);
            var state = PlayerVisuals.renderer().fillRenderState(character, input,
                    PlayerVisuals.renderer().createRenderState(character, input), .5F);
            var id = model.getModelResource(state);
            var baked = model.getBakedModel(id);
            if (baked.isMissingno()) throw new AssertionError("Missing actual GeckoLib character model: " + id);
            for (String bone : new String[]{"head", "hair", "right_hand", "left_hand", "weapon", "cape", "glider_left", "glider_right"})
                if (baked.getBone(bone).isEmpty()) throw new AssertionError("Missing baked bone: " + id + ":" + bone);
            for (var phase : Locomotion.Phase.values())
                if (model.getBakedAnimation(character, phase.clip()) == null) throw new AssertionError("Missing baked clip: " + id + ":" + phase.clip());
            // Parse and bake again through the pinned library, not just our JSON-shape test.
            try (var geo = client.getResourceManager().openAsReader(Identifier.fromNamespaceAndPath("genshininminecraft", "geckolib/models/" + id.getPath() + ".geo.json"));
                 var anim = client.getResourceManager().openAsReader(Identifier.fromNamespaceAndPath("genshininminecraft", "geckolib/animations/" + id.getPath() + ".animation.json"))) {
                if (Geometry.GSON.fromJson(geo, Geometry.class).bake(id).getBone("weapon").isEmpty()) throw new AssertionError("Geometry parser discarded weapon");
                var clips = ActorAnimations.GSON.fromJson(anim, ActorAnimations.class).animations();
                if (clips.size() != Locomotion.Phase.values().length) throw new AssertionError("Incomplete parsed locomotion set");
            } catch (java.io.IOException ex) { throw new AssertionError("Cannot parse generated GeckoLib assets", ex); }
            var controller = state.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES)[0];
            if (Math.abs(controller.animationPoint().animTime() - .2) > .000001)
                throw new AssertionError("First late extraction must seek to .2 seconds, not restart at zero");
            var repeated = PlayerVisuals.renderer().fillRenderState(character, input,
                    PlayerVisuals.renderer().createRenderState(character, input), .5F);
            if (Math.abs(repeated.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES)[0].animationPoint().animTime() - .2) > .000001)
                throw new AssertionError("Repeated extraction must not advance a phase twice");
            var next = input(slot, Locomotion.Phase.DASH, 3, .05);
            PlayerVisuals.renderer().fillRenderState(character, next, PlayerVisuals.renderer().createRenderState(character, next), .5F);
            if (Math.abs(controller.animationPoint().animTime() - .2) > .000001)
                throw new AssertionError("New occurrence cannot mutate already-extracted controller state");
        }
    }
    private static CharacterRenderInput input(int slot, Locomotion.Phase phase, int occurrence, double seconds) {
        return new CharacterRenderInput(9999L + slot, slot, phase, occurrence, seconds, 100,
                0, 0, 0, 1, 0xf000f0, 0, false, false, 0, 0, false);
    }
    private static void remoteSnapshotFixture(Minecraft client) {
        // A real RemotePlayer/render-state path with a synthetic public packet; not a two-client network claim.
        var remote = new RemotePlayer(client.level, new GameProfile(new UUID(20, 26), "CharacterFixture"));
        remote.setId(2000000);
        remote.snapTo(client.player.position().add(2, 0, 0));
        client.level.addEntity(remote);
        try {
            for (int slot = 0; slot < 4; slot++) {
                CombatFeedback.accept(new PlayerCharacterPayload(remote.getId(), remote.getUUID(), slot, Locomotion.Phase.IDLE,
                        slot + 1, 0, 0, 120, client.level.getGameTime()));
                var state = client.getEntityRenderDispatcher().getRenderer(remote).createRenderState(remote, 0);
                var geo = ((CharacterAvatarState) state).genshin$characterState();
                if (geo == null || geo.input().slot() != slot) throw new AssertionError("Remote accepted switch must use its own character cache");
            }
            CombatFeedback.accept(new PlayerCharacterPayload(remote.getId(), new UUID(99, 99), 0, Locomotion.Phase.IDLE,
                    1, 0, 0, 120, client.level.getGameTime()));
            if (PlayerVisuals.usesCharacter(remote)) throw new AssertionError("Reused entity ID must never inherit another UUID's visual");
        } finally { client.level.removeEntity(remote.getId(), Entity.RemovalReason.DISCARDED); }
    }
    private static void screenshot(ClientGameTestContext context, String name) {
        Path directory = context.computeOnClient(client -> client.gameDirectory.toPath().resolve("screenshots"));
        Path path = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(directory));
        if (!Files.isRegularFile(path)) throw new AssertionError("Character screenshot not written: " + path);
        GenshinInMinecraft.LOGGER.info("Character GameTest screenshot: {}", path.toAbsolutePath());
    }
}

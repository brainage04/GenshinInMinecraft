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
import io.github.brainage04.genshininminecraft.rules.CombatVisual;
import io.github.brainage04.genshininminecraft.rules.CombatAnimations;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
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
        combat(context, server);
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
    private static void combat(ClientGameTestContext context, TestDedicatedServerContext server) {
        int fov = context.computeOnClient(client -> client.options.fov().get());
        String[] names = {"aether", "amber", "kaeya", "lisa"};
        try {
            for (int slot = 0; slot < 4; slot++) {
                final int selected = slot;
                server.runCommand("genshin managed off");
                context.waitFor(client -> !CombatInput.managed());
                server.runCommand("genshin managed on");
                context.waitFor(client -> PlayerVisuals.usesCharacter(client.player));
                server.runOnServer(minecraftServer -> {
                    var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    var session = CombatRuntime.get(minecraftServer).session(player);
                    if (selected != 0 && !session.intent(CharacterKit.Intent.values()[CharacterKit.Intent.SWITCH_1.ordinal() + selected],
                            Frames.atServerTick(minecraftServer.getTickCount()))) throw new AssertionError("Capture switch rejected");
                });
                context.waitFor(client -> PlayerVisuals.snapshot(client.player.getId()).slot() == selected);
                context.waitTicks(20);
                context.runOnClient(client -> {
                    var avatar = (net.minecraft.client.renderer.entity.state.AvatarRenderState)
                            client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 0);
                    ManagedCamera.setAngles(avatar.bodyRot + 150, 10);
                    client.options.fov().set(55);
                    client.gui.hud.getChat().clearMessages(true);
                });
                context.getInput().pressKey(options -> options.keyToggleGui);
                context.waitFor(client -> client.gui.hud.isHidden());
                actionCapture(context, server, selected, CharacterKit.Intent.ATTACK_PRESS, CombatVisual.Action.N1,
                        "genshin-combat-" + names[slot] + "-n1", fov);
                actionCapture(context, server, selected, CharacterKit.Intent.SKILL_PRESS, CombatVisual.Action.SKILL_TAP,
                        "genshin-combat-" + names[slot] + "-skill", fov);
                actionCapture(context, server, selected, CharacterKit.Intent.BURST_PRESS, CombatVisual.Action.BURST,
                        "genshin-combat-" + names[slot] + "-burst", fov);
                if (slot == 0 || slot == 3) channelCaptures(context, server, slot, names[slot], fov);
                context.getInput().pressKey(options -> options.keyToggleGui);
                context.waitFor(client -> !client.gui.hud.isHidden());
                server.runCommand("tick unfreeze");
            }
        } finally {
            server.runCommand("tick unfreeze");
            server.runCommand("tick rate 20");
            server.runCommand("genshin managed off");
            context.waitFor(client -> !CombatInput.managed());
            server.runCommand("genshin managed on");
            context.waitFor(client -> PlayerVisuals.usesCharacter(client.player) && CombatInput.state().activeSlot() == 0);
            context.runOnClient(client -> { client.options.fov().set(fov); ManagedCamera.setAngles(0, 0); });
        }
    }
    private static void actionCapture(ClientGameTestContext context, TestDedicatedServerContext server, int slot,
            CharacterKit.Intent intent, CombatVisual.Action action, String name, int defaultFov) {
        var timing = CombatAnimations.timing(slot, action);
        // Place the accepted action inside the current 60-fps tick interval so its sourced hitmark
        // falls exactly on a server tick. No visual packet/pose is invented; the real kit drains it.
        int offset = (3 - timing.strike() % 3) % 3;
        long start = server.computeOnServer(minecraftServer -> {
            var player = minecraftServer.getPlayerList().getPlayers().getFirst();
            var session = CombatRuntime.get(minecraftServer).session(player);
            long frame = Frames.atServerTick(minecraftServer.getTickCount()) + offset;
            if (intent == CharacterKit.Intent.BURST_PRESS) session.kit().grantEnergy(80);
            if (!session.intent(intent, frame)) throw new AssertionError("Real kit rejected capture action " + action);
            if (intent == CharacterKit.Intent.ATTACK_PRESS) session.intent(CharacterKit.Intent.ATTACK_RELEASE, frame);
            if (intent == CharacterKit.Intent.SKILL_PRESS && (slot == 0 || slot == 3))
                session.intent(CharacterKit.Intent.SKILL_RELEASE, frame);
            timeline(minecraftServer).schedule(frame + timing.strike(), at -> {
                var snapshot = session.visualSnapshot(slot);
                if (snapshot.sampleFrame() != at || snapshot.action() != action)
                    throw new AssertionError("Capture must publish the actual kit at the exact hitmark: " + snapshot);
                minecraftServer.tickRateManager().setTickRate(1);
                minecraftServer.tickRateManager().setFrozen(true);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTimePacket(snapshot.sampleGameTime(), java.util.Map.of()));
                player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(snapshot));
            });
            return frame;
        });
        long hit = start + timing.strike();
        context.waitFor(client -> {
            var snapshot = PlayerVisuals.snapshot(client.player.getId());
            return snapshot != null && snapshot.action() == action && snapshot.actionStartFrame() == start
                    && snapshot.sampleFrame() == hit && client.level.getGameTime() == snapshot.sampleGameTime()
                    && client.level.tickRateManager().isFrozen();
        });
        context.runOnClient(client -> {
            var state = (CharacterAvatarState) client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 0);
            var input = state.genshin$characterState().input();
            if (input.action() != action || Math.abs(input.actionSeconds() - timing.strike() / 60.0) > .000001)
                throw new AssertionError("Actual extraction must be exactly at the sourced hitmark: " + input);
            var controller = state.genshin$characterState().getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES)[1];
            if (Math.abs(controller.animationPoint().animTime() - input.actionSeconds()) > .000001)
                throw new AssertionError("Baked action controller must seek the actual hit pose");
            client.particleEngine.clearParticles(); // Keep this pose evidence readable; field gameplay is untouched.
        });
        screenshot(context, name);
        if (intent != CharacterKit.Intent.ATTACK_PRESS) {
            float yaw = context.computeOnClient(client -> ManagedCamera.yaw());
            context.runOnClient(client -> {
                var avatar = (net.minecraft.client.renderer.entity.state.AvatarRenderState)
                        client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 0);
                ManagedCamera.setAngles(avatar.bodyRot, 0);
                client.options.fov().set(defaultFov);
            });
            context.waitTicks(2);
            screenshot(context, name.replace("genshin-combat-", "genshin-pose-") + "-orbit");
            context.runOnClient(client -> { ManagedCamera.setAngles(yaw, 10); client.options.fov().set(55); });
        }
        server.runCommand("tick unfreeze");
        server.runCommand("tick rate 20");
        context.waitFor(client -> PlayerVisuals.snapshot(client.player.getId()).action() == CombatVisual.Action.NONE);
    }

    private static void channelCaptures(ClientGameTestContext context, TestDedicatedServerContext server,
            int slot, String name, int defaultFov) {
        server.runCommand("genshin managed off");
        context.waitFor(client -> !CombatInput.managed());
        server.runCommand("genshin managed on");
        context.waitFor(client -> PlayerVisuals.usesCharacter(client.player));
        server.runOnServer(minecraftServer -> {
            var player = minecraftServer.getPlayerList().getPlayers().getFirst();
            var session = CombatRuntime.get(minecraftServer).session(player);
            if (slot != 0 && !session.intent(CharacterKit.Intent.SWITCH_4, Frames.atServerTick(minecraftServer.getTickCount())))
                throw new AssertionError("Channel capture switch rejected");
        });
        context.waitFor(client -> PlayerVisuals.snapshot(client.player.getId()).slot() == slot);
        context.waitTicks(20);
        long start = server.computeOnServer(minecraftServer -> {
            var session = CombatRuntime.get(minecraftServer).session(minecraftServer.getPlayerList().getPlayers().getFirst());
            long frame = Frames.atServerTick(minecraftServer.getTickCount());
            if (!session.intent(CharacterKit.Intent.SKILL_PRESS, frame)) throw new AssertionError("Channel press rejected");
            return frame;
        });
        channelPose(context, server, slot, CombatVisual.Action.SKILL_START, start + 12,
                "genshin-combat-" + name + "-skill-start", defaultFov);
        channelPose(context, server, slot, CombatVisual.Action.SKILL_HOLD, start + (slot == 0 ? 36 : 120),
                "genshin-combat-" + name + "-skill-hold", defaultFov);
        // Release before Traveler's auto-release, but after Lisa's sourced full-charge threshold.
        long release = server.computeOnServer(minecraftServer -> {
            var session = CombatRuntime.get(minecraftServer).session(minecraftServer.getPlayerList().getPlayers().getFirst());
            int hit = CombatAnimations.timing(slot, CombatVisual.Action.SKILL_RELEASE).strike();
            long frame = Frames.atServerTick(minecraftServer.getTickCount()) + (3 - hit % 3) % 3;
            if (!session.intent(CharacterKit.Intent.SKILL_RELEASE, frame)) throw new AssertionError("Channel release rejected");
            return frame;
        });
        channelPose(context, server, slot, CombatVisual.Action.SKILL_RELEASE,
                release + CombatAnimations.timing(slot, CombatVisual.Action.SKILL_RELEASE).strike(),
                "genshin-combat-" + name + "-skill-release", defaultFov);
        server.runCommand("tick unfreeze");
        server.runCommand("tick rate 20");
        context.waitFor(client -> PlayerVisuals.snapshot(client.player.getId()).action() == CombatVisual.Action.NONE);
    }
    private static io.github.brainage04.genshininminecraft.rules.EventTimeline timeline(net.minecraft.server.MinecraftServer server) {
        try {
            var field = CombatRuntime.class.getDeclaredField("timeline");
            field.setAccessible(true);
            return (io.github.brainage04.genshininminecraft.rules.EventTimeline) field.get(CombatRuntime.get(server));
        } catch (ReflectiveOperationException ex) { throw new AssertionError("Cannot observe test-only timeline", ex); }
    }
    private static void channelPose(ClientGameTestContext context, TestDedicatedServerContext server,
            int slot, CombatVisual.Action action, long frame, String name, int defaultFov) {
        server.runOnServer(minecraftServer -> {
            var player = minecraftServer.getPlayerList().getPlayers().getFirst();
            var session = CombatRuntime.get(minecraftServer).session(player);
            timeline(minecraftServer).schedule(frame, at -> {
                var snapshot = session.visualSnapshot(slot);
                if (snapshot.sampleFrame() != at || snapshot.action() != action)
                    throw new AssertionError("Channel capture must observe actual kit phase: " + snapshot);
                minecraftServer.tickRateManager().setTickRate(1);
                minecraftServer.tickRateManager().setFrozen(true);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTimePacket(snapshot.sampleGameTime(), java.util.Map.of()));
                player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(snapshot));
            });
        });
        server.runCommand("tick unfreeze");
        server.runCommand("tick rate 20");
        context.waitFor(client -> {
            var snapshot = PlayerVisuals.snapshot(client.player.getId());
            return snapshot != null && snapshot.action() == action && snapshot.sampleFrame() == frame
                    && client.level.getGameTime() == snapshot.sampleGameTime() && client.level.tickRateManager().isFrozen();
        });
        context.runOnClient(client -> {
            var state = (CharacterAvatarState) client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 0);
            var input = state.genshin$characterState().input();
            var controller = state.genshin$characterState().getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES)[1];
            if (input.action() != action || Math.abs(controller.animationPoint().animTime() - input.actionSeconds()) > .000001)
                throw new AssertionError("Channel controller must seek the actual start/hold/release pose");
            client.particleEngine.clearParticles();
            var avatar = (net.minecraft.client.renderer.entity.state.AvatarRenderState) state;
            ManagedCamera.setAngles(avatar.bodyRot + 150, 10); client.options.fov().set(55);
        });
        screenshot(context, name);
        context.runOnClient(client -> {
            var avatar = (net.minecraft.client.renderer.entity.state.AvatarRenderState)
                    client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 0);
            ManagedCamera.setAngles(avatar.bodyRot, 0); client.options.fov().set(defaultFov);
        });
        context.waitTicks(2);
        screenshot(context, name.replace("genshin-combat-", "genshin-pose-") + "-orbit");
        context.runOnClient(client -> {
            var avatar = (net.minecraft.client.renderer.entity.state.AvatarRenderState)
                    client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 0);
            ManagedCamera.setAngles(avatar.bodyRot + 150, 10); client.options.fov().set(55);
        });
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
                int required = Locomotion.Phase.values().length;
                for (var action : CombatVisual.Action.values()) {
                    if (CombatAnimations.timing(slot, action) == null) continue;
                    required++;
                    if (model.getBakedAnimation(character, action.clip()) == null)
                        throw new AssertionError("Missing baked combat clip: " + id + ":" + action.clip());
                }
                if (clips.size() != required) throw new AssertionError("Incomplete parsed locomotion/combat set");
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
        combatSeeking();
    }
    private static void combatSeeking() {
        for (int slot = 0; slot < 4; slot++) {
            var character = new CharacterAnimatable(slot);
            for (var action : CombatVisual.Action.values()) {
                var timing = CombatAnimations.timing(slot, action);
                if (timing == null || action == CombatVisual.Action.HURT) continue;
                double seconds = Math.min(.15, timing.recovery() / 120.0);
                var input = combatInput(slot, action, 5, seconds, false);
                var state = PlayerVisuals.renderer().fillRenderState(character, input,
                        PlayerVisuals.renderer().createRenderState(character, input), .5F);
                var captured = state.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES)[1];
                if (Math.abs(captured.animationPoint().animTime() - seconds) > .000001)
                    throw new AssertionError("First late combat seek " + slot + ":" + action);
                var again = PlayerVisuals.renderer().fillRenderState(character, input,
                        PlayerVisuals.renderer().createRenderState(character, input), .5F);
                if (Math.abs(again.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES)[1].animationPoint().animTime() - seconds) > .000001)
                    throw new AssertionError("Duplicate action extraction/pass must not progress time");
                var next = combatInput(slot, action, 6, .01, true);
                var arms = PlayerVisuals.renderer().fillRenderState(character, next,
                        PlayerVisuals.renderer().createRenderState(character, next), .5F);
                if (Math.abs(arms.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES)[1].animationPoint().animTime() - .01) > .000001)
                    throw new AssertionError("New occurrence/first-person must seek its own time");
                if (Math.abs(captured.animationPoint().animTime() - seconds) > .000001)
                    throw new AssertionError("New occurrence must not mutate an already-extracted action snapshot");
                // A newly created view simulates culling/re-entry/cache invalidation/late tracking.
                var fresh = new CharacterAnimatable(slot);
                var reentry = PlayerVisuals.renderer().fillRenderState(fresh, input,
                        PlayerVisuals.renderer().createRenderState(fresh, input), .5F);
                if (Math.abs(reentry.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES)[1].animationPoint().animTime() - seconds) > .000001)
                    throw new AssertionError("Re-entry cannot restart a committed action");
                var clear = combatInput(slot, CombatVisual.Action.NONE, 7, 0, false);
                var cancelled = PlayerVisuals.renderer().fillRenderState(character, clear,
                        PlayerVisuals.renderer().createRenderState(character, clear), .5F);
                var remaining = cancelled.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES);
                if (remaining.length != 1 || Math.abs(remaining[0].animationPoint().animTime() - .1) > .000001)
                    throw new AssertionError("Cancelled/swapped action must leave only the unchanged locomotion controller");
            }
        }
    }
    private static CharacterRenderInput combatInput(int slot, CombatVisual.Action action, int occurrence, double seconds, boolean arms) {
        return new CharacterRenderInput(12000L + slot, slot, Locomotion.Phase.WALK, 1, .1, 100,
                0, 0, 0, 1, 0xf000f0, 0, false, false, 0, 0, arms, action, occurrence, seconds, 0, 1);
    }

    private static CharacterRenderInput input(int slot, Locomotion.Phase phase, int occurrence, double seconds) {
        return new CharacterRenderInput(9999L + slot, slot, phase, occurrence, seconds, 100,
                0, 0, 0, 1, 0xf000f0, 0, false, false, 0, 0, false,
                CombatVisual.Action.NONE, 0, 0, 0, 1);
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
                        slot + 1, 0, 0, 120, client.level.getGameTime(), CombatVisual.Action.NONE, 0, 0, -1, 0, 0, -60));
                var state = client.getEntityRenderDispatcher().getRenderer(remote).createRenderState(remote, 0);
                var geo = ((CharacterAvatarState) state).genshin$characterState();
                if (geo == null || geo.input().slot() != slot) throw new AssertionError("Remote accepted switch must use its own character cache");
            }
            CombatFeedback.accept(new PlayerCharacterPayload(remote.getId(), new UUID(99, 99), 0, Locomotion.Phase.IDLE,
                    1, 0, 0, 120, client.level.getGameTime(), CombatVisual.Action.NONE, 0, 0, -1, 0, 0, -60));
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

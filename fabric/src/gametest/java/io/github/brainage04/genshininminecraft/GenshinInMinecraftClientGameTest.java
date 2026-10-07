package io.github.brainage04.genshininminecraft;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import io.github.brainage04.genshininminecraft.rules.CameraMath;
import net.minecraft.client.CameraType;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import net.minecraft.world.phys.AABB;
import io.github.brainage04.genshininminecraft.client.CombatFeedback;
import io.github.brainage04.genshininminecraft.client.GenshinHud;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.AmberKit;
import io.github.brainage04.genshininminecraft.enemy.BaronBunny;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.lwjgl.glfw.GLFW;


import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public class GenshinInMinecraftClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();

        ClientGameTestServers.withDedicatedServer(context, serverProperties, "GenshinInMinecraft GameTest", server -> { try {
            context.computeOnClient(client -> {
                if (!GenshinInMinecraftClient.isInitialized()) {
                    throw new AssertionError("Expected the client initializer to run before the client GameTest.");
                }
        
                if (client.level == null) {
                    throw new AssertionError("Expected the client to be connected to a world during the client GameTest.");
                }
        
                if (client.player == null) {
                    throw new AssertionError("Expected the client player to be available during the client GameTest.");
                }
        
                return null;
            });
        
            ClientGameTestRecorder.startRecording(context);
            ClientGameTestRecorder.showStep(context, "genshin.ready", "GenshinInMinecraft client GameTest", "client initializer and world ready");
            context.waitTicks(20);
            // The convention's dev-only recording widget overlaps the combat HUD; keep recording
            // and step logs, but capture the actual game's HUD without that diagnostic panel.
            context.runOnClient(client -> HudElementRegistry.removeElement(
                    Identifier.fromNamespaceAndPath("fabricmoddingconventions", "gametest_recording_feedback")));
            // The client GameTest defaults to MINIMAL; visual evidence must enable real particles.
            context.runOnClient(client -> client.options.particles().set(ParticleStatus.ALL));
            context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
            server.runCommand("execute as @a at @s run genshin arena");
            context.waitFor(client -> CombatInput.managed());
            context.waitFor(client -> client.level.getBlockState(client.player.blockPosition().below()).is(Blocks.SMOOTH_STONE));
            server.runCommand("title @a clear");
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");
            context.getInput().lookAt(0, 0);
            context.waitFor(client -> ManagedCamera.decoupled() && client.gameRenderer.mainCamera().isDetached());
            CharacterRenderingChecks.characters(context, server);
            CharacterRenderingChecks.dash(context, server);
            EnemyRenderingChecks.enemies(context, server);
            spectatorExitResendsCameraYaw(context, server);
            context.runOnClient(client -> {
                ManagedCamera.setAngles(90, 15);
                client.player.setYRot(0);
                client.player.setYBodyRot(0);
            });
            var orbitOrigin = context.computeOnClient(client -> client.player.position());
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(6);
            context.getInput().releaseKey(options -> options.keyUp);
            context.runOnClient(client -> {
                var travel = client.player.position().subtract(orbitOrigin);
                if (travel.x >= -.5 || Math.abs(travel.z) > .12)
                    throw new AssertionError("Camera yaw90 W must move -X, independently of initial body yaw0: " + travel);
                if (Math.abs(CameraMath.wrap(client.player.getYRot() - 90)) > 1
                        || Math.abs(CameraMath.wrap(client.player.yBodyRot - 90)) > 5)
                    throw new AssertionError("Character must turn smoothly to camera-relative movement yaw90");
                if (client.options.getCameraType() != CameraType.THIRD_PERSON_BACK
                        || !client.gameRenderer.mainCamera().isDetached()
                        || client.gameRenderer.mainCamera().position().distanceTo(client.player.getEyePosition()) < 3.5)
                    throw new AssertionError("Managed orbit must render the character from approximately four blocks behind");
                client.gui.hud.getChat().clearMessages(true);
            });
            server.waitFor(minecraftServer -> Math.abs(CameraMath.wrap(
                    minecraftServer.getPlayerList().getPlayers().getFirst().getYRot() - 90)) < 1);
            screenshot(context, "genshin-camera-orbit");
            context.waitTicks(10);
            // Raw mouse input must change only the orbit; the body remains stationary-facing.
            float facingBeforeMouse = context.computeOnClient(client -> client.player.getYRot());
            // Vanilla deliberately discards the first cursor event after mouse grab.
            context.getInput().moveCursor(0, 0);
            context.waitTicks(1);
            context.getInput().moveCursor(40, 0);
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (Math.abs(CameraMath.wrap(ManagedCamera.yaw() - 90)) < .1
                        || Math.abs(CameraMath.wrap(client.player.getYRot() - facingBeforeMouse)) > .1)
                    throw new AssertionError("Managed mouse input must orbit without turning an idle character: camera="
                            + ManagedCamera.yaw() + ", body=" + client.player.getYRot() + ", previousBody=" + facingBeforeMouse
                            + ", grabbed=" + client.mouseHandler.isMouseGrabbed());
                ManagedCamera.setAngles(90, 0);
            });
            server.runCommand("execute as @a at @s run fill ~2 ~ ~-1 ~2 ~3 ~1 minecraft:stone");
            context.waitFor(client -> client.gameRenderer.mainCamera().position().distanceTo(client.player.getEyePosition()) < 2.6);
            screenshot(context, "genshin-camera-collision");
            server.runCommand("execute as @a at @s run fill ~2 ~ ~-1 ~2 ~3 ~1 minecraft:air");
            context.waitFor(client -> client.gameRenderer.mainCamera().position().distanceTo(client.player.getEyePosition()) > 3.5);
            // A strafe dash must use camera WASD, NOT raw A rotated again by the turned body.
            context.runOnClient(client -> ManagedCamera.setAngles(0, 15));
            context.getInput().holdKey(options -> options.keyLeft);
            context.waitTicks(5);
            context.getInput().holdKey(options -> options.keySprint);
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return CombatRuntime.get(minecraftServer).session(player).stamina().dashing(
                        Frames.atServerTick(minecraftServer.getTickCount()));
            });
            server.runOnServer(minecraftServer -> {
                var motion = minecraftServer.getPlayerList().getPlayers().getFirst().getDeltaMovement();
                if (motion.x < .5 || Math.abs(motion.z) > .01)
                    throw new AssertionError("Camera yaw0 A dash must go +X despite body yaw-90: " + motion);
            });
            context.getInput().releaseKey(options -> options.keySprint);
            context.getInput().releaseKey(options -> options.keyLeft);
            context.waitFor(client -> CombatInput.state().stamina() == 100 && !client.player.isSprinting());
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.waitFor(client -> client.options.getCameraType() == CameraType.FIRST_PERSON
                    && !ManagedCamera.decoupled() && !client.gameRenderer.mainCamera().isDetached());
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.waitFor(client -> ManagedCamera.decoupled());
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");
            context.runOnClient(client -> ManagedCamera.setAngles(0, 0));
            int[] lockTargets = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                int[] ids = new int[2];
                for (int index = 0; index < 2; index++) {
                    var mob = new Hilichurl(GenshinEntities.HILICHURL, player.level());
                    mob.snapTo(player.position().add(index == 0 ? 2 : 0, 0, index == 0 ? 2 : -1));
                    mob.setNoAi(true);
                    if (!player.level().addFreshEntity(mob)) throw new AssertionError("Could not spawn soft-lock fixture");
                    ids[index] = mob.getId();
                }
                return ids;
            });
            context.waitFor(client -> client.level.getEntity(lockTargets[0]) != null && client.level.getEntity(lockTargets[1]) != null);
            context.getInput().pressKey(options -> options.keyAttack);
            context.waitFor(client -> CombatFeedback.hasDamageNumber(lockTargets[0]));
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                if (Math.abs(CameraMath.wrap(player.getYRot() + 45)) > 1
                        || player.level().getEntity(lockTargets[1]) instanceof Hilichurl behind && behind.getHealth() != behind.getMaxHealth())
                    throw new AssertionError("Attack must send snapped front-target body yaw before intent and exclude the closer rear enemy");
                player.level().getEntity(lockTargets[0]).discard();
                player.level().getEntity(lockTargets[1]).discard();
            });
            context.runOnClient(client -> {
                if (Math.abs(ManagedCamera.yaw()) > .1) throw new AssertionError("Soft lock must not rotate the orbit camera");
                ManagedCamera.setAngles(0, 0);
            });
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");
            context.waitTicks(20);
            context.runOnClient(client -> {
                if (CombatInput.state().stamina() != 100 || GenshinHud.staminaVisible())
                    throw new AssertionError("Full, idle new-player100 stamina must hide the wheel");
            });
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(3);
            var dashOrigin = context.computeOnClient(client -> client.player.position());
            context.getInput().holdKey(options -> options.keySprint);
            context.waitTicks(6);
            var dashTravel = context.computeOnClient(client -> client.player.position().subtract(dashOrigin));
            GenshinInMinecraft.LOGGER.info("Dash input probe: {} horizontal blocks over six client ticks from Sprint press (includes network delivery)",
                    Math.sqrt(dashTravel.horizontalDistanceSqr()));
            context.waitFor(client -> CombatInput.state().stamina() <= 80 && CombatInput.state().staminaDraining());
            float sprintStamina = context.computeOnClient(client -> CombatInput.state().stamina());
            context.waitTicks(10);
            context.runOnClient(client -> {
                if (CombatInput.state().stamina() >= sprintStamina || !client.player.isSprinting())
                    throw new AssertionError("Holding vanilla sprint must drain server-synced stamina in the managed arena");
                if (!GenshinHud.staminaVisible())
                    throw new AssertionError("A draining, partially depleted stamina wheel must be visible");
            });
            context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
            screenshot(context, "genshin-stamina-wheel");
            context.getInput().releaseKey(options -> options.keySprint);
            context.getInput().releaseKey(options -> options.keyUp);
            context.waitFor(client -> !client.player.isSprinting() && !CombatInput.state().staminaDraining());
            context.waitFor(client -> CombatInput.state().stamina() == 100 && !GenshinHud.staminaVisible());
            traversalControls(context, server);
            int originalHotbar = context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
            context.getInput().pressKey(GLFW.GLFW_KEY_2);
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return CombatRuntime.get(minecraftServer).session(player).party().activeSlot() == 1;
            });
            context.waitFor(client -> CombatInput.state().activeSlot() == 1);
            context.runOnClient(client -> {
                if (client.player.getInventory().getSelectedSlot() != originalHotbar)
                    throw new AssertionError("Managed party key2 must not also change vanilla hotbar selection");
                if (GenshinHud.party().size() != 4 || !GenshinHud.party().get(1).active()
                        || GenshinHud.party().getFirst().active())
                    throw new AssertionError("HUD must show four element-coloured rows and highlight active Amber");
            });
            screenshot(context, "genshin-party-four");
            context.runOnClient(client -> {
                if (CombatInput.state().switchRemainingFrames() <= 0 || !GenshinHud.partyUnavailable(0)
                        || GenshinHud.partyUnavailable(1))
                    throw new AssertionError("After real key2, living off-field rows must dim/sweep while active Amber stays readable");
            });
            screenshot(context, "genshin-switch-cooldown");
            int switchRejection = context.computeOnClient(client -> CombatInput.state().rejectionSerial());
            context.getInput().pressKey(GLFW.GLFW_KEY_1);
            context.waitFor(client -> CombatInput.state().rejectionSerial() != switchRejection);
            context.runOnClient(client -> {
                if (CombatInput.state().activeSlot() != 1 || !GenshinHud.rejectionVisible()
                        || !GenshinHud.rejectionText().equals("Character-switching in cooldown"))
                    throw new AssertionError("Rejected cooldown switch must stay on Amber and show the sourced brief overlay");
            });
            float ordinaryFov = context.computeOnClient(client -> client.gameRenderer.mainCamera().getFov());
            context.getInput().holdKey(options -> options.keyAttack);
            context.waitFor(client -> CombatInput.aiming() && client.gameRenderer.mainCamera().getFov() < ordinaryFov * .85F);
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return ((AmberKit) CombatRuntime.get(minecraftServer).session(player).kit()).fullyCharged();
            });
            context.waitFor(client -> CombatInput.fullyChargedAim());
            context.waitFor(client -> CombatInput.state().switchBlocked() && CombatInput.state().switchRemainingFrames() == 0);
            context.runOnClient(client -> {
                if (!GenshinHud.partyUnavailable(0) || !CombatInput.state().actionBlocked())
                    throw new AssertionError("Held bow aim must dim party/E/Q without inventing a release countdown");
            });
            int aimRejection = context.computeOnClient(client -> CombatInput.state().rejectionSerial());
            context.getInput().pressKey(GLFW.GLFW_KEY_1);
            context.waitFor(client -> CombatInput.state().rejectionSerial() != aimRejection);
            context.runOnClient(client -> {
                if (!CombatInput.aiming() || CombatInput.state().activeSlot() != 1
                        || !GenshinHud.rejectionText().equals("Cannot switch character now"))
                    throw new AssertionError("Rejected switch during aim must keep Amber's held shot and report the action lock");
            });
            screenshot(context, "genshin-party-action-locked");
            context.runOnClient(client -> ManagedCamera.setAngles(90, 0));
            context.waitTicks(2);
            context.runOnClient(client -> {
                var camera = client.gameRenderer.mainCamera();
                var offset = camera.position().subtract(client.player.getEyePosition());
                if (!camera.isDetached() || offset.x < 1.7 || offset.x > 2.3 || offset.z > -.4 || offset.z < -.8
                        || Math.abs(CameraMath.wrap(client.player.getYRot() - 90)) > .1)
                    throw new AssertionError("Amber aim must use clipped2-block/right-offset shoulder view with body along aim yaw: " + offset);
            });
            int aimTarget = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var mob = new Hilichurl(GenshinEntities.HILICHURL, player.level());
                mob.snapTo(player.position().add(-4, 0, 0));
                mob.setNoAi(true);
                if (!player.level().addFreshEntity(mob)) throw new AssertionError("Could not spawn camera-aim target");
                return mob.getId();
            });
            context.waitFor(client -> client.level.getEntity(aimTarget) != null);
            screenshot(context, "genshin-camera-amber-shoulder");
            screenshot(context, "genshin-amber-aim");
            context.getInput().releaseKey(options -> options.keyAttack);
            context.waitFor(client -> !CombatInput.aiming() && client.gameRenderer.mainCamera().getFov() > ordinaryFov * .9F);
            context.waitFor(client -> CombatFeedback.hasDamageNumber(aimTarget)
                    && (CombatFeedback.auraElements(aimTarget) & (1 << Element.PYRO.ordinal())) != 0);
            server.runOnServer(minecraftServer -> minecraftServer.getPlayerList().getPlayers().getFirst().level().getEntity(aimTarget).discard());
            context.runOnClient(client -> ManagedCamera.setAngles(0, 0));
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");
            context.waitTicks(5);
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return !player.level().getEntitiesOfClass(BaronBunny.class, new AABB(player.position(), player.position()).inflate(8)).isEmpty();
            });
            context.waitFor(client -> {
                for (var entity : client.level.entitiesForRendering())
                    if (entity instanceof BaronBunny && entity.getCustomName() != null
                            && entity.getCustomName().getString().equals("Baron Bunny")) return true;
                return false;
            });
            int bunnyId = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return player.level().getEntitiesOfClass(BaronBunny.class,
                        new AABB(player.position(), player.position()).inflate(8)).getFirst().getId();
            });
            context.waitFor(client -> CombatInput.state().skillRemainingFrames() > 0);
            context.runOnClient(client -> ManagedCamera.setAngles(0, 12));
            context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
            screenshot(context, "genshin-amber-baron-bunny");
            int pyroMobId = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var mob = new Hilichurl(GenshinEntities.HILICHURL, player.level());
                mob.snapTo(player.position().add(1.25, 0, 3)); // Separate silhouettes, still inside the inner rain region.
                mob.setNoAi(true);
                if (!((ServerLevel) player.level()).addFreshEntity(mob)) throw new AssertionError("Could not spawn rain target");
                CombatRuntime.get(minecraftServer).session(player).kit().grantEnergy(40);
                return mob.getId();
            });
            context.waitFor(client -> CombatInput.state().energy() == 40);
            context.runOnClient(client -> client.particleEngine.clearParticles());
            context.getInput().pressKey(GLFW.GLFW_KEY_Q);
            context.waitFor(client -> CombatInput.state().energy() == 0 && CombatInput.state().burstRemainingFrames() > 0
                    && CombatFeedback.hasDamageNumber(pyroMobId)
                    && (CombatFeedback.auraElements(pyroMobId) & (1 << Element.PYRO.ordinal())) != 0);
            context.waitFor(client -> {
                String particles = client.particleEngine.countParticles();
                return Integer.parseInt(particles.substring(particles.lastIndexOf(' ') + 1)) > 0;
            });
            context.runOnClient(client -> {
                if (client.gui.screen() instanceof InventoryScreen) throw new AssertionError("Amber E must not open inventory");
                if (CombatFeedback.auraElements(bunnyId) != 0 || CombatFeedback.hasDamageNumber(bunnyId))
                    throw new AssertionError("Fiery Rain must not apply aura or damage feedback to Amber's own Bunny");
            });
            screenshot(context, "genshin-amber-fiery-rain");
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var bunny = (BaronBunny) player.level().getEntity(bunnyId);
                if (bunny.getHealth() != bunny.getMaxHealth()
                        || ((AmberKit) CombatRuntime.get(minecraftServer).session(player).kit()).puppetHp() != 2037.88 * .4136)
                    throw new AssertionError("Fiery Rain must leave the source-scaled Bunny HP full");
                player.level().getEntity(pyroMobId).discard();
            });
            context.runOnClient(client -> ManagedCamera.setAngles(0, 0));
            context.waitTicks(40);
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var bunny = (BaronBunny) player.level().getEntity(bunnyId);
                if (bunny.getHealth() != bunny.getMaxHealth())
                    throw new AssertionError("All Fiery Rain waves must leave Bunny HP full");
            });
            context.runOnClient(client -> {
                if (CombatFeedback.auraElements(bunnyId) != 0 || CombatFeedback.hasDamageNumber(bunnyId))
                    throw new AssertionError("No Bunny aura/damage feedback may appear throughout Fiery Rain");
            });
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return !((AmberKit) CombatRuntime.get(minecraftServer).session(player).party().kit(1)).puppetAlive();
            });
            context.getInput().pressKey(GLFW.GLFW_KEY_3);
            context.waitFor(client -> CombatInput.state().activeSlot() == 2);
            int cryoMobId = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var mob = new Hilichurl(GenshinEntities.HILICHURL, player.level());
                mob.snapTo(player.position().add(0, 0, 4));
                mob.setNoAi(true);
                if (!((ServerLevel) player.level()).addFreshEntity(mob)) throw new AssertionError("Could not spawn Kaeya HUD target");
                return mob.getId();
            });
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            context.waitFor(client -> CombatInput.state().skillRemainingFrames() > 0 && CombatInput.state().energy() == 9
                    && CombatFeedback.hasDamageNumber(cryoMobId)
                    && (CombatFeedback.auraElements(cryoMobId) & (1 << Element.CRYO.ordinal())) != 0);
            context.runOnClient(client -> {
                if (!GenshinHud.party().get(2).active() || CombatInput.state().energy() != 9
                        || client.gui.screen() instanceof InventoryScreen)
                    throw new AssertionError("Kaeya E must show authoritative cooldown, Cryo damage/aura and9 skill energy, not SOON/inventory");
            });
            screenshot(context, "genshin-kaeya-frostgnaw");
            context.runOnClient(client -> {
                if (GenshinHud.skillSweepFraction() <= 0 || GenshinHud.skillSweepFraction() >= 1)
                    throw new AssertionError("Frostgnaw sweep must reflect a partially elapsed six-second cooldown");
            });
            screenshot(context, "genshin-skill-cooldown-sweep");
            long skillReady = server.computeOnServer(minecraftServer -> CombatRuntime.get(minecraftServer)
                    .session(minecraftServer.getPlayerList().getPlayers().getFirst()).kit().skillReadyFrame());
            int skillRejection = context.computeOnClient(client -> CombatInput.state().rejectionSerial());
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            context.waitFor(client -> CombatInput.state().rejectionSerial() != skillRejection);
            context.runOnClient(client -> {
                if (!GenshinHud.rejectionVisible() || !GenshinHud.rejectionText().equals("Elemental Skill is on cooldown"))
                    throw new AssertionError("Server-rejected E must briefly flash its icon and explain the cooldown");
            });
            server.runOnServer(minecraftServer -> {
                var kit = CombatRuntime.get(minecraftServer).session(minecraftServer.getPlayerList().getPlayers().getFirst()).kit();
                if (kit.skillReadyFrame() != skillReady || kit.energy() != 9)
                    throw new AssertionError("Rejected E must neither restart Frostgnaw cooldown nor grant energy");
            });
            server.runOnServer(minecraftServer -> CombatRuntime.get(minecraftServer)
                    .session(minecraftServer.getPlayerList().getPlayers().getFirst()).kit().grantEnergy(21));
            context.waitFor(client -> CombatInput.state().energy() == 30);
            screenshot(context, "genshin-burst-half-energy");
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                player.level().getEntity(cryoMobId).discard();
            });
            context.waitTicks(22);
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                CombatRuntime.get(minecraftServer).session(player).kit().grantEnergy(60);
            });
            context.waitFor(client -> CombatInput.state().energy() == 60 && CombatInput.state().burstRemainingFrames() == 0);
            screenshot(context, "genshin-kaeya-burst-ready");
            context.runOnClient(client -> client.particleEngine.clearParticles());
            context.getInput().pressKey(GLFW.GLFW_KEY_Q);
            context.waitFor(client -> CombatInput.state().energy() == 0 && CombatInput.state().burstRemainingFrames() > 0);
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return ((io.github.brainage04.genshininminecraft.rules.kit.KaeyaKit)
                        CombatRuntime.get(minecraftServer).session(player).kit()).burstActive(
                                Frames.atServerTick(minecraftServer.getTickCount()));
            });
            context.waitFor(client -> {
                String particles = client.particleEngine.countParticles();
                return Integer.parseInt(particles.substring(particles.lastIndexOf(' ') + 1)) > 0;
            });
            screenshot(context, "genshin-kaeya-glacial-waltz");
            // Orbit no longer needs three F5 presses; retain the actual action's swap boundary.
            server.waitFor(minecraftServer -> {
                var party = CombatRuntime.get(minecraftServer).session(minecraftServer.getPlayerList().getPlayers().getFirst()).party();
                long frame = Frames.atServerTick(minecraftServer.getTickCount());
                return frame >= party.switchReadyFrame() && party.activeKit().canSwitch(frame);
            });
            context.getInput().pressKey(GLFW.GLFW_KEY_1);
            context.waitFor(client -> CombatInput.state().activeSlot() == 0);
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return !((io.github.brainage04.genshininminecraft.rules.kit.KaeyaKit)
                        CombatRuntime.get(minecraftServer).session(player).party().kit(2)).burstActive(
                                Frames.atServerTick(minecraftServer.getTickCount()));
            });
            context.getInput().pressKey(GLFW.GLFW_KEY_4);
            context.waitFor(client -> CombatInput.state().activeSlot() == 3);
            int electroMobId = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var mob = new Hilichurl(GenshinEntities.HILICHURL, player.level());
                // This multi-tick EC/Rose fixture must survive several145.052595 reaction ticks.
                // Keep the original sourced Lv20/885.200HP encounter; the arena-default fixture below tests Lv8.
                mob.setGenshinLevel(20);
                mob.snapTo(player.position().add(0, 0, 3));
                mob.setNoAi(true);
                mob.setYRot(180); mob.setYBodyRot(180); mob.setYHeadRot(180);
                if (!((ServerLevel) player.level()).addFreshEntity(mob)) throw new AssertionError("Could not spawn Lisa/EC target");
                return mob.getId();
            });
            server.runCommand("execute as @a at @s run genshin aura hydro 4");
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            context.waitFor(client -> CombatFeedback.conductiveStacks(electroMobId) == 1
                    && CombatInput.state().skillRemainingFrames() > 0 && CombatFeedback.hasDamageNumber(electroMobId));
            context.runOnClient(client -> {
                if (client.gui.screen() instanceof InventoryScreen)
                    throw new AssertionError("Lisa E must cast Violet Arc, not open inventory");
            });
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                CombatRuntime.get(minecraftServer).session(player).kit().grantEnergy(80);
            });
            context.waitFor(client -> CombatInput.state().energy() == 80);
            context.runOnClient(client -> client.particleEngine.clearParticles());
            context.getInput().pressKey(GLFW.GLFW_KEY_Q);
            context.waitFor(client -> CombatInput.state().energy() == 0 && CombatInput.state().burstRemainingFrames() > 0);
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var session = CombatRuntime.get(minecraftServer).session(player);
                var target = CombatRuntime.get(minecraftServer).target((Hilichurl) player.level().getEntity(electroMobId));
                long cast = session.kit().burstReadyFrame() - 53 - 1200;
                return Frames.atServerTick(minecraftServer.getTickCount()) >= cast + 119
                        && target.aura().gauge(Element.HYDRO) > 0 && target.aura().gauge(Element.ELECTRO) > 0;
            });
            server.runCommand("execute as @a at @s run tp @s ~-2 ~ ~ 0 0");
            context.runOnClient(client -> ManagedCamera.setAngles(-15, 15));
            context.waitFor(client -> (CombatFeedback.auraElements(electroMobId) & (1 << Element.HYDRO.ordinal())) != 0
                    && (CombatFeedback.auraElements(electroMobId) & (1 << Element.ELECTRO.ordinal())) != 0
                    && CombatFeedback.hasDamageNumber(electroMobId) && CombatFeedback.conductiveStacks(electroMobId) == 1);
            context.waitFor(client -> {
                String particles = client.particleEngine.countParticles();
                return Integer.parseInt(particles.substring(particles.lastIndexOf(' ') + 1)) > 0;
            });
            context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
            screenshot(context, "genshin-lisa-rose-electro-charged");
            context.runOnClient(client -> ManagedCamera.setAngles(0, 0));
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                player.level().getEntity(electroMobId).discard();
            });
            context.getInput().pressKey(GLFW.GLFW_KEY_1);
            context.waitFor(client -> CombatInput.state().activeSlot() == 0);
            int mobId = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var mob = new Cow(EntityTypes.COW, player.level());
                mob.snapTo(player.position().add(0, 0, 2));
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                if (!((ServerLevel) player.level()).addFreshEntity(mob)) throw new AssertionError("Could not spawn HUD test cow");
                CombatRuntime.get(minecraftServer).target(mob).aura().applyHit(Element.PYRO, 2,
                        Frames.atServerTick(minecraftServer.getTickCount()));
                return mob.getId();
            });
            context.waitFor(client -> CombatFeedback.auraElements(mobId) == (1 << Element.PYRO.ordinal()));
            screenshot(context, "genshin-hud-aura");
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            server.waitFor(minecraftServer -> minecraftServer.getPlayerList().getPlayers().stream().anyMatch(player ->
                    CombatRuntime.get(minecraftServer).session(player).kit().skillReadyFrame() > 0));
            context.waitFor(client -> CombatInput.state().skillRemainingFrames() > 0 && CombatFeedback.hasDamageNumber(mobId));
            context.runOnClient(client -> {
                if (client.gui.screen() instanceof InventoryScreen) throw new AssertionError("Managed E must not also open vanilla inventory");
                if (CombatInput.state().skillRemainingFrames() <= 0) throw new AssertionError("Server cooldown must reach the client");
                if (GenshinHud.party().size() != 4 || !GenshinHud.party().getFirst().active()) {
                    throw new AssertionError("HUD must expose all four members with active Traveler");
                }
                if (!CombatFeedback.hasDamageNumber(mobId)) throw new AssertionError("Skill hit must sync a damage number for the test mob");
                if (CombatFeedback.extract(0).isEmpty()) throw new AssertionError("Managed HUD must extract visible world feedback");
            });
            screenshot(context, "genshin-hud-skill");
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                CombatRuntime.get(minecraftServer).session(player).kit().grantEnergy(60);
            });
            context.waitFor(client -> CombatInput.state().energy() == 60 && CombatInput.state().burstRemainingFrames() == 0);
            screenshot(context, "genshin-hud-burst-ready");
            ClientGameTestRecorder.showStep(context, "genshin.skill", "Managed arena controls", "E starts server-owned Palm Vortex, not inventory");
            server.runCommand("execute as @a at @s run genshin arena camp");
            context.waitFor(client -> CombatInput.managed());
            // Move close enough to inspect the original masks, but remain out of aggro in creative.
            server.runCommand("gamemode creative @a");
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~10 0 0");
            context.runOnClient(client -> ManagedCamera.setAngles(0, 0));
            context.waitFor(client -> {
                int count = 0;
                for (var entity : client.level.entitiesForRendering()) if (entity.getType() == GenshinEntities.HILICHURL) count++;
                return count == 3;
            });
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var camp = player.level().getEntitiesOfClass(Hilichurl.class, new AABB(player.position(), player.position()).inflate(12));
                if (camp.size() != 3) throw new AssertionError("Arena camp command must spawn three hilichurls");
                for (Hilichurl member : camp) {
                    // Present each original mask to the camera rather than taking a back-view only.
                    member.setYRot(180);
                    member.setYBodyRot(180);
                    member.setYHeadRot(180);
                }
            });
            context.waitTicks(10);
            context.runOnClient(client -> {
                long labels = CombatFeedback.extract(0).stream().filter(text -> {
                    StringBuilder value = new StringBuilder();
                    text.text().accept((index, style, codePoint) -> { value.appendCodePoint(codePoint); return true; });
                    return value.toString().equals("Lv. 8 · Hilichurl");
                }).count();
                if (labels != 3) throw new AssertionError("Every arena hilichurl must show its synced Lv. 8 name line");
            });
            context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
            screenshot(context, "genshin-hilichurl-camp");
            server.runOnServer(minecraftServer -> CombatRuntime.get(minecraftServer)
                    .session(minecraftServer.getPlayerList().getPlayers().getFirst()).party().kit(1).setHp(0));
            context.waitFor(client -> CombatInput.state().members().get(1).hpFraction() == 0);
            context.runOnClient(client -> {
                if (!GenshinHud.partyUnavailable(1) || GenshinHud.party().get(1).hp() != 0)
                    throw new AssertionError("Fallen Amber must have an empty HP bar and grey unavailable row");
            });
            screenshot(context, "genshin-party-dead-member");
            int fallenRejection = context.computeOnClient(client -> CombatInput.state().rejectionSerial());
            context.getInput().pressKey(GLFW.GLFW_KEY_2);
            context.waitFor(client -> CombatInput.state().rejectionSerial() != fallenRejection);
            context.runOnClient(client -> {
                if (CombatInput.state().activeSlot() != 0 || !GenshinHud.rejectionText().equals("Character is down"))
                    throw new AssertionError("Selecting a fallen member must preserve active Traveler and explain rejection");
            });
            server.runCommand("gamemode adventure @a");
            context.waitFor(client -> !client.player.isCreative());
            server.runCommand("genshin managed off");
            context.waitFor(client -> !CombatInput.managed());
            context.waitFor(client -> client.options.getCameraType() == CameraType.FIRST_PERSON
                    && !ManagedCamera.decoupled() && !client.gameRenderer.mainCamera().isDetached());
            CharacterRenderingChecks.unmanaged(context);
            context.getInput().lookAt(37, 12);
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (Math.abs(CameraMath.wrap(client.gameRenderer.mainCamera().yRot() - client.player.getYRot())) > .1)
                    throw new AssertionError("Unmanaged first-person camera must follow vanilla entity yaw");
            });
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.waitFor(client -> client.options.getCameraType() == CameraType.THIRD_PERSON_BACK && client.gameRenderer.mainCamera().isDetached());
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.waitFor(client -> client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT);
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.waitFor(client -> client.options.getCameraType() == CameraType.FIRST_PERSON);
            context.runOnClient(client -> {
                if (!GenshinHud.party().isEmpty() || CombatFeedback.hasDamageNumber(mobId)) {
                    throw new AssertionError("Managed-off must clear HUD resources and world feedback");
                }
            });
            context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
            screenshot(context, "genshin-hud-vanilla");
            context.getInput().pressKey(GLFW.GLFW_KEY_2);
            context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 1);
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            context.waitForScreen(InventoryScreen.class);
        } finally {
            context.getInput().releaseKey(GLFW.GLFW_KEY_E);
            context.getInput().releaseKey(options -> options.keySprint);
            context.getInput().releaseKey(options -> options.keyUp);
            context.getInput().releaseKey(options -> options.keyLeft);
            context.getInput().releaseKey(options -> options.keyRight);
            context.getInput().releaseKey(options -> options.keyAttack);
        } });
    }

    private static void traversalControls(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runCommand("execute as @a at @s run genshin arena");
        context.waitTicks(5);
        var base = context.computeOnClient(client -> client.player.blockPosition());
        String wall = (base.getX() - 2) + " " + base.getY() + " " + (base.getZ() + 3) + " "
                + (base.getX() + 2) + " " + (base.getY() + 10) + " " + (base.getZ() + 3);
        server.runCommand("fill " + wall + " minecraft:stone");
        server.runCommand("tp @a " + (base.getX() + .5) + " " + base.getY() + " " + (base.getZ() + .5) + " 0 0");
        context.runOnClient(client -> ManagedCamera.setAngles(0, 12));
        context.waitTicks(5);
        double groundY = context.computeOnClient(client -> client.player.getY());
        context.getInput().holdKey(options -> options.keyUp);
        context.waitFor(client -> CombatInput.state().climbing());
        context.waitFor(client -> client.player.getY() > groundY + 2);
        context.getInput().releaseKey(options -> options.keyUp);
        context.runOnClient(client -> {
            if (!CombatInput.state().climbing() || !GenshinHud.staminaVisible() || CombatInput.state().stamina() >= 100)
                throw new AssertionError("Real W wall entry must rise, consume authoritative stamina and display wheel");
            client.gui.hud.getChat().clearMessages(true);
        });
        screenshot(context, "genshin-traversal-climbing");
        CharacterRenderingChecks.traversal(context, "climb", false);
        context.getInput().holdKey(options -> options.keyShift);
        context.waitFor(client -> !CombatInput.state().climbing());
        context.getInput().releaseKey(options -> options.keyShift);
        server.runCommand("fill " + wall + " minecraft:air");
        String tower = (base.getX() + 6) + " " + base.getY() + " " + base.getZ() + " "
                + (base.getX() + 8) + " " + (base.getY() + 11) + " " + (base.getZ() + 2);
        server.runCommand("fill " + tower + " minecraft:stone");
        server.runCommand("tp @a " + (base.getX() + 7.5) + " " + (base.getY() + 12) + " " + (base.getZ() + 1.5) + " 0 0");
        context.waitFor(client -> client.player.onGround() && client.player.getY() > groundY + 11);
        context.runOnClient(client -> ManagedCamera.setAngles(0, 15));
        context.getInput().holdKey(options -> options.keyUp);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitFor(client -> client.player.getZ() > base.getZ() + 3.5 && !client.player.onGround());
        context.getInput().releaseKey(options -> options.keyJump);
        context.waitTicks(2);
        context.getInput().pressKey(options -> options.keyJump);
        context.waitFor(client -> CombatInput.state().gliding());
        context.getInput().releaseKey(options -> options.keyUp);
        double glideY = context.computeOnClient(client -> client.player.getY());
        int glideTick = context.computeOnClient(client -> client.player.tickCount);
        context.waitTicks(6);
        context.runOnClient(client -> {
            double descent = glideY - client.player.getY();
            double expected = (client.player.tickCount - glideTick) * .11755;
            if (!CombatInput.state().gliding() || Math.abs(descent - expected) > .01
                    || Math.abs(client.player.getDeltaMovement().y + .11755) > .00001)
                throw new AssertionError("Glider must descend at historical adapted2.351m/s: actual=" + descent + ", expected=" + expected);
            if (!GenshinHud.staminaVisible() || !CombatInput.state().staminaDraining())
                throw new AssertionError("Glider must show shared stamina wheel/drain");
            client.gui.hud.getChat().clearMessages(true);
        });
        screenshot(context, "genshin-traversal-gliding");
        CharacterRenderingChecks.traversal(context, "glide", true);
        context.getInput().pressKey(options -> options.keyJump);
        context.waitFor(client -> !CombatInput.state().gliding());
        server.runCommand("tp @a " + (base.getX() + .5) + " " + base.getY() + " " + (base.getZ() + .5) + " 0 0");
        server.runCommand("fill " + tower + " minecraft:air");
        context.waitFor(client -> client.player.onGround() && CombatInput.state().stamina() == 100);
        climbCornerControls(context, server);
        context.runOnClient(client -> ManagedCamera.setAngles(0, 0));
    }

    private static void climbCornerControls(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runCommand("execute as @a at @s run genshin arena");
        context.waitTicks(5);
        var base = context.computeOnClient(client -> client.player.blockPosition());
        String pillar = base.getX() + " " + base.getY() + " " + (base.getZ() + 3) + " "
                + (base.getX() + 2) + " " + (base.getY() + 5) + " " + (base.getZ() + 5);
        server.runCommand("fill " + pillar + " minecraft:stone");
        server.runCommand("tp @a " + (base.getX() + 1.5) + " " + base.getY() + " " + (base.getZ() + .5) + " 0 0");
        context.runOnClient(client -> ManagedCamera.setAngles(0, 12));
        context.waitTicks(5);
        context.getInput().holdKey(options -> options.keyUp);
        context.waitFor(client -> CombatInput.state().climbing() && client.player.getY() > base.getY() + 2);
        context.getInput().releaseKey(options -> options.keyUp);
        context.waitTicks(3);
        double height = context.computeOnClient(client -> client.player.getY());
        context.runOnClient(client -> ManagedCamera.setAngles(-45, 15));
        context.getInput().holdKey(options -> options.keyRight);
        context.waitTicks(5);
        double[] sample = server.computeOnServer(minecraftServer -> {
            var player = minecraftServer.getPlayerList().getPlayers().getFirst();
            var stamina = CombatRuntime.get(minecraftServer).session(player).stamina();
            return new double[]{stamina.frame(), stamina.current()};
        });
        for (var normal : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.NORTH,
                net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.SOUTH}) {
            context.waitFor(client -> {
                if (!CombatInput.state().climbing()) throw new AssertionError("Held D must stay attached around all four pillar faces");
                if (Math.abs(CameraMath.wrap(ManagedCamera.yaw() + 45)) > .01)
                    throw new AssertionError("Climb body turns must not rotate the independent camera");
                return CombatInput.state().wallOrdinal() == normal.ordinal();
            });
            if (normal == net.minecraft.core.Direction.EAST) {
                context.waitTicks(6);
                context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
                screenshot(context, "genshin-traversal-corner");
                server.runOnServer(minecraftServer -> {
                    var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    var stamina = CombatRuntime.get(minecraftServer).session(player).stamina();
                    double frames = stamina.frame() - sample[0];
                    double spent = sample[1] - stamina.current();
                    if (frames < 60 || Math.abs(spent - frames * 5.36 / 60) > .000001)
                        throw new AssertionError("Measured connected-client climb must drain5.36/s: frames=" + frames + ", spent=" + spent);
                });
            }
        }
        context.waitFor(client -> {
            if (!CombatInput.state().climbing()) throw new AssertionError("Final face must remain attached");
            return client.player.getX() <= base.getX() + 1.5;
        });
        context.getInput().releaseKey(options -> options.keyRight);
        context.waitTicks(3);
        context.runOnClient(client -> {
            if (CombatInput.state().wallOrdinal() != net.minecraft.core.Direction.SOUTH.ordinal()
                    || Math.abs(client.player.getZ() - base.getZ() - 2.7) > .01
                    || Math.abs(client.player.getY() - height) > .01
                    || Math.abs(CameraMath.wrap(client.player.getYRot())) > .01)
                throw new AssertionError("Four-corner loop must return to start face/height with body facing the wall: " + client.player.position());
        });
        context.getInput().holdKey(options -> options.keyShift);
        context.waitFor(client -> !CombatInput.state().climbing());
        context.getInput().releaseKey(options -> options.keyShift);
        server.runCommand("fill " + pillar + " minecraft:air");
        server.runCommand("tp @a " + (base.getX() + .5) + " " + base.getY() + " " + (base.getZ() + .5) + " 0 0");
        context.waitFor(client -> client.player.onGround() && CombatInput.state().stamina() == 100);
    }

    private static void spectatorExitResendsCameraYaw(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> ManagedCamera.setAngles(0, 0));
        context.waitTicks(3);
        server.runOnServer(minecraftServer -> {
            var player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (!CombatRuntime.get(minecraftServer).receiveCameraYaw(player, 0))
                throw new AssertionError("Precondition: playable owner accepts the initial yaw0 basis");
        });
        server.runCommand("gamemode spectator @a");
        context.waitFor(client -> client.player.isSpectator());
        context.runOnClient(client -> ManagedCamera.setAngles(90, 0));
        context.waitTicks(3);
        server.runOnServer(minecraftServer -> {
            var player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (CombatRuntime.get(minecraftServer).receiveCameraYaw(player, 90))
                throw new AssertionError("Spectator camera yaw must be rejected by the server");
        });
        server.runCommand("gamemode adventure @a");
        context.waitFor(client -> !client.player.isSpectator() && !client.player.getAbilities().flying);
        context.waitTicks(3);
        context.runOnClient(client -> {
            if (Math.abs(CameraMath.wrap(ManagedCamera.yaw() - 90)) > .1)
                throw new AssertionError("Leaving spectator must preserve the unmoved orbit yaw90");
        });
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(3);
        context.getInput().holdKey(options -> options.keySprint);
        server.waitFor(minecraftServer -> {
            var player = minecraftServer.getPlayerList().getPlayers().getFirst();
            return CombatRuntime.get(minecraftServer).session(player).stamina().dashing(
                    Frames.atServerTick(minecraftServer.getTickCount()));
        });
        server.runOnServer(minecraftServer -> {
            var motion = minecraftServer.getPlayerList().getPlayers().getFirst().getDeltaMovement();
            if (motion.x > -.5 || Math.abs(motion.z) > .01)
                throw new AssertionError("Spectator exit must resend unchanged yaw90: W dash must go -X, not stale yaw0 +Z: " + motion);
        });
        context.getInput().releaseKey(options -> options.keySprint);
        context.getInput().releaseKey(options -> options.keyUp);
        context.waitFor(client -> CombatInput.state().stamina() == 100 && !client.player.isSprinting());
    }

    private static void screenshot(ClientGameTestContext context, String name) {
        Path directory = context.computeOnClient(client -> client.gameDirectory.toPath().resolve("screenshots"));
        Path image = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(directory));
        if (!Files.isRegularFile(image)) throw new AssertionError("Client GameTest screenshot was not written: " + image);
        GenshinInMinecraft.LOGGER.info("HUD GameTest screenshot: {}", image.toAbsolutePath());
    }
}

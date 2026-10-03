package io.github.brainage04.genshininminecraft;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import net.minecraft.world.phys.AABB;
import io.github.brainage04.genshininminecraft.client.CombatFeedback;
import io.github.brainage04.genshininminecraft.client.GenshinHud;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
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
            server.runCommand("execute as @a at @s run genshin arena");
            context.waitFor(client -> CombatInput.managed());
            context.waitFor(client -> client.level.getBlockState(client.player.blockPosition().below()).is(Blocks.SMOOTH_STONE));
            server.runCommand("title @a clear");
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");
            context.getInput().lookAt(0, 0);
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
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            context.getInput().pressKey(GLFW.GLFW_KEY_Q);
            context.waitTicks(5);
            context.runOnClient(client -> {
                if (client.gui.screen() instanceof InventoryScreen || CombatInput.state().skillRemainingFrames() != 0
                        || CombatInput.state().burstRemainingFrames() != 0 || CombatInput.state().energy() != 0)
                    throw new AssertionError("Unavailable Amber E/Q must not open inventory, spend energy or start cooldowns");
                client.gui.hud.getChat().clearMessages(true);
            });
            screenshot(context, "genshin-party-four");
            context.waitTicks(22);
            // The previous Amber rejection hint must expire before capturing Kaeya's available talents.
            context.waitTicks(45);
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
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.waitTicks(10);
            context.waitFor(client -> {
                String particles = client.particleEngine.countParticles();
                return Integer.parseInt(particles.substring(particles.lastIndexOf(' ') + 1)) > 0;
            });
            screenshot(context, "genshin-kaeya-glacial-waltz");
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.getInput().pressKey(GLFW.GLFW_KEY_1);
            context.waitFor(client -> CombatInput.state().activeSlot() == 0);
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return !((io.github.brainage04.genshininminecraft.rules.kit.KaeyaKit)
                        CombatRuntime.get(minecraftServer).session(player).party().kit(2)).burstActive(
                                Frames.atServerTick(minecraftServer.getTickCount()));
            });
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
            context.getInput().lookAt(0, 0);
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
            context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
            screenshot(context, "genshin-hilichurl-camp");
            server.runCommand("gamemode adventure @a");
            context.waitFor(client -> !client.player.isCreative());
            server.runCommand("genshin managed off");
            context.waitFor(client -> !CombatInput.managed());
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
        } });
    }

    private static void screenshot(ClientGameTestContext context, String name) {
        Path directory = context.computeOnClient(client -> client.gameDirectory.toPath().resolve("screenshots"));
        Path image = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(directory));
        if (!Files.isRegularFile(image)) throw new AssertionError("Client GameTest screenshot was not written: " + image);
        GenshinInMinecraft.LOGGER.info("HUD GameTest screenshot: {}", image.toAbsolutePath());
    }
}

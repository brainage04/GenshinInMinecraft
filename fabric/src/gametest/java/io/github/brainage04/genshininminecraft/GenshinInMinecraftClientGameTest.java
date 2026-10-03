package io.github.brainage04.genshininminecraft;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.client.CombatFeedback;
import io.github.brainage04.genshininminecraft.client.GenshinHud;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
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
            server.runCommand("execute as @a at @s run genshin arena");
            context.waitFor(client -> CombatInput.managed());
            context.waitFor(client -> client.level.getBlockState(client.player.blockPosition().below()).is(Blocks.SMOOTH_STONE));
            server.runCommand("title @a clear");
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");
            context.getInput().lookAt(0, 0);
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
                if (GenshinHud.party().size() != 1 || !GenshinHud.party().getFirst().active()) {
                    throw new AssertionError("HUD must expose the active Traveler party row");
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
            server.runCommand("genshin managed off");
            context.waitFor(client -> !CombatInput.managed());
            context.runOnClient(client -> {
                if (!GenshinHud.party().isEmpty() || CombatFeedback.hasDamageNumber(mobId)) {
                    throw new AssertionError("Managed-off must clear HUD resources and world feedback");
                }
            });
            screenshot(context, "genshin-hud-vanilla");
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            context.waitForScreen(InventoryScreen.class);
        } finally {
            context.getInput().releaseKey(GLFW.GLFW_KEY_E);
        } });
    }

    private static void screenshot(ClientGameTestContext context, String name) {
        Path directory = context.computeOnClient(client -> client.gameDirectory.toPath().resolve("screenshots"));
        Path image = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(directory));
        if (!Files.isRegularFile(image)) throw new AssertionError("Client GameTest screenshot was not written: " + image);
        GenshinInMinecraft.LOGGER.info("HUD GameTest screenshot: {}", image.toAbsolutePath());
    }
}

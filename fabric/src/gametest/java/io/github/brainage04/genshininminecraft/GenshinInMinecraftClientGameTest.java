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
import io.github.brainage04.genshininminecraft.rules.kit.AmberKit;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
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
            screenshot(context, "genshin-party-four");
            float ordinaryFov = context.computeOnClient(client -> client.gameRenderer.mainCamera().getFov());
            context.getInput().holdKey(options -> options.keyAttack);
            context.waitFor(client -> CombatInput.aiming() && client.gameRenderer.mainCamera().getFov() < ordinaryFov * .85F);
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return ((AmberKit) CombatRuntime.get(minecraftServer).session(player).kit()).fullyCharged();
            });
            context.waitFor(client -> CombatInput.fullyChargedAim());
            screenshot(context, "genshin-amber-aim");
            context.getInput().releaseKey(options -> options.keyAttack);
            context.waitFor(client -> !CombatInput.aiming() && client.gameRenderer.mainCamera().getFov() > ordinaryFov * .9F);
            context.waitTicks(5);
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return !player.level().getEntitiesOfClass(Rabbit.class, new AABB(player.position(), player.position()).inflate(8)).isEmpty();
            });
            context.waitFor(client -> {
                for (var entity : client.level.entitiesForRendering())
                    if (entity instanceof Rabbit && entity.getCustomName() != null
                            && entity.getCustomName().getString().equals("Baron Bunny")) return true;
                return false;
            });
            int bunnyId = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return player.level().getEntitiesOfClass(Rabbit.class,
                        new AABB(player.position(), player.position()).inflate(8)).getFirst().getId();
            });
            context.waitFor(client -> CombatInput.state().skillRemainingFrames() > 0);
            context.getInput().lookAt(0, 12);
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
                var bunny = (Rabbit) player.level().getEntity(bunnyId);
                if (bunny.getHealth() != bunny.getMaxHealth()
                        || ((AmberKit) CombatRuntime.get(minecraftServer).session(player).kit()).puppetHp() != 2037.88 * .4136)
                    throw new AssertionError("Fiery Rain must leave the source-scaled Bunny HP full");
                player.level().getEntity(pyroMobId).discard();
            });
            context.getInput().lookAt(0, 0);
            context.waitTicks(40);
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var bunny = (Rabbit) player.level().getEntity(bunnyId);
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
            context.getInput().pressKey(GLFW.GLFW_KEY_4);
            context.waitFor(client -> CombatInput.state().activeSlot() == 3);
            int electroMobId = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var mob = new Hilichurl(GenshinEntities.HILICHURL, player.level());
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
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            server.runCommand("execute as @a at @s run tp @s ~-2 ~ ~ 0 0");
            context.getInput().lookAt(-15, 15);
            context.waitFor(client -> (CombatFeedback.auraElements(electroMobId) & (1 << Element.HYDRO.ordinal())) != 0
                    && (CombatFeedback.auraElements(electroMobId) & (1 << Element.ELECTRO.ordinal())) != 0
                    && CombatFeedback.hasDamageNumber(electroMobId) && CombatFeedback.conductiveStacks(electroMobId) == 1);
            context.waitFor(client -> {
                String particles = client.particleEngine.countParticles();
                return Integer.parseInt(particles.substring(particles.lastIndexOf(' ') + 1)) > 0;
            });
            context.runOnClient(client -> client.gui.hud.getChat().clearMessages(true));
            screenshot(context, "genshin-lisa-rose-electro-charged");
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.getInput().lookAt(0, 0);
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
            context.getInput().releaseKey(options -> options.keyAttack);
        } });
    }

    private static void screenshot(ClientGameTestContext context, String name) {
        Path directory = context.computeOnClient(client -> client.gameDirectory.toPath().resolve("screenshots"));
        Path image = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(directory));
        if (!Files.isRegularFile(image)) throw new AssertionError("Client GameTest screenshot was not written: " + image);
        GenshinInMinecraft.LOGGER.info("HUD GameTest screenshot: {}", image.toAbsolutePath());
    }
}

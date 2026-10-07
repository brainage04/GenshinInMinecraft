package io.github.brainage04.genshininminecraft;

import com.geckolib.constant.DataTickets;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import io.github.brainage04.genshininminecraft.client.enemy.BunnyVisuals;
import io.github.brainage04.genshininminecraft.client.enemy.EnemyGeoRenderState;
import io.github.brainage04.genshininminecraft.client.enemy.EnemyRenderInput;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.BaronBunny;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.EnemyAnimations.Phase;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import java.nio.file.Files;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.level.GameType;

/** Real AI and kit transitions, with time frozen only to inspect the committed server pose. */
@SuppressWarnings("UnstableApiUsage")
final class EnemyRenderingChecks {
    static void enemies(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(EnemyRenderingChecks::bakedSeekingFixture);
        int fov = context.computeOnClient(client -> client.options.fov().get());
        int enemyId = -1;
        try {
            server.runCommand("tick rate 2");
            context.runOnClient(client -> { ManagedCamera.setAngles(150, 10); client.options.fov().set(55); });
            context.getInput().pressKey(options -> options.keyToggleGui);
            context.waitFor(client -> client.gui.hud.isHidden());
            enemyId = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                var enemy = new Hilichurl(GenshinEntities.HILICHURL, player.level());
                enemy.snapTo(player.position().add(-1.25, 0, -1.2));
                enemy.setCamp(enemy.position(), enemy.position());
                if (!player.level().addFreshEntity(enemy)) throw new AssertionError("Enemy capture spawn rejected");
                return enemy.getId();
            });
            final int id = enemyId;
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var enemy = (Hilichurl) player.level().getEntity(id);
                if (enemy.visualPhase() != Phase.TELEGRAPH || player.level().getGameTime() * 3 - enemy.visualStartFrame() < 12) return false;
                minecraftServer.tickRateManager().setFrozen(true);
                player.connection.send(new ClientboundSetTimePacket(player.level().getGameTime(), java.util.Map.of()));
                return true;
            });
            context.waitFor(client -> client.level.getEntity(id) instanceof Hilichurl enemy
                    && enemy.visualPhase() == Phase.TELEGRAPH && client.level.tickRateManager().isFrozen());
            context.runOnClient(client -> {
                var enemy = (Hilichurl) client.level.getEntity(id);
                var state = (EnemyGeoRenderState) client.getEntityRenderDispatcher().getRenderer(enemy).createRenderState(enemy, 0);
                var input = state.getGeckolibData(EnemyRenderInput.TICKET);
                if (input.phase() != Phase.TELEGRAPH || input.seconds() < .15 || input.seconds() >= .5)
                    throw new AssertionError("Actual AI telegraph must seek its raised-club windup: " + input);
                assertSeek(state, input.phase().seconds(input.seconds()));
                client.particleEngine.clearParticles();
            });
            screenshot(context, "genshin-hilichurl-telegraph");
            server.runCommand("tick unfreeze");
            server.waitFor(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var enemy = (Hilichurl) player.level().getEntity(id);
                if (enemy.visualPhase() != Phase.STRIKE) return false;
                minecraftServer.tickRateManager().setFrozen(true);
                // Seek precisely to the first committed impact pose, not the recovery screenshot.
                player.connection.send(new ClientboundSetTimePacket(enemy.visualStartFrame() / 3, java.util.Map.of()));
                return true;
            });
            context.waitFor(client -> client.level.getEntity(id) instanceof Hilichurl enemy
                    && enemy.visualPhase() == Phase.STRIKE && client.level.tickRateManager().isFrozen()
                    && client.level.getGameTime() * 3 == enemy.visualStartFrame());
            context.runOnClient(client -> {
                var enemy = (Hilichurl) client.level.getEntity(id);
                var state = (EnemyGeoRenderState) client.getEntityRenderDispatcher().getRenderer(enemy).createRenderState(enemy, 0);
                var input = state.getGeckolibData(EnemyRenderInput.TICKET);
                if (input.phase() != Phase.STRIKE || input.seconds() != 0) throw new AssertionError("Impact pose must begin at actual strike tick");
                assertSeek(state, 0); client.particleEngine.clearParticles();
            });
            screenshot(context, "genshin-hilichurl-strike");
            server.runOnServer(minecraftServer -> minecraftServer.getPlayerList().getPlayers().getFirst().level().getEntity(id).discard());
            enemyId = -1;
            server.runCommand("tick unfreeze"); server.runCommand("tick rate 20");
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var session = CombatRuntime.get(minecraftServer).session(player);
                if (!session.intent(Intent.SWITCH_2, Frames.atServerTick(minecraftServer.getTickCount()))) throw new AssertionError("Bunny capture switch rejected");
            });
            context.waitFor(client -> CombatInput.state().activeSlot() == 1);
            context.waitTicks(20);
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                if (!CombatRuntime.get(minecraftServer).session(player).intent(Intent.SKILL_PRESS, Frames.atServerTick(minecraftServer.getTickCount())))
                    throw new AssertionError("Bunny capture skill rejected");
            });
            context.waitFor(client -> BunnyVisuals.activePresentations() > 0);
            int bunnyId = server.computeOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                return player.level().getEntitiesOfClass(BaronBunny.class, player.getBoundingBox().inflate(8)).stream()
                        .findFirst().map(BaronBunny::getId).orElse(-1);
            });
            if (bunnyId < 0) {
                server.waitFor(minecraftServer -> {
                    var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    return !player.level().getEntitiesOfClass(BaronBunny.class, player.getBoundingBox().inflate(8)).isEmpty();
                });
                bunnyId = server.computeOnServer(minecraftServer -> {
                    var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    return player.level().getEntitiesOfClass(BaronBunny.class, player.getBoundingBox().inflate(8)).getFirst().getId();
                });
            }
            final int bunny = bunnyId;
            context.waitFor(client -> client.level.getEntity(bunny) instanceof BaronBunny && BunnyVisuals.activePresentations() == 0);
            context.waitTicks(6);
            context.runOnClient(client -> {
                var entity = (BaronBunny) client.level.getEntity(bunny);
                var state = (EnemyGeoRenderState) client.getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 0);
                if (state.getGeckolibData(EnemyRenderInput.TICKET).phase() != Phase.IDLE) throw new AssertionError("Landed doll must idle wobble after squash/land");
                ManagedCamera.setAngles(entity.getYRot() + 100, 10); client.options.fov().set(75);
                client.particleEngine.clearParticles();
            });
            screenshot(context, "genshin-baron-bunny-landed");
            server.runOnServer(minecraftServer -> {
                var player = minecraftServer.getPlayerList().getPlayers().getFirst();
                player.level().getEntity(bunny).hurtServer(player.level(), player.level().damageSources().genericKill(), Float.MAX_VALUE);
            });
            context.waitFor(client -> BunnyVisuals.activePresentations() > 0 && client.level.getEntity(bunny) == null);
            screenshot(context, "genshin-baron-bunny-explode");
            context.waitFor(client -> BunnyVisuals.activePresentations() == 0);
        } finally {
            if (enemyId >= 0) { final int id = enemyId; server.runOnServer(s -> { var entity = s.getPlayerList().getPlayers().getFirst().level().getEntity(id); if (entity != null) entity.discard(); }); }
            server.runCommand("tick unfreeze"); server.runCommand("tick rate 20");
            if (context.computeOnClient(client -> client.gui.hud.isHidden())) context.getInput().pressKey(options -> options.keyToggleGui);
            server.runCommand("genshin managed off"); context.waitFor(client -> !CombatInput.managed());
            PersistenceChecks.resetFixture(server);
            server.runCommand("genshin managed on"); context.waitFor(client -> CombatInput.managed() && CombatInput.state().activeSlot() == 0);
            context.runOnClient(client -> { client.options.fov().set(fov); ManagedCamera.setAngles(0, 0); });
        }
    }
    private static void assertSeek(EnemyGeoRenderState state, double seconds) {
        var animation = state.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES)[0].animationPoint();
        if (animation == null || Math.abs(animation.animTime() - seconds) > .000001) throw new AssertionError("Enemy controller did not seek server clock");
    }
    private static void bakedSeekingFixture(net.minecraft.client.Minecraft client) {
        var enemy = new Hilichurl(GenshinEntities.HILICHURL, client.level);
        enemy.setId(2000001);
        var renderer = (io.github.brainage04.genshininminecraft.client.HilichurlRenderer)
                client.getEntityRenderDispatcher().getRenderer(enemy);
        var model = renderer.getGeoModel();
        var state = renderer.createRenderState(enemy, 0);
        if (model.getBakedModel(model.getModelResource(state)).isMissingno()) throw new AssertionError("Hilichurl model failed to bake");
        for (String bone : new String[]{"mask", "club", "loincloth", "right_forearm"})
            if (model.getBakedModel(model.getModelResource(state)).getBone(bone).isEmpty()) throw new AssertionError("Enemy silhouette missing " + bone);
        for (var phase : new Phase[]{Phase.IDLE, Phase.WALK, Phase.RUN, Phase.TELEGRAPH, Phase.STRIKE, Phase.RECOVERY, Phase.HURT, Phase.DEATH})
            if (model.getBakedAnimation(enemy, phase.clip()) == null) throw new AssertionError("Enemy clip failed to bake: " + phase);
        var late = new EnemyRenderInput(Phase.TELEGRAPH, 5, .35, 0, 1);
        state.addGeckolibData(EnemyRenderInput.TICKET, late);
        com.geckolib.animation.AnimationProcessor.extractControllerStates(enemy, state, model);
        assertSeek(state, .35);
        var immutable = state.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES)[0];
        com.geckolib.animation.AnimationProcessor.extractControllerStates(enemy, state, model);
        assertSeek(state, .35); // Frozen/duplicate extraction cannot advance the pose.
        state.addGeckolibData(EnemyRenderInput.TICKET, new EnemyRenderInput(Phase.TELEGRAPH, 6, .2, 0, 1));
        com.geckolib.animation.AnimationProcessor.extractControllerStates(enemy, state, model);
        assertSeek(state, .2);
        if (Math.abs(immutable.animationPoint().animTime() - .35) > .000001)
            throw new AssertionError("New enemy occurrence mutated an extracted snapshot");
        var bunny = new BaronBunny(GenshinEntities.BARON_BUNNY, client.level);
        bunny.setId(2000002);
        var bunnyRenderer = (io.github.brainage04.genshininminecraft.client.BaronBunnyRenderer)
                client.getEntityRenderDispatcher().getRenderer(bunny);
        var bunnyState = bunnyRenderer.createRenderState(bunny, 0);
        var bunnyModel = bunnyRenderer.getGeoModel();
        if (bunnyModel.getBakedModel(bunnyModel.getModelResource(bunnyState)).isMissingno()) throw new AssertionError("Plush Bunny model failed to bake");
        for (var phase : new Phase[]{Phase.IDLE, Phase.WALK, Phase.THROWN, Phase.LAND, Phase.HURT, Phase.EXPLODE})
            if (bunnyModel.getBakedAnimation(bunny, phase.clip()) == null) throw new AssertionError("Bunny clip failed to bake: " + phase);
        if (!state.hasGeckolibData(DataTickets.ANIMATABLE_MANAGER) || !bunnyState.hasGeckolibData(DataTickets.ANIMATABLE_MANAGER))
            throw new AssertionError("Explicit geo state must share inherited ticket writers/readers");
    }
    private static void screenshot(ClientGameTestContext context, String name) {
        var directory = context.computeOnClient(client -> client.gameDirectory.toPath().resolve("screenshots"));
        var path = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(directory));
        if (!Files.isRegularFile(path)) throw new AssertionError("Enemy screenshot missing: " + path);
        GenshinInMinecraft.LOGGER.info("Enemy GameTest screenshot: {}", path.toAbsolutePath());
    }
}

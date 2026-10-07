package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import io.github.brainage04.genshininminecraft.client.ProjectileVisuals;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.network.ProjectileVisualPayload.Kind;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ParticleStatus;

/** Captures real server-accepted shots/fields, never injected mesh packets; minimal particles prove geometry. */
@SuppressWarnings("UnstableApiUsage")
final class ProjectileRenderingChecks {
    private record Scene(int slot, Intent intent, Kind mesh, int sample, int heldFrames, double distance, String name) {}
    private static final Scene[] SCENES = {
        new Scene(1, Intent.ATTACK_PRESS, Kind.ARROW, 23, 0, 10, "amber-normal"),
        new Scene(1, Intent.ATTACK_PRESS, Kind.ARROW, 9, 18, 10, "amber-aimed"),
        new Scene(1, Intent.ATTACK_PRESS, Kind.PYRO_ARROW, 9, 90, 10, "amber-charged-mid-flight"),
        new Scene(1, Intent.BURST_PRESS, Kind.RAIN_ARROW, 69, 0, 3, "fiery-rain"),
        new Scene(3, Intent.ATTACK_PRESS, Kind.CATALYST_BOLT, 32, 0, 5, "lisa-normal"),
        new Scene(3, Intent.ATTACK_PRESS, Kind.CHARGED_BOLT, 95, -1, 3, "lisa-charged"),
        new Scene(3, Intent.SKILL_PRESS, Kind.VIOLET_ORB, 33, 0, 10, "lisa-violet-arc"),
        new Scene(3, Intent.BURST_PRESS, Kind.ROSE_BOLT, 125, 0, 5, "lightning-rose-bolt"),
        new Scene(0, Intent.SKILL_PRESS, Kind.PALM_VORTEX, 35, 0, 3, "palm-vortex"),
        new Scene(0, Intent.BURST_PRESS, Kind.TORNADO, 36, 0, 6, "gust-surge"),
        new Scene(2, Intent.SKILL_PRESS, Kind.FROST_ICICLE, 34, 0, 5, "frostgnaw"),
        new Scene(2, Intent.BURST_PRESS, Kind.WALTZ_ICICLE, 64, 0, 5, "glacial-waltz")
    };
    static void projectiles(ClientGameTestContext context, TestDedicatedServerContext server) {
        int fov = context.computeOnClient(client -> client.options.fov().get());
        var particles = context.computeOnClient(client -> client.options.particles().get());
        int target = -1;
        try {
            context.getInput().pressKey(options -> options.keyToggleGui);
            context.waitFor(client -> client.gui.hud.isHidden());
            for (Scene scene : SCENES) {
                server.runCommand("tick unfreeze"); server.runCommand("tick rate 20");
                server.runCommand("genshin managed off"); context.waitFor(client -> !CombatInput.managed());
                context.runOnClient(client -> {
                    ProjectileVisuals.extract(0);
                    if (!ProjectileVisuals.renderStates().isEmpty()) throw new AssertionError("Unmanage must clear projectiles");
                });
                if (target >= 0) { final int old = target; server.runOnServer(s -> { var entity = s.getPlayerList().getPlayers().getFirst().level().getEntity(old); if (entity != null) entity.discard(); }); }
                server.runCommand("genshin managed on"); context.waitFor(client -> CombatInput.managed());
                target = server.computeOnServer(s -> {
                    var player = s.getPlayerList().getPlayers().getFirst();
                    player.setYRot(0); player.setXRot(0); player.setYBodyRot(0); player.setYHeadRot(0);
                    var enemy = new Hilichurl(GenshinEntities.HILICHURL, player.level());
                    enemy.setGenshinLevel(20); enemy.setNoAi(true);
                    enemy.snapTo(player.position().add(0, 0, scene.distance())); enemy.setCamp(enemy.position(), enemy.position());
                    if (!player.level().addFreshEntity(enemy)) throw new AssertionError("Projectile target spawn rejected");
                    var session = CombatRuntime.get(s).session(player);
                    if (scene.slot() != 0 && !session.intent(Intent.values()[Intent.SWITCH_1.ordinal() + scene.slot()], Frames.atServerTick(s.getTickCount())))
                        throw new AssertionError("Projectile capture switch rejected");
                    return enemy.getId();
                });
                final int id = target;
                context.waitFor(client -> client.level.getEntity(id) != null && CombatInput.state().activeSlot() == scene.slot());
                context.waitTicks(20);
                context.runOnClient(client -> {
                    client.player.setYRot(0); client.player.setXRot(0); client.player.setYBodyRot(0);
                    ManagedCamera.setAngles(-20, 10); client.options.fov().set(70);
                    client.options.particles().set(ParticleStatus.MINIMAL); client.particleEngine.clearParticles();
                });
                long start = server.computeOnServer(s -> {
                    var player = s.getPlayerList().getPlayers().getFirst();
                    player.setYRot(0); player.setXRot(0);
                    var session = CombatRuntime.get(s).session(player);
                    long frame = Frames.atServerTick(s.getTickCount());
                    if (scene.intent() == Intent.BURST_PRESS) session.kit().grantEnergy(80);
                    if (!session.intent(scene.intent(), frame)) throw new AssertionError("Projectile action rejected " + scene.name());
                    if (scene.heldFrames() == 0 && scene.intent() == Intent.ATTACK_PRESS) session.intent(Intent.ATTACK_RELEASE, frame);
                    if (scene.intent() == Intent.SKILL_PRESS && (scene.slot() == 0 || scene.slot() == 3)) session.intent(Intent.SKILL_RELEASE, frame);
                    return frame;
                });
                if (scene.heldFrames() > 0) {
                    server.waitFor(s -> Frames.atServerTick(s.getTickCount()) >= start + scene.heldFrames());
                    long release = server.computeOnServer(s -> {
                        long frame = Frames.atServerTick(s.getTickCount());
                        if (!CombatRuntime.get(s).session(s.getPlayerList().getPlayers().getFirst()).intent(Intent.ATTACK_RELEASE, frame))
                            throw new AssertionError("Aimed release rejected");
                        return frame;
                    });
                    freeze(server, release + scene.sample());
                } else freeze(server, start + scene.sample());
                server.waitFor(s -> s.tickRateManager().isFrozen());
                long worldTime = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().level().getGameTime());
                context.waitFor(client -> client.level.tickRateManager().isFrozen() && client.level.getGameTime() == worldTime && ProjectileVisuals.has(scene.mesh()));
                context.runOnClient(client -> {
                    ProjectileVisuals.extract(0);
                    var states = ProjectileVisuals.renderStates();
                    var mesh = states.stream().filter(state -> state.kind() == scene.mesh()).findFirst().orElseThrow();
                    if (mesh.progress() <= 0 || mesh.progress() >= 1) throw new AssertionError("Must capture an actual in-flight/live mesh " + mesh);
                    if (scene.mesh() == Kind.RAIN_ARROW && states.stream().filter(state -> state.kind() == Kind.RAIN_ARROW
                            && state.progress() > 0 && state.progress() < 1).count() != 12)
                        throw new AssertionError("Rain capture must contain six falling arrows from each of two real waves");
                    if (scene.mesh() == Kind.WALTZ_ICICLE && states.stream().filter(state -> state.kind() == Kind.WALTZ_ICICLE).count() != 3)
                        throw new AssertionError("Waltz must have all three rendered icicles");
                    ProjectileVisuals.extract(.9F);
                    if (!states.equals(ProjectileVisuals.renderStates())) throw new AssertionError("Frozen repeated extraction must be immutable");
                    client.particleEngine.clearParticles();
                });
                if (scene.mesh() == Kind.PYRO_ARROW) server.runOnServer(s -> {
                    var player = s.getPlayerList().getPlayers().getFirst();
                    var enemy = (Hilichurl) player.level().getEntity(id);
                    if (CombatRuntime.get(s).target(enemy).hp() >= CombatRuntime.get(s).target(enemy).maxHp())
                        throw new AssertionError("Visual-only arrow must preserve authoritative release-time damage");
                });
                screenshot(context, "genshin-projectile-" + scene.name());
                if (scene.mesh() == Kind.WALTZ_ICICLE || scene.mesh() == Kind.TORNADO) {
                    server.runOnServer(s -> {
                        if (!CombatRuntime.get(s).handleDeath(s.getPlayerList().getPlayers().getFirst()))
                            throw new AssertionError("Field-owner fall must force a living replacement");
                    });
                    context.waitFor(client -> !ProjectileVisuals.has(scene.mesh()));
                }
            }
        } finally {
            server.runCommand("tick unfreeze"); server.runCommand("tick rate 20");
            if (target >= 0) { final int id = target; server.runOnServer(s -> { var entity = s.getPlayerList().getPlayers().getFirst().level().getEntity(id); if (entity != null) entity.discard(); }); }
            if (context.computeOnClient(client -> client.gui.hud.isHidden())) context.getInput().pressKey(options -> options.keyToggleGui);
            server.runCommand("genshin managed off"); context.waitFor(client -> !CombatInput.managed());
            server.runCommand("genshin managed on"); context.waitFor(client -> CombatInput.managed() && CombatInput.state().activeSlot() == 0);
            context.runOnClient(client -> { client.options.fov().set(fov); client.options.particles().set(particles); ManagedCamera.setAngles(0, 0); });
        }
    }
    private static void freeze(TestDedicatedServerContext server, long frame) {
        long aligned = (frame + 2) / 3 * 3;
        server.runOnServer(s -> timeline(s).schedule(aligned, at -> {
            s.tickRateManager().setTickRate(1); s.tickRateManager().setFrozen(true);
            var player = s.getPlayerList().getPlayers().getFirst();
            player.connection.send(new ClientboundSetTimePacket(player.level().getGameTime(), java.util.Map.of()));
        }));
    }
    private static EventTimeline timeline(MinecraftServer server) {
        try {
            var field = CombatRuntime.class.getDeclaredField("timeline"); field.setAccessible(true);
            return (EventTimeline) field.get(CombatRuntime.get(server));
        } catch (ReflectiveOperationException ex) { throw new AssertionError("Cannot observe test-only timeline", ex); }
    }
    private static void screenshot(ClientGameTestContext context, String name) {
        context.waitTicks(2);
        var directory = context.computeOnClient(client -> client.gameDirectory.toPath().resolve("screenshots"));
        var path = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(directory));
        if (!Files.isRegularFile(path)) throw new AssertionError("Missing projectile screenshot " + path);
        GenshinInMinecraft.LOGGER.info("Projectile GameTest screenshot: {}", path.toAbsolutePath());
    }
}

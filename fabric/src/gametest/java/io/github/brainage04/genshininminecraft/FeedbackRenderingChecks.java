package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.client.CombatFeedback;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.GenshinHud;
import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.Reaction;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.ui.DamageNumberAnimation;
import io.github.brainage04.genshininminecraft.ui.ElementPalette;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.server.level.ParticleStatus;

/** Real kit damage/crit/reaction and switch packets; no injected damage-number payloads. */
@SuppressWarnings("UnstableApiUsage")
final class FeedbackRenderingChecks {
    private record Scene(int slot, Intent intent, int hitFrame, Element aura, Reaction.Type reaction, String name) {}
    private static final Scene[] SCENES = {
        new Scene(0, Intent.ATTACK_PRESS, 13, null, null, "crit"),
        new Scene(0, Intent.SKILL_PRESS, 32, Element.PYRO, Reaction.Type.SWIRL, "swirl"),
        new Scene(2, Intent.SKILL_PRESS, 28, Element.PYRO, Reaction.Type.MELT, "melt")
    };
    static void feedback(ClientGameTestContext context, TestDedicatedServerContext server) {
        var particles = context.computeOnClient(client -> client.options.particles().get());
        int target = -1;
        try {
            for (Scene scene : SCENES) {
                server.runCommand("tick unfreeze"); server.runCommand("tick rate 20");
                server.runCommand("genshin managed off"); context.waitFor(client -> !CombatInput.managed());
                context.runOnClient(client -> {
                    if (!CombatFeedback.damageNumbers().isEmpty()) throw new AssertionError("Unmanage must clear combat text");
                });
                if (target >= 0) discard(server, target);
                PersistenceChecks.resetFixture(server);
                server.runCommand("genshin managed on"); context.waitFor(client -> CombatInput.managed());
                target = server.computeOnServer(s -> {
                    var player = s.getPlayerList().getPlayers().getFirst();
                    player.setYRot(0); player.setXRot(0); player.setYBodyRot(0); player.setYHeadRot(0);
                    var enemy = new Hilichurl(GenshinEntities.HILICHURL, player.level());
                    enemy.setGenshinLevel(20); enemy.setNoAi(true);
                    enemy.snapTo(player.position().add(0, 0, 2.5)); enemy.setCamp(enemy.position(), enemy.position());
                    if (!player.level().addFreshEntity(enemy)) throw new AssertionError("Feedback target spawn rejected");
                    var session = CombatRuntime.get(s).session(player);
                    if (scene.slot() != 0 && !session.intent(Intent.values()[Intent.SWITCH_1.ordinal() + scene.slot()], Frames.atServerTick(s.getTickCount())))
                        throw new AssertionError("Feedback switch rejected");
                    return enemy.getId();
                });
                final int id = target;
                context.waitFor(client -> client.level.getEntity(id) != null && CombatInput.state().activeSlot() == scene.slot());
                context.waitTicks(22);
                context.runOnClient(client -> {
                    client.player.setYRot(0); client.player.setXRot(0); client.player.setYBodyRot(0);
                    ManagedCamera.setAngles(0, 10);
                    client.options.particles().set(ParticleStatus.MINIMAL);
                    client.particleEngine.clearParticles(); client.gui.hud.getChat().clearMessages(true);
                });
                long start = server.computeOnServer(s -> {
                    var player = s.getPlayerList().getPlayers().getFirst();
                    var runtime = CombatRuntime.get(s);
                    var session = runtime.session(player);
                    long frame = Frames.atServerTick(s.getTickCount());
                    if (scene.aura() != null) runtime.target((Hilichurl) player.level().getEntity(id)).aura().applyHit(scene.aura(), 2, frame);
                    // Stable fixture for the existing authoritative crit roll; no CRIT stat or hit override.
                    try {
                        var field = session.getClass().getDeclaredField("random"); field.setAccessible(true);
                        ((Random) field.get(session)).setSeed(4096);
                    } catch (ReflectiveOperationException ex) { throw new AssertionError("Cannot seed crit fixture", ex); }
                    if (!session.intent(scene.intent(), frame)) throw new AssertionError("Feedback action rejected");
                    session.intent(scene.intent() == Intent.ATTACK_PRESS ? Intent.ATTACK_RELEASE : Intent.SKILL_RELEASE, frame);
                    return frame;
                });
                ProjectileRenderingChecks.freeze(server, start + scene.hitFrame() + 6);
                server.waitFor(s -> s.tickRateManager().isFrozen());
                long worldTime = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().level().getGameTime());
                context.waitFor(client -> client.level.tickRateManager().isFrozen() && client.level.getGameTime() == worldTime && CombatFeedback.hasDamageNumber(id));
                // Packets can arrive on the freeze tick. Sample after three real world ticks
                // from receipt, never mistake the initial .6 pop scale for settled crit styling.
                server.runCommand("tick step 3");
                server.waitFor(s -> s.getPlayerList().getPlayers().getFirst().level().getGameTime() >= worldTime + 3);
                long sampledTime = server.computeOnServer(s -> {
                    var player = s.getPlayerList().getPlayers().getFirst();
                    long time = player.level().getGameTime();
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTimePacket(time, java.util.Map.of()));
                    return time;
                });
                context.waitFor(client -> client.level.getGameTime() == sampledTime);
                context.runOnClient(client -> {
                    var numbers = CombatFeedback.damageNumbers().stream().filter(number -> number.targetId() == id).toList();
                    var texts = CombatFeedback.extract(0);
                    if (scene.reaction() == null) {
                        var number = numbers.stream().filter(CombatFeedback.NumberEntry::critical).findFirst()
                                .orElseThrow(() -> new AssertionError("Actual seeded hit must crit"));
                        var amount = texts.stream().filter(text -> text.text() == number.amount()).findFirst().orElseThrow();
                        if (amount.emphasis() < (float) DamageNumberAnimation.CRITICAL_SCALE || amount.color() != ElementPalette.color(Element.PHYSICAL))
                            throw new AssertionError("Crit must be larger, bold, white Physical text: emphasis=" + amount.emphasis()
                                    + ", color=" + Integer.toHexString(amount.color()) + ", ageTicks=" + (sampledTime - number.bornTick()));
                    } else {
                        var number = numbers.stream().filter(entry -> entry.reaction() != null).findFirst().orElseThrow();
                        if (number.amount() == null || number.reactionColor() != ElementPalette.reactionColor(scene.reaction()))
                            throw new AssertionError("Reaction must retain its real damage and independent label colour");
                        var amount = texts.stream().filter(text -> text.text() == number.amount()).findFirst().orElseThrow();
                        var label = texts.stream().filter(text -> text.text() == number.reaction()).findFirst().orElseThrow();
                        if (label.pixelX() != amount.pixelX() || Math.abs(label.pixelY() - amount.pixelY() - DamageNumberAnimation.REACTION_Y) > .0001
                                || !label.position().equals(amount.position())) throw new AssertionError("Reaction label must sit above its own number");
                        if (numbers.stream().map(CombatFeedback.NumberEntry::slot).distinct().count() != numbers.size())
                            throw new AssertionError("Simultaneous talent/reaction hits must not share a stack slot");
                    }
                    if (!texts.equals(CombatFeedback.extract(.9F))) throw new AssertionError("Frozen combat text must not advance on extraction");
                    client.particleEngine.clearParticles();
                });
                screenshot(context, "genshin-damage-" + scene.name());
            }
            server.runCommand("tick unfreeze"); server.runCommand("tick rate 20");
            context.waitTicks(24);
            context.runOnClient(client -> {
                if (!CombatFeedback.damageNumbers().isEmpty()) throw new AssertionError("Expired numbers must retire");
            });
            // Public bubble-hit evidence shows incoming numbers in the damage's element, not red.
            final int id = target;
            float hp = context.computeOnClient(client -> CombatInput.state().hp());
            server.runOnServer(s -> {
                var player = s.getPlayerList().getPlayers().getFirst();
                if (!CombatRuntime.get(s).enemyHit(player, (Hilichurl) player.level().getEntity(id), 120, 1, 20))
                    throw new AssertionError("Incoming feedback fixture must be an accepted hit");
            });
            context.waitFor(client -> CombatInput.state().hp() < hp && CombatFeedback.hasDamageNumber(client.player.getId()));
            context.runOnClient(client -> {
                var incoming = CombatFeedback.damageNumbers().stream().filter(number -> number.targetId() == client.player.getId())
                        .findFirst().orElseThrow();
                if (incoming.color() != ElementPalette.color(Element.PHYSICAL) || incoming.critical() || incoming.amount() == null)
                    throw new AssertionError("Incoming club damage must have an ordinary white Physical number, not red");
            });
            server.runCommand("genshin managed off"); context.waitFor(client -> !CombatInput.managed());
            PersistenceChecks.resetFixture(server);
            server.runCommand("genshin managed on"); context.waitFor(client -> CombatInput.managed());
            long switchFrame = server.computeOnServer(s -> {
                var session = CombatRuntime.get(s).session(s.getPlayerList().getPlayers().getFirst());
                session.kit().setHp(session.kit().maxHp() * .5);
                long frame = Frames.atServerTick(s.getTickCount());
                if (!session.intent(Intent.SWITCH_2, frame)) throw new AssertionError("HP-readability switch rejected");
                return frame;
            });
            ProjectileRenderingChecks.freeze(server, switchFrame + 3);
            context.waitFor(client -> client.level.tickRateManager().isFrozen() && CombatInput.state().activeSlot() == 1 && GenshinHud.partyUnavailable(0));
            Path path = screenshot(context, "genshin-switch-cooldown-readable-hp");
            assertGreenHp(context, path);
        } finally {
            server.runCommand("tick unfreeze"); server.runCommand("tick rate 20");
            if (target >= 0) discard(server, target);
            server.runCommand("genshin managed off"); context.waitFor(client -> !CombatInput.managed());
            PersistenceChecks.resetFixture(server);
            server.runCommand("genshin managed on"); context.waitFor(client -> CombatInput.managed() && CombatInput.state().activeSlot() == 0);
            context.runOnClient(client -> { client.options.particles().set(particles); ManagedCamera.setAngles(0, 0); });
        }
    }
    private static void assertGreenHp(ClientGameTestContext context, Path path) {
        int[] dimensions = context.computeOnClient(client -> new int[] {client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight()});
        try {
            var image = ImageIO.read(path.toFile());
            int x = (int) ((dimensions[0] - 122 + 7 + 10) * image.getWidth() / (double) dimensions[0]);
            int y = (int) ((18 + 16 + 2) * image.getHeight() / (double) dimensions[1]);
            int color = image.getRGB(x, y), red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
            if (green < 140 || green - red < 25 || green - blue < 30)
                throw new AssertionError("Half-HP inactive row must remain readable green under cooldown: " + Integer.toHexString(color));
        } catch (java.io.IOException ex) { throw new AssertionError("Cannot inspect HP screenshot", ex); }
    }
    private static void discard(TestDedicatedServerContext server, int id) {
        server.runOnServer(s -> { var entity = s.getPlayerList().getPlayers().getFirst().level().getEntity(id); if (entity != null) entity.discard(); });
    }
    private static Path screenshot(ClientGameTestContext context, String name) {
        context.waitTicks(2);
        var directory = context.computeOnClient(client -> client.gameDirectory.toPath().resolve("screenshots"));
        var path = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(directory));
        if (!Files.isRegularFile(path)) throw new AssertionError("Missing feedback screenshot " + path);
        GenshinInMinecraft.LOGGER.info("Feedback GameTest screenshot: {}", path.toAbsolutePath());
        return path;
    }
}

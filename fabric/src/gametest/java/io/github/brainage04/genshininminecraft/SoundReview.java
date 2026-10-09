package io.github.brainage04.genshininminecraft;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import io.github.brainage04.genshininminecraft.client.TeleportScreen;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.*;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.world.OverlayDefinition;
import io.github.brainage04.genshininminecraft.world.OverlayRuntime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Opt-in recording-only sequence: real server transitions and SoundEngine listener receipts. */
@SuppressWarnings("UnstableApiUsage")
final class SoundReview {
    private static final Path ROOT = Path.of(System.getenv().getOrDefault("GENSHIN_SOUND_REVIEW", "."));
    private static volatile String cue = "setup";
    private static Vec3 origin;
    private static int target = -1;
    private SoundReview() {}
    private static synchronized void log(String kind, String text) {
        String line = System.currentTimeMillis() + "\t" + kind + "\t" + cue + "\t" + text + "\n";
        try { Files.writeString(ROOT.resolve("events.tsv"), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (java.io.IOException e) { throw new AssertionError(e); }
        GenshinInMinecraft.LOGGER.info("[SOUND_REVIEW] {}", line.strip());
    }
    private static void mark(ClientGameTestContext c, String text) {
        cue = text; log("CUE", text);
        c.runOnClient(client -> { client.gui.hud.setOverlayMessage(Component.literal(text), false); client.gui.hud.getChat().clearMessages(true); });
    }
    private static void gap(ClientGameTestContext c) { c.waitTicks(30); }
    private static long intent(TestDedicatedServerContext s, Intent action, boolean required) {
        return s.computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            long frame = Frames.atServerTick(server.getTickCount());
            boolean accepted = CombatRuntime.get(server).session(player).intent(action, frame);
            log("INTENT", action + " accepted=" + accepted + " frame=" + frame);
            if (required && !accepted) throw new AssertionError("Rejected review action " + action + " at " + cue);
            return frame;
        });
    }
    private static void waitFrame(TestDedicatedServerContext s, long frame) {
        s.waitFor(server -> Frames.atServerTick(server.getTickCount()) >= frame, 1200);
    }
    private static void clear(TestDedicatedServerContext s) {
        s.runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            for (var enemy : p.level().getEntitiesOfClass(Hilichurl.class, p.getBoundingBox().inflate(100))) enemy.discard();
        });
        target = -1;
    }
    private static void reset(ClientGameTestContext c, TestDedicatedServerContext s, int slot, boolean enemy) {
        mark(c, "Fixture reset / select party slot " + (slot + 1));
        clear(s);
        s.runCommand("genshin managed off"); c.waitFor(client -> !CombatInput.managed());
        PersistenceChecks.resetFixture(s);
        s.runCommand("genshin managed on"); c.waitFor(client -> CombatInput.managed());
        s.runCommand("gamemode adventure @a");
        s.runCommand("tp @a " + origin.x + " " + origin.y + " " + origin.z + " 0 0");
        c.runOnClient(client -> { ManagedCamera.setAngles(0, 12); client.player.setYRot(0); client.player.setXRot(0); });
        c.waitTicks(5);
        if (slot != 0) { intent(s, Intent.values()[Intent.SWITCH_1.ordinal() + slot], true); c.waitFor(client -> CombatInput.state().activeSlot() == slot); }
        c.waitTicks(25);
        if (enemy) target = spawn(s, 0, 2.5, true, 20);
        if (enemy) { int id = target; c.waitFor(client -> client.level.getEntity(id) != null); }
        gap(c);
    }
    private static int spawn(TestDedicatedServerContext s, double x, double z, boolean noAi, int level) {
        return s.computeOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            var enemy = new Hilichurl(GenshinEntities.HILICHURL, p.level());
            enemy.setGenshinLevel(level); enemy.setNoAi(noAi);
            enemy.snapTo(p.position().add(x, 0, z)); enemy.setCamp(enemy.position(), enemy.position());
            if (!p.level().addFreshEntity(enemy)) throw new AssertionError("Review target spawn failed");
            return enemy.getId();
        });
    }
    private static void tap(TestDedicatedServerContext s, Intent action) {
        intent(s, action, true);
        if (action == Intent.ATTACK_PRESS) intent(s, Intent.ATTACK_RELEASE, true);
        if (action == Intent.SKILL_PRESS) intent(s, Intent.SKILL_RELEASE, false);
    }
    private static void seedCrit(TestDedicatedServerContext s) {
        s.runOnServer(server -> {
            var session = CombatRuntime.get(server).session(server.getPlayerList().getPlayers().getFirst());
            try { var field = session.getClass().getDeclaredField("random"); field.setAccessible(true); ((Random) field.get(session)).setSeed(4096); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
    }
    private static void characters(ClientGameTestContext c, TestDedicatedServerContext s) {
        String[] names = {"Traveler", "Amber", "Kaeya", "Lisa"};
        for (int slot = 0; slot < 4; slot++) {
            reset(c, s, slot, true);
            for (int n = 0; n < (slot == 3 ? 4 : 5); n++) {
                mark(c, names[slot] + " normal N" + (n + 1));
                int strike = switch (slot) { case 0 -> TravelerAnemoKit.normalStrike(n); case 1 -> AmberKit.normalStrike(n); case 2 -> KaeyaKit.normalStrike(n); default -> LisaKit.normalStrike(n); };
                int recovery = switch (slot) { case 0 -> TravelerAnemoKit.normalRecovery(n); case 1 -> AmberKit.normalRecovery(n); case 2 -> KaeyaKit.normalRecovery(n); default -> LisaKit.normalRecovery(n); };
                long start = intent(s, Intent.ATTACK_PRESS, true); intent(s, Intent.ATTACK_RELEASE, true);
                waitFrame(s, start + Math.max(recovery + 6, strike + 84));
            }
            gap(c);
            mark(c, names[slot] + (slot == 1 ? " fully charged aimed shot" : " charged attack"));
            long start = intent(s, Intent.ATTACK_PRESS, true);
            waitFrame(s, start + 100); intent(s, Intent.ATTACK_RELEASE, true); c.waitTicks(40); gap(c);
            reset(c, s, slot, true);
            mark(c, names[slot] + " skill tap / particles"); tap(s, Intent.SKILL_PRESS);
            c.waitTicks(slot == 1 ? 190 : 65); gap(c);
            mark(c, names[slot] + " skill cooldown ready");
            c.waitFor(client -> CombatInput.state().skillRemainingFrames() == 0, 1200); gap(c);
            mark(c, names[slot] + (slot == 0 || slot == 3 ? " skill hold / full release" : " skill held input (same tap-only skill)"));
            start = intent(s, Intent.SKILL_PRESS, true); waitFrame(s, start + 125); intent(s, Intent.SKILL_RELEASE, false);
            c.waitTicks(slot == 1 ? 175 : 65); gap(c);
            reset(c, s, slot, true);
            mark(c, names[slot] + " burst becomes ready");
            s.runOnServer(server -> { var session = CombatRuntime.get(server).session(server.getPlayerList().getPlayers().getFirst()); session.kit().grantEnergy(80); session.resourcesChanged(); });
            c.waitTicks(35); gap(c);
            mark(c, names[slot] + " burst"); tap(s, Intent.BURST_PRESS);
            c.waitTicks(slot == 3 ? 345 : slot == 2 ? 190 : 145); gap(c);
        }
    }
    private static void reactions(ClientGameTestContext c, TestDedicatedServerContext s) {
        String[] names = {"Swirl", "Overloaded", "Melt", "Vaporize", "Superconduct", "Electro-Charged", "Frozen"};
        int[] slots = {0, 3, 2, 1, 2, 3, 2};
        String[] auras = {"pyro", "pyro", "pyro", "hydro", "electro", "hydro", "hydro"};
        for (int i = 0; i < names.length; i++) {
            reset(c, s, slots[i], true);
            s.runCommand("execute as @a at @s run genshin aura " + auras[i] + " 4");
            gap(c); mark(c, "Reaction: " + names[i] + " (debug " + auras[i] + " aura)");
            if (slots[i] == 1) { long start = intent(s, Intent.ATTACK_PRESS, true); waitFrame(s, start + 90); intent(s, Intent.ATTACK_RELEASE, true); }
            else tap(s, Intent.SKILL_PRESS);
            c.waitTicks(85); gap(c);
        }
        reset(c, s, 0, true); seedCrit(s); mark(c, "Actual seeded critical talent hit"); tap(s, Intent.ATTACK_PRESS); c.waitTicks(40); gap(c);
    }
    private static void traversal(ClientGameTestContext c, TestDedicatedServerContext s) {
        reset(c, s, 0, false);
        mark(c, "Dash then held sprint"); c.getInput().holdKey(options -> options.keyUp); c.waitTicks(4);
        c.getInput().holdKey(options -> options.keySprint); c.waitTicks(45);
        c.getInput().releaseKey(options -> options.keySprint); c.getInput().releaseKey(options -> options.keyUp); gap(c);
        reset(c, s, 0, false);
        int x = (int) Math.floor(origin.x), y = (int) origin.y, z = (int) Math.floor(origin.z);
        String wall = (x-2)+" "+y+" "+(z+3)+" "+(x+2)+" "+(y+14)+" "+(z+3);
        s.runCommand("fill " + wall + " minecraft:stone");
        mark(c, "Climb attachment and ascent"); c.getInput().holdKey(options -> options.keyUp);
        c.waitFor(client -> CombatInput.state().climbing() && client.player.getY() > origin.y + 2, 600);
        c.getInput().releaseKey(options -> options.keyUp); gap(c);
        mark(c, "Climb jump"); c.getInput().pressKey(options -> options.keyJump); c.waitTicks(35); gap(c);
        c.getInput().holdKey(options -> options.keyShift); c.waitTicks(5); c.getInput().releaseKey(options -> options.keyShift);
        s.runCommand("fill " + wall + " minecraft:air"); c.waitTicks(50); gap(c);
        reset(c, s, 0, false);
        String tower = (x+6)+" "+y+" "+z+" "+(x+8)+" "+(y+11)+" "+(z+2);
        s.runCommand("fill " + tower + " minecraft:stone");
        s.runCommand("tp @a " + (x+7.5)+" "+(y+12)+" "+(z+1.5)+" 0 0");
        c.waitFor(client -> client.player.onGround() && client.player.getY() > y+11, 400);
        c.runOnClient(client -> ManagedCamera.setAngles(0, 15));
        mark(c, "Jump from platform"); c.getInput().holdKey(options -> options.keyUp); c.getInput().holdKey(options -> options.keyJump);
        c.waitFor(client -> client.player.getZ() > z+3.5 && !client.player.onGround(), 400);
        c.getInput().releaseKey(options -> options.keyJump); c.waitTicks(2);
        mark(c, "Glider open"); c.getInput().pressKey(options -> options.keyJump); c.waitFor(client -> CombatInput.state().gliding(), 400);
        c.getInput().releaseKey(options -> options.keyUp); c.waitTicks(40); gap(c);
        mark(c, "Glider close"); c.getInput().pressKey(options -> options.keyJump); c.waitFor(client -> !CombatInput.state().gliding(), 400);
        mark(c, "Landing after glider close"); c.waitFor(client -> client.player.onGround(), 400); gap(c);
        s.runCommand("fill " + tower + " minecraft:air");
        reset(c, s, 0, false);
        mark(c, "High fall: landing, HP loss, fallen member, forced switch");
        s.runCommand("tp @a " + origin.x+" "+(origin.y+40)+" "+origin.z+" 0 0");
        c.waitFor(client -> client.player.onGround() && CombatInput.state().activeSlot() != 0, 600); c.waitTicks(40); gap(c);
    }
    private static void feedbackAndEnemy(ClientGameTestContext c, TestDedicatedServerContext s) {
        reset(c, s, 0, false);
        for (int slot : new int[]{1, 2, 3, 0}) {
            mark(c, "Manual switch " + (slot+1)); c.getInput().pressKey(GLFW.GLFW_KEY_1+slot);
            c.waitFor(client -> CombatInput.state().activeSlot() == slot); c.waitTicks(35); gap(c);
        }
        mark(c, "Rejected burst: insufficient energy"); intent(s, Intent.BURST_PRESS, false); c.waitTicks(40); gap(c);
        mark(c, "Hilichurl aggro / telegraph / connected club strike");
        target = spawn(s, 0, 1.8, false, 8); c.waitTicks(65);
        int id = target;
        s.runOnServer(server -> ((Hilichurl) server.getPlayerList().getPlayers().getFirst().level().getEntity(id)).setNoAi(true));
        gap(c); mark(c, "Hilichurl hurt from normal hit"); seedCrit(s); tap(s, Intent.ATTACK_PRESS); c.waitTicks(40); gap(c);
        s.runOnServer(server -> { var enemy = (Hilichurl) server.getPlayerList().getPlayers().getFirst().level().getEntity(id); if (enemy.isAlive()) enemy.setHealth(.01F); });
        mark(c, "Hilichurl death from normal hit"); tap(s, Intent.ATTACK_PRESS); c.waitTicks(45); gap(c);
    }
    private static void waypoint(ClientGameTestContext c, TestDedicatedServerContext s) {
        reset(c, s, 0, false);
        s.runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst(); var base = new OverlayDefinition.Position(origin.x, origin.y, origin.z, 0);
            var first = new OverlayDefinition.Position(origin.x, origin.y, origin.z+3, 0);
            var arrival = new OverlayDefinition.Position(origin.x+8, origin.y, origin.z, 0);
            OverlayRuntime.apply(server, new OverlayDefinition(1, "sound-review", p.level().dimension().identifier().toString(), base,
                    List.of(new OverlayDefinition.TravelPoint("review", "Sound review waypoint", "waypoint", first, arrival)), List.of()));
        });
        c.waitTicks(20); mark(c, "Waypoint activation (real right click)");
        c.runOnClient(client -> { ManagedCamera.setAngles(0, 12); client.player.setYRot(0); client.player.setXRot(0); });
        c.getInput().pressKey(options -> options.keyUse);
        c.waitForScreen(TeleportScreen.class); c.waitTicks(40); gap(c);
        mark(c, "Waypoint teleport (unlocked destination)"); c.getInput().pressKey(GLFW.GLFW_KEY_TAB); c.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
        c.waitFor(client -> client.gui.screen() == null && client.player.getX() > origin.x+7, 400); c.waitTicks(40); gap(c);
    }
    private static void busy(ClientGameTestContext c, TestDedicatedServerContext s) {
        reset(c, s, 0, false);
        for (int i = 0; i < 3; i++) spawn(s, i-1, 2.5, false, 20);
        s.runOnServer(server -> { var session = CombatRuntime.get(server).session(server.getPlayerList().getPlayers().getFirst()); for (var member : session.party().members()) member.grantEnergy(80); session.resourcesChanged(); });
        c.waitTicks(10);
        mark(c, "Busy fight START: party vs three Lv20 hilichurls (20 seconds)");
        long until = System.nanoTime()+20_000_000_000L;
        int step = 0;
        while (System.nanoTime() < until) {
            int slot = (step/6)%4;
            intent(s, Intent.values()[Intent.SWITCH_1.ordinal()+slot], false);
            if (step%6 == 0) { intent(s, Intent.BURST_PRESS, false); }
            else if (step%6 == 2) { intent(s, Intent.SKILL_PRESS, false); intent(s, Intent.SKILL_RELEASE, false); }
            else { intent(s, Intent.ATTACK_PRESS, false); intent(s, Intent.ATTACK_RELEASE, false); }
            c.waitTicks(10); step++;
        }
        mark(c, "Busy fight END"); clear(s); gap(c);
    }
    static void run(ClientGameTestContext c) {
        try { Files.createDirectories(ROOT); } catch (java.io.IOException e) { throw new AssertionError(e); }
        var properties = ClientGameTestServers.flatServerProperties();
        properties.setProperty("server-ip", "127.0.0.1");
        properties.setProperty("online-mode", "false");
        // Fabric's phased test server intentionally waits for software-rendered client/test phases.
        // Its ordinary tick watchdog is not a deadline for this recording-only sequence; the launcher is.
        properties.setProperty("max-tick-time", "0");
        ClientGameTestServers.withDedicatedServer(c, properties, "Private sound review", s -> {
            c.runOnClient(client -> {
                client.getWindow().setWindowed(1280, 720);
                for (var source : SoundSource.values()) client.options.getSoundSourceOptionInstance(source).set(1.0);
                client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0);
                client.options.getSoundSourceOptionInstance(SoundSource.RECORDS).set(0.0);
                client.getSoundManager().stop();
                client.options.particles().set(ParticleStatus.ALL); client.options.renderDistance().set(5); client.options.fov().set(70);
                client.gui.toastManager().clear();
                client.getSoundManager().addListener((sound, weighed, radius) -> log("SOUND", String.format(Locale.ROOT,
                        "%s source=%s volume=%.4f pitch=%.4f pos=%.2f,%.2f,%.2f file=%s tick=%d", sound.getIdentifier(), sound.getSource(), sound.getVolume(), sound.getPitch(), sound.getX(), sound.getY(), sound.getZ(), sound.getSound().getLocation(), client.level == null ? -1 : client.level.getGameTime())));
                for (var source : SoundSource.values()) log("VOLUME", source + "=" + client.options.getSoundSourceVolume(source));
            });
            try { Files.writeString(ROOT.resolve("ready"), "ok\n"); } catch (java.io.IOException e) { throw new AssertionError(e); }
            c.waitFor(client -> Files.exists(ROOT.resolve("capture-started")), 1200);
            s.runCommand("execute as @a at @s run genshin arena"); c.waitFor(client -> CombatInput.managed());
            s.runCommand("time set day"); s.runCommand("weather clear"); s.runCommand("title @a clear");
            origin = c.computeOnClient(client -> client.player.position()); c.waitTicks(30);
            mark(c, "Private arena sound review: normal volume, real server events"); gap(c);
            characters(c, s); reactions(c, s); traversal(c, s); feedbackAndEnemy(c, s); waypoint(c, s); busy(c, s);
            mark(c, "Sound review COMPLETE"); c.waitTicks(40);
            try { Files.writeString(ROOT.resolve("completed"), "ok\n"); } catch (java.io.IOException e) { throw new AssertionError(e); }
        });
    }
}

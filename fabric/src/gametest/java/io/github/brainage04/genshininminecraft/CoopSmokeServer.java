package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.world.TestArena;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Test-source-only file control bridge. Never present in either release jar; never touches a purchased world. */
public final class CoopSmokeServer implements ModInitializer {
    private Path directory;
    private String phase = "joining";
    private int started;
    private Hilichurl enemy;
    private double before;
    private double survivingHp;
    private ServerPlayer deadPlayer;
    private boolean arenaReady;
    @Override public void onInitialize() {
        String root = System.getProperty("genshin.coopSmoke");
        if (root == null) return;
        directory = Path.of(root);
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }
    private ServerPlayer player(MinecraftServer server, String name) {
        return server.getPlayerList().getPlayers().stream().filter(player -> player.getGameProfile().name().equals(name)).findFirst().orElse(null);
    }
    private boolean ack(String name) { return Files.isRegularFile(directory.resolve(name)); }
    private void phase(MinecraftServer server, String next) throws java.io.IOException {
        phase = next; started = server.getTickCount(); Files.writeString(directory.resolve("phase"), next);
        GenshinInMinecraft.LOGGER.info("Two-client co-op smoke phase: {}", next);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private void face(ServerPlayer player) {
        Vec3 delta = enemy.position().add(0, 1, 0).subtract(player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.horizontalDistanceSqr())));
        player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.of(), yaw, pitch, false);
    }
    private void tick(MinecraftServer server) {
        try {
            if (phase.equals("done")) {
                if (ack("CoopHost-done") && ack("CoopGuest-done")) server.halt(false);
                return;
            }
            ServerPlayer host = player(server, "CoopHost"), guest = player(server, "CoopGuest");
            if (host == null) return;
            if (!arenaReady) { TestArena.build(host); arenaReady = true; }
            if (guest == null) return;
            var runtime = CombatRuntime.get(server);
            if (!phase.equals("joining") && server.getTickCount() - started > (phase.equals("respawn") ? 200 : 2400))
                throw new AssertionError("Smoke phase timed out: " + phase + "; guest replacement=" + (guest != deadPlayer)
                        + ", nativeHp=" + guest.getHealth() + ", alive=" + guest.isAlive()
                        + ", allocation=" + runtime.session(guest).party().allocatedMask() + ", kitHp=" + runtime.session(guest).kit().hp());
            switch (phase) {
                case "joining" -> {
                    guest.setGameMode(GameType.ADVENTURE);
                    guest.teleportTo(host.level(), host.getX() + 2, host.getY(), host.getZ(), Set.of(), 0, 0, false);
                    runtime.reconcileCoop(server);
                    require(runtime.pickRoster(host, 5) && runtime.pickRoster(guest, 10), "Each client must select its own two-character allocation");
                    require(runtime.receive(host, Intent.SWITCH_3) && runtime.receive(guest, Intent.SWITCH_4), "Distinct Kaeya/Lisa active bodies");
                    enemy = new Hilichurl(GenshinEntities.HILICHURL, host.level()); enemy.setGenshinLevel(20);
                    enemy.snapTo(host.position().add(0, 0, 2)); enemy.setNoAi(true); enemy.setCustomName(net.minecraft.network.chat.Component.literal("Co-op smoke hilichurl"));
                    require(host.level().addFreshEntity(enemy), "Shared enemy spawn");
                    require(Math.abs(runtime.target(enemy).maxHp() - 1327.8000024) < .001, "Sourced two-player enemy HP13.584×65.1649×1.50");
                    before = runtime.target(enemy).hp(); phase(server, "models");
                }
                case "models" -> {
                    if (ack("CoopHost-model") && ack("CoopGuest-model")) { face(host); face(guest); phase(server, "host-hit"); }
                }
                case "host-hit" -> {
                    if (ack("CoopHost-hit") && runtime.target(enemy).hp() < before && enemy.getLastHurtByPlayer() == host) {
                        before = runtime.target(enemy).hp(); phase(server, "guest-hit");
                    }
                }
                case "guest-hit" -> {
                    if (ack("CoopGuest-hit") && runtime.target(enemy).hp() < before && enemy.getLastHurtByPlayer() == guest
                            && runtime.target(enemy).aura().gauge(Element.ELECTRO) > 0) {
                        before = runtime.target(enemy).hp(); phase(server, "reaction");
                    }
                }
                case "reaction" -> {
                    if (ack("CoopHost-reaction") && runtime.target(enemy).hp() < before && enemy.getLastHurtByPlayer() == host
                            && runtime.target(enemy).aura().physicalResistanceReduction() == .4) {
                        survivingHp = runtime.session(host).kit().hp(); deadPlayer = guest;
                        guest.hurtServer(guest.level(), guest.level().damageSources().genericKill(), Float.MAX_VALUE);
                        require(guest.isAlive(), "Guest's other allocated member survives first fall");
                        phase(server, "guest-falling");
                    }
                }
                case "guest-falling" -> {
                    if (server.getTickCount() - started >= 21) {
                        guest.hurtServer(guest.level(), guest.level().damageSources().genericKill(), Float.MAX_VALUE);
                        require(!guest.isAlive() && runtime.session(guest).party().wiped(),
                                "Only guest allocated roster wipes: mask=" + runtime.session(guest).party().allocatedMask()
                                        + ", active=" + runtime.session(guest).party().activeSlot() + ", hp=" + runtime.session(guest).kit().hp());
                        require(host.isAlive() && runtime.session(host).kit().hp() == survivingHp, "Host survives guest wipe");
                        before = runtime.target(enemy).hp(); phase(server, "guest-wiped");
                    }
                }
                case "guest-wiped" -> {
                    if (ack("CoopHost-continue") && runtime.target(enemy).hp() < before && enemy.getLastHurtByPlayer() == host) phase(server, "respawn");
                }
                case "respawn" -> {
                    if (guest.isAlive() && guest != deadPlayer) {
                        var party = runtime.session(guest).party();
                        for (int slot = 0; slot < 4; slot++) if (party.allocated(slot))
                            require(Math.abs(party.kit(slot).hp() - Math.round(party.kit(slot).maxHp() * .35)) < .001, "Guest own35% respawn");
                        require(host.isAlive() && runtime.session(host).kit().hp() == survivingHp, "Host continues unchanged through guest respawn");
                        Files.writeString(directory.resolve("server-result.json"), "{\"passed\":true,\"clients\":2,\"enemyMaxHp\":1327.8000024,\"bothHitSameEnemy\":true,\"crossPlayerSuperconduct\":true,\"independentWipeRespawn\":true}\n");
                        phase(server, "done");
                    }
                }
            }
        } catch (Throwable failure) {
            try { Files.writeString(directory.resolve("failure.txt"), failure.toString()); } catch (java.io.IOException ignored) {}
            GenshinInMinecraft.LOGGER.error("Two-client co-op smoke failed", failure); server.halt(false);
        }
    }
}

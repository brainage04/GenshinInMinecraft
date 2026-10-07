package io.github.brainage04.genshininminecraft.gametest;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.BaronBunny;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.Party;
import io.github.brainage04.genshininminecraft.rules.PartySave;
import io.github.brainage04.genshininminecraft.rules.kit.AmberKit;
import io.github.brainage04.genshininminecraft.rules.kit.KaeyaKit;
import io.github.brainage04.genshininminecraft.rules.kit.LisaKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.world.ManagedWorldData;
import io.github.brainage04.genshininminecraft.world.PartySavedData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/** Resources survive entity and rules-clock replacement; casts do not. Shared by both loaders. */
public final class PersistenceGameTests {
    private PersistenceGameTests() {}

    public static void logoutAndSavedDataRestartRestoreResourcesNotFields(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var server = context.getLevel().getServer();
            var session = runtime.session(player);
            long start = Frames.atServerTick(server.getTickCount());
            var enemy = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
            enemy.setGenshinLevel(20);
            enemy.snapTo(player.position().add(0, 0, 3));
            enemy.setNoAi(true);
            context.assertTrue(context.getLevel().addFreshEntity(enemy), "Field target spawned");
            var target = runtime.target(enemy);
            context.assertTrue(session.intent(Intent.SWITCH_2, start), "Select Amber");
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start), "Create Bunny");
            session.advanceTo(start + 45);
            var bunnies = context.getLevel().getEntitiesOfClass(BaronBunny.class,
                    new AABB(player.position(), player.position()).inflate(10));
            context.assertValueEqual(bunnies.size(), 1, "Real landed Bunny exists before logout");
            var bunny = bunnies.getFirst();
            context.assertTrue(session.intent(Intent.SWITCH_4, start + 60), "Select Lisa");
            session.kit().grantEnergy(80);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start + 60), "Create Rose");
            context.assertTrue(session.intent(Intent.SWITCH_2, start + 120), "Return to Amber");
            session.kit().grantEnergy(40);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start + 120), "Create Rain");
            context.assertTrue(session.intent(Intent.SWITCH_3, start + 181), "Select Kaeya");
            session.kit().grantEnergy(60);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start + 181), "Create Waltz");
            session.advanceTo(start + 234);
            context.assertTrue(((AmberKit) session.party().kit(1)).puppetAlive()
                    && ((KaeyaKit) session.kit()).burstActive(start + 234)
                    && ((LisaKit) session.party().kit(3)).roseActive(start + 234), "Fields are live, not an empty persistence fixture");
            session.party().kit(0).setHp(0);
            session.party().kit(1).setHp(1234.5);
            session.kit().setHp(1700.25);
            session.kit().grantEnergy(23);
            session.stamina().consume(80, start + 234);
            // Exercise the actual shared vanilla world-save injection, not just manual map insertion.
            server.saveAllChunks(true, true, false);
            PartySave expected = PartySavedData.get(server).get(player.getUUID());
            context.assertTrue(expected != null, "Vanilla world save snapshots the online UUID record");
            context.assertTrue(expected.members().get(1).skillRemaining() > 0 && expected.members().get(2).burstRemaining() > 0,
                    "Both skill and burst cooldown remaining are nonzero before logout");
            runtime.forget(player.getUUID());
            context.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            context.assertTrue(bunny.isRemoved(), "Logout discards landed Bunny immediately");
            double hpAfterLogout = target.hp();
            session.advanceTo(start + 2400);
            close(context, target.hp(), hpAfterLogout, "Rain/Waltz/Rose and queued casts never hit after logout");
            var rejoined = replacement(context, player);
            try {
                var restored = runtime.session(rejoined);
                assertResources(context, restored, expected);
                context.assertFalse(((AmberKit) restored.party().kit(1)).puppetAlive(), "Bunny rules state is not persisted");
                context.assertFalse(((KaeyaKit) restored.party().kit(2)).burstActive(restored.party().frame()), "Waltz is not persisted");
                context.assertFalse(((LisaKit) restored.party().kit(3)).roseActive(restored.party().frame()), "Rose is not persisted");
                restored.advanceTo(restored.party().frame() + 3);
                context.assertValueEqual((int) restored.party().kit(1).skillRemaining(), expected.members().get(1).skillRemaining() - 3,
                        "Cooldown resumes online from saved remaining, not from an old absolute deadline");
                // New runtime means a new rules clock. Serialize and replace the world store as on restart.
                CombatRuntime.saveAll(server);
                PartySave restartExpected = PartySavedData.get(server).get(player.getUUID());
                var tag = PartySavedData.TYPE.codec().encodeStart(NbtOps.INSTANCE, PartySavedData.get(server)).getOrThrow();
                CombatRuntime.stop(server);
                var loaded = PartySavedData.TYPE.codec().parse(NbtOps.INSTANCE, tag).getOrThrow();
                server.overworld().getDataStorage().set(PartySavedData.TYPE, loaded);
                context.assertValueEqual(loaded.get(player.getUUID()), restartExpected, "All schema fields round trip through NBT codec");
                var restarted = CombatRuntime.get(server).session(rejoined);
                assertResources(context, restarted, restartExpected);
                context.assertTrue(restarted.party().frame() < restored.party().frame(), "Restart really rebases to a lower session clock");
            } finally {
                context.getLevel().removePlayerImmediately(rejoined, Entity.RemovalReason.DISCARDED);
            }
        });
        context.succeed();
    }

    public static void managedOffSuspendsResourcesAndCancelsQueuedLanding(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var server = context.getLevel().getServer();
            var session = runtime.session(player);
            long start = Frames.atServerTick(server.getTickCount());
            session.intent(Intent.SWITCH_2, start);
            session.kit().setHp(700.5);
            session.kit().grantEnergy(19);
            session.stamina().consume(45, start);
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start), "Bunny landing queued before off");
            ManagedWorldData.get(server).setManaged(server, false);
            var saved = PartySavedData.get(server).get(player.getUUID());
            context.assertTrue(saved.members().get(1).skillRemaining() > 0 && saved.stamina() == 55,
                    "Managed-off retains a running skill cooldown and depleted stamina, not a reset fixture");
            session.advanceTo(start + 1800);
            context.assertTrue(context.getLevel().getEntitiesOfClass(BaronBunny.class,
                    new AABB(player.position(), player.position()).inflate(10)).isEmpty(), "Managed-off cancels the queued Bunny landing");
            player.setHealth(20); // Vanilla off-mode health must not overwrite the suspended Genshin HP.
            ManagedWorldData.get(server).setManaged(server, true);
            assertResources(context, runtime.session(player), saved);
        });
        context.succeed();
    }

    public static void wipedPartyRespawnRevives35PercentAndRetainsCooldown(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            session.intent(Intent.SKILL_PRESS, start);
            session.intent(Intent.SKILL_RELEASE, start);
            for (int slot = 0; slot < Party.SIZE; slot++) session.party().kit(slot).setHp(0);
            player.die(context.getLevel().damageSources().genericKill());
            context.assertFalse(player.isAlive(), "A real party wipe permits vanilla death");
            runtime.forget(player.getUUID());
            var saved = PartySavedData.get(context.getLevel().getServer()).get(player.getUUID());
            context.assertTrue(saved.members().stream().allMatch(PartySave.Member::fallen), "Logout saves fallen members, not an implicit revival");
            context.assertTrue(saved.members().getFirst().skillRemaining() > 0, "Wipe fixture retains a nonzero skill cooldown");
            context.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            var respawned = replacement(context, player);
            try {
                runtime.respawn(respawned); // Shared adapter invoked by each loader's after-death respawn event.
                var restored = runtime.session(respawned);
                for (var member : restored.party().members()) {
                    close(context, member.hp(), Math.round(member.maxHp() * .35), "Every wiped member revives at sourced rounded35% HP");
                    close(context, member.energy(), 0, "Fallen energy remains zero after revival");
                }
                context.assertValueEqual((int) restored.party().kit(0).skillRemaining(), saved.members().getFirst().skillRemaining(),
                        "Respawn does not reset the surviving cooldown state");
                close(context, respawned.getHealth(), 20 * restored.kit().hp() / restored.kit().maxHp(), "Respawn mirrors restored active HP");
            } finally {
                context.getLevel().removePlayerImmediately(respawned, Entity.RemovalReason.DISCARDED);
            }
        });
        context.succeed();
    }

    private static ServerPlayer replacement(GameTestHelper context, ServerPlayer previous) {
        var server = context.getLevel().getServer();
        var player = new ServerPlayer(server, context.getLevel(), previous.getGameProfile(), ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(previous.getGameProfile(), false));
        player.setGameMode(GameType.SURVIVAL);
        player.snapTo(previous.position());
        context.getLevel().addNewPlayer(player);
        return player;
    }
    private static void assertResources(GameTestHelper context, CombatRuntime.Session restored, PartySave expected) {
        context.assertValueEqual(restored.party().save(), expected, "HP/energy/cooldowns/spans/slot/stamina/fallen survive the lifecycle boundary exactly");
    }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .0001, message + ": expected " + expected + ", actual " + actual);
    }
}

package io.github.brainage04.genshininminecraft.gametest;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.world.*;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Synthetic binding lives entirely on the existing small test floor, not on the purchased world. */
public final class OverlayGameTests {
    private OverlayGameTests() {}
    public static OverlayDefinition fixture(ServerPlayer player) {
        var origin = player.position();
        var statue = pos(origin.add(0, 0, 2));
        var near = pos(origin.add(3, 0, 0));
        var far = pos(origin.add(7, 0, 0));
        var camp = pos(origin.add(-5, 0, 4));
        return new OverlayDefinition(1, "synthetic-overlay", player.level().dimension().identifier().toString(), pos(origin),
                List.of(new OverlayDefinition.TravelPoint("statue", "Test Statue", "statue", statue, pos(origin.add(0, 0, 7))),
                        new OverlayDefinition.TravelPoint("near", "Near Waypoint", "waypoint", near, pos(origin.add(3, 0, 1))),
                        new OverlayDefinition.TravelPoint("far", "Far Waypoint", "waypoint", far, pos(origin.add(7, 0, 1)))),
                List.of(new OverlayDefinition.Camp("camp", "Test camp", camp, 43200000,
                        List.of(new OverlayDefinition.Member(pos(origin.add(-6, 0, 4)), 8, "basic"),
                                new OverlayDefinition.Member(pos(origin.add(-5, 0, 4)), 8, "basic"),
                                new OverlayDefinition.Member(pos(origin.add(-4, 0, 4)), 8, "shooter-substituted-by-basic")))));
    }
    private static OverlayDefinition.Position pos(Vec3 value) { return new OverlayDefinition.Position(value.x, value.y, value.z, 0); }
    public static void overlayApplyIsIdempotentAndPreservesBlocks(GameTestHelper context) {
        withOverlay(context, (runtime, player) -> {
            var level = player.level();
            var definition = OverlaySavedData.get(level.getServer()).definition();
            var ground = player.blockPosition().below();
            var before = level.getBlockState(ground);
            context.assertTrue(OverlayRuntime.apply(level.getServer(), definition), "Repeated explicit apply succeeds");
            OverlayRuntime.reconcile(level.getServer(), System.currentTimeMillis());
            for (var point : definition.points()) {
                var marker = level.getEntityInAnyDimension(definition.entityId(point.id()));
                context.assertTrue(marker instanceof OverlayMarker, "Exactly one stable UUID identifies " + point.id());
                context.assertValueEqual(((OverlayMarker) marker).overlayId(), point.id(), "Overlay ID survives reconciliation");
                var count = 0;
                for (var entity : level.getAllEntities()) if (entity instanceof OverlayMarker value && value.overlayId().equals(point.id())) count++;
                context.assertValueEqual(count, 1, "No duplicate marker after apply twice");
            }
            var marker = (OverlayMarker) level.getEntityInAnyDimension(definition.entityId("near"));
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            context.assertTrue(marker.save(output), "Marker is persisted as a real mod entity");
            marker.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            var reloaded = EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,
                    level.registryAccess(), output.buildResult()), level, EntitySpawnReason.LOAD, entity -> entity);
            context.assertTrue(reloaded instanceof OverlayMarker && level.addFreshEntity(reloaded), "Real marker NBT reloads");
            OverlayRuntime.reconcile(level.getServer(), System.currentTimeMillis());
            context.assertTrue(level.getEntityInAnyDimension(definition.entityId("near")) == reloaded, "Apply keeps the saved entity and stable ID");
            context.assertValueEqual(level.getBlockState(ground), before, "Applying never changes test/map floor blocks");
        });
        context.succeed();
    }
    public static void overlayActivationPersistsPerPlayerAndTeleportMoves(GameTestHelper context) {
        withOverlay(context, (runtime, player) -> {
            var server = player.level().getServer();
            var data = OverlaySavedData.get(server);
            var definition = data.definition();
            player.snapTo(OverlayRuntime.vec(definition.arrival()).add(0, -200, 0));
            OverlayRuntime.arrive(player);
            close(context, player.position().distanceTo(OverlayRuntime.vec(definition.arrival())), 0,
                    "First overlay entry bypasses the port's bottom-of-world vanilla spawn heightmap");
            player.snapTo(OverlayRuntime.vec(definition.arrival()).add(.5, 0, 0));
            OverlayRuntime.arrive(player);
            close(context, player.position().distanceTo(OverlayRuntime.vec(definition.arrival())), .5, "First-arrival metadata never pins later movement");
            context.assertFalse(OverlayRuntime.teleport(player, "near"), "Locked travel rejects forged IDs");
            OverlayRuntime.recoverNearStatue(player, 0);
            context.assertFalse(data.activated(player.getUUID(), "statue"), "Statue proximity is not first-touch activation");
            var marker = (OverlayMarker) player.level().getEntityInAnyDimension(definition.entityId("near"));
            marker.interact(player, InteractionHand.MAIN_HAND, marker.position());
            context.assertTrue(data.activated(player.getUUID(), "near"), "Actual marker interaction unlocks");
            context.assertFalse(data.activated(new UUID(0, 1), "near"), "Another player's unlock set stays separate");
            data = roundTrip(server);
            context.assertTrue(data.activated(player.getUUID(), "near"), "Unlock persists through real NBT codec/store replacement");
            context.assertFalse(data.activated(new UUID(0, 1), "near"), "Per-player boundary survives restart storage");
            OverlayRuntime.arrive(player);
            close(context, player.position().distanceTo(OverlayRuntime.vec(definition.arrival())), .5, "First-entry marker persists across reload");
            var session = runtime.session(player);
            session.kit().setHp(1234);
            session.kit().grantEnergy(17);
            session.resourcesChanged();
            context.assertTrue(OverlayRuntime.teleport(player, "near"), "Activated free teleport accepted");
            close(context, player.position().distanceTo(OverlayRuntime.vec(definition.point("near").arrival())), 0, "Server moves to authored arrival");
            close(context, session.kit().hp(), 1234, "Ordinary waypoint teleport never heals");
            close(context, session.kit().energy(), 17, "Voluntary travel is not death/energy reset");
        });
        context.succeed();
    }
    public static void overlayWipeUsesNearestActivatedPointAt35Percent(GameTestHelper context) {
        withOverlay(context, (runtime, player) -> {
            var server = player.level().getServer();
            var definition = OverlaySavedData.get(server).definition();
            context.assertTrue(OverlayRuntime.interact(player, "near"), "Unlock nearer destination");
            player.snapTo(OverlayRuntime.vec(definition.point("far").position()));
            context.assertTrue(OverlayRuntime.interact(player, "far"), "Unlock far/last-visited destination too");
            player.snapTo(OverlayRuntime.vec(definition.point("near").position()).add(-.5, 0, 0));
            var session = runtime.session(player);
            for (var member : session.party().members()) member.setHp(0);
            player.die(player.level().damageSources().genericKill());
            context.assertFalse(player.isAlive(), "Last member's death is a real wipe");
            runtime.forget(player.getUUID());
            player.level().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            var revived = new ServerPlayer(server, context.getLevel(), player.getGameProfile(), ClientInformation.createDefault());
            revived.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), revived,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            revived.setGameMode(GameType.SURVIVAL);
            revived.snapTo(OverlayRuntime.vec(definition.arrival()));
            context.getLevel().addNewPlayer(revived);
            try {
                runtime.respawn(revived);
                close(context, revived.position().distanceTo(OverlayRuntime.vec(definition.point("near").arrival())), 0,
                        "Nearest unlocked point at final death wins, not last visited or vanilla spawn");
                for (var member : runtime.session(revived).party().members())
                    close(context, member.hp(), Math.round(member.maxHp() * .35), "Every member has sourced rounded35% HP");
            } finally { runtime.forget(revived.getUUID()); context.getLevel().removePlayerImmediately(revived, Entity.RemovalReason.DISCARDED); }
        });
        context.succeed();
    }
    public static void overlayStatueRevivesAndHealsFromSharedReserve(GameTestHelper context) {
        withOverlay(context, (runtime, player) -> {
            var server = player.level().getServer();
            var data = OverlaySavedData.get(server);
            var party = runtime.session(player).party();
            party.kit(0).setHp(party.kit(0).maxHp() - 100);
            party.kit(1).setHp(0);
            party.kit(2).setHp(party.kit(2).maxHp() - 200);
            party.kit(3).setHp(party.kit(3).maxHp() - 300);
            runtime.session(player).resourcesChanged();
            OverlayRuntime.recoverNearStatue(player, 0);
            close(context, party.kit(1).hp(), Math.round(party.kit(1).maxHp() * .35), "Locked Statue immediately revives off-field member");
            close(context, data.reserve(), 0, "Revival spends no restorative power");
            context.assertTrue(OverlayRuntime.interact(player, "statue"), "Interact to unlock Statue, not proximity");
            context.assertTrue(OverlayRuntime.interact(player, "statue"), "Repeat interaction is harmless");
            close(context, data.capacity(), 5000, "One unlocked physical Statue grants5000 world reserve once");
            close(context, data.reserve(), 5000, "Repeated activation cannot refill reserve");
            double wanted = 0;
            for (var member : party.members()) wanted += member.maxHp() - member.hp();
            OverlayRuntime.recoverNearStatue(player, 39);
            close(context, data.reserve(), 5000, "No healing before two-second dwell");
            OverlayRuntime.recoverNearStatue(player, 40);
            for (var member : party.members()) close(context, member.hp(), member.maxHp(), "Reserve heals all deployed party slots");
            close(context, data.reserve(), 5000 - wanted, "Only actual recovered HP is consumed");
            data = roundTrip(server);
            close(context, data.reserve(), 5000 - wanted, "World reserve survives persistence");
            long now = System.currentTimeMillis();
            data.regenerate(now + 15000);
            close(context, data.reserve(), Math.min(5000, 5000 - wanted + 50), "Regeneration is1% per15 real-time seconds");
        });
        context.succeed();
    }
    public static void overlayCampRespawnsOnlyAfterPersistentTimer(GameTestHelper context) {
        withOverlay(context, (runtime, player) -> {
            var server = player.level().getServer();
            var definition = OverlaySavedData.get(server).definition();
            for (int index = 0; index < 3; index++) {
                var id = "camp." + index;
                var enemy = (Hilichurl) player.level().getEntityInAnyDimension(definition.entityId(id));
                context.assertTrue(enemy != null && enemy.genshinLevel() == 8, "Authored Lv8 member exists");
                enemy.kill(player.level());
                enemy.discard();
                long deadline = OverlaySavedData.get(server).memberState(id);
                context.assertTrue(deadline > System.currentTimeMillis() + 43198000 && deadline <= System.currentTimeMillis() + 43200000,
                        "Actual death schedules the documented twelve-hour timer");
            }
            var data = roundTrip(server);
            long deadline = Math.max(data.memberState("camp.0"), Math.max(data.memberState("camp.1"), data.memberState("camp.2")));
            long earliest = Math.min(data.memberState("camp.0"), Math.min(data.memberState("camp.1"), data.memberState("camp.2")));
            OverlayRuntime.apply(server, definition);
            OverlayRuntime.reconcile(server, earliest - 1);
            for (int index = 0; index < 3; index++) context.assertTrue(player.level().getEntityInAnyDimension(definition.entityId("camp." + index)) == null,
                    "Reapply/reload/return does not bypass killed-member timer");
            OverlayRuntime.reconcile(server, deadline);
            for (int index = 0; index < 3; index++) {
                var id = "camp." + index;
                var enemy = player.level().getEntityInAnyDimension(definition.entityId(id));
                context.assertTrue(enemy instanceof Hilichurl && enemy.isAlive(), "Member respawns at exact timer boundary with stable ID");
                context.assertValueEqual(data.memberState(id), -1L, "Respawn is recorded as alive");
            }
        });
        context.succeed();
    }
    private static OverlaySavedData roundTrip(net.minecraft.server.MinecraftServer server) {
        var tag = OverlaySavedData.CODEC.encodeStart(NbtOps.INSTANCE, OverlaySavedData.get(server)).getOrThrow();
        var decoded = OverlaySavedData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        server.overworld().getDataStorage().set(OverlaySavedData.TYPE, decoded);
        return decoded;
    }
    private static void withOverlay(GameTestHelper context, BiConsumer<CombatRuntime, ServerPlayer> test) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var server = player.level().getServer();
            var original = OverlaySavedData.get(server);
            var spawn = context.getLevel().getRespawnData();
            server.overworld().getDataStorage().set(OverlaySavedData.TYPE, new OverlaySavedData());
            var definition = fixture(player);
            try {
                context.assertTrue(OverlayRuntime.apply(server, definition), "Synthetic overlay applies only in managed world");
                for (var point : definition.points()) context.getLevel().waitForEntities(ChunkPos.containing(BlockPos.containing(OverlayRuntime.vec(point.position()))), 0);
                for (var member : definition.camps().getFirst().members()) context.getLevel().waitForEntities(ChunkPos.containing(BlockPos.containing(OverlayRuntime.vec(member.position()))), 0);
                OverlayRuntime.reconcile(server, System.currentTimeMillis());
                test.accept(runtime, player);
            } finally {
                for (var point : definition.points()) {
                    var entity = context.getLevel().getEntityInAnyDimension(definition.entityId(point.id()));
                    if (entity != null) entity.discard();
                }
                for (int index = 0; index < 3; index++) {
                    var entity = context.getLevel().getEntityInAnyDimension(definition.entityId("camp." + index));
                    if (entity != null) entity.discard();
                }
                context.getLevel().setRespawnData(spawn);
                server.overworld().getDataStorage().set(OverlaySavedData.TYPE, original);
                OverlayRuntime.stop(server);
            }
        });
    }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .0001, message + ": expected " + expected + ", actual " + actual);
    }
}

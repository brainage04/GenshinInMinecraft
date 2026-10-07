package io.github.brainage04.genshininminecraft.world;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.network.TeleportListPayload;
import io.github.brainage04.genshininminecraft.rules.PartySave;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

/** Explicit installation; subsequent loads reconcile only entity-loaded anchor chunks, never map blocks. */
public final class OverlayRuntime {
    public static final double ADAPTED_INTERACTION_RADIUS = 4;
    public static final double ADAPTED_HEAL_RADIUS = 6;
    public static final int HEAL_DWELL_TICKS = 40;
    public static final String MEMBER_TAG = "genshin_overlay:";
    private static final Map<MinecraftServer, OverlayRuntime> SERVERS = new HashMap<>();
    private final Map<UUID, Dwell> dwell = new HashMap<>();
    private boolean bootstrapped;
    private record Dwell(String statue, long since, int playerEntityId) {}
    private OverlayRuntime() {}
    public static void stop(MinecraftServer server) { SERVERS.remove(server); }
    public static Vec3 vec(OverlayDefinition.Position position) { return new Vec3(position.x(), position.y(), position.z()); }
    public static ServerLevel level(MinecraftServer server, OverlayDefinition definition) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(definition.dimension())));
    }
    public static boolean apply(MinecraftServer server, OverlayDefinition definition) {
        if (!ManagedWorld.isManaged(server.overworld())) return false;
        ServerLevel level = level(server, definition);
        if (level == null) throw new IllegalArgumentException("Overlay dimension is unavailable: " + definition.dimension());
        OverlaySavedData.get(server).install(definition);
        var arrival = definition.arrival();
        level.setRespawnData(LevelData.RespawnData.of(level.dimension(), BlockPos.containing(vec(arrival)), arrival.yaw(), 0));
        // Explicit application requests terrain once. Entity creation waits for the async entity read.
        for (var point : definition.points()) level.getChunk(BlockPos.containing(vec(point.position())));
        for (var camp : definition.camps()) for (var member : camp.members()) level.getChunk(BlockPos.containing(vec(member.position())));
        reconcile(server, System.currentTimeMillis());
        return true;
    }
    public static void tick(MinecraftServer server) {
        var runtime = SERVERS.computeIfAbsent(server, ignored -> new OverlayRuntime());
        if (!runtime.bootstrapped) {
            runtime.bootstrapped = true;
            var marker = server.getWorldPath(LevelResource.ROOT).resolve(".genshin-playtest.json");
            if (Files.isRegularFile(marker) && OverlaySavedData.get(server).definition() == null) {
                try {
                    var json = com.google.gson.JsonParser.parseString(Files.readString(marker)).getAsJsonObject();
                    if (json.get("version").getAsInt() != 1 || !json.get("overlay").getAsString().equals("mondstadt"))
                        throw new IllegalArgumentException("Unsupported playtest marker");
                    ManagedWorldData.get(server).setManaged(server, true);
                    apply(server, OverlayDefinition.mondstadt());
                } catch (java.io.IOException exception) { throw new IllegalStateException("Cannot read playtest marker", exception); }
            }
        }
        if (!ManagedWorld.isManaged(server.overworld())) { runtime.dwell.clear(); return; }
        var data = OverlaySavedData.get(server);
        if (data.definition() == null) return;
        long now = System.currentTimeMillis();
        data.regenerate(now);
        if (server.getTickCount() % 20 == 0) reconcile(server, now);
        for (var player : server.getPlayerList().getPlayers()) {
            arrive(player);
            runtime.recover(player, server.overworld().getGameTime());
        }
        runtime.dwell.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }
    /** The explicit clock parameter makes the real twelve-hour timer testable without shortening it. */
    public static void reconcile(MinecraftServer server, long now) {
        if (!ManagedWorld.isManaged(server.overworld())) return;
        var data = OverlaySavedData.get(server);
        var definition = data.definition();
        if (definition == null) return;
        var level = level(server, definition);
        if (level == null) return;
        for (var point : definition.points()) {
            if (!loaded(level, point.position())) continue;
            var entity = level.getEntityInAnyDimension(definition.entityId(point.id()));
            if (entity == null) {
                var marker = new OverlayMarker(GenshinEntities.OVERLAY_MARKER, level);
                marker.setUUID(definition.entityId(point.id()));
                marker.configure(point);
                marker.snapTo(vec(point.position()), point.position().yaw(), 0);
                if (level.addFreshEntity(marker)) entity = marker;
            }
            if (entity instanceof OverlayMarker marker) marker.activated(data.activatedByAnyone(point.id()));
        }
        for (var camp : definition.camps()) {
            for (int index = 0; index < camp.members().size(); index++) {
                String memberId = camp.id() + "." + index;
                var member = camp.members().get(index);
                if (!loaded(level, member.position())) continue;
                var entity = level.getEntityInAnyDimension(definition.entityId(memberId));
                long state = data.memberState(memberId);
                if (entity != null) { if (entity.isAlive() && state == 0) data.memberState(memberId, -1); continue; }
                // A living member can be in an unloaded leash chunk. Absence is NOT evidence of death.
                if (state == -1 || state > now) continue;
                var enemy = new Hilichurl(GenshinEntities.HILICHURL, level);
                enemy.setUUID(definition.entityId(memberId));
                enemy.setGenshinLevel(member.level());
                enemy.addTag(MEMBER_TAG + memberId);
                enemy.snapTo(vec(member.position()), member.position().yaw(), 0);
                enemy.setCamp(vec(camp.position()), vec(member.position()));
                if (level.addFreshEntity(enemy)) {
                    CombatRuntime.get(server).target(enemy);
                    data.memberState(memberId, -1);
                }
            }
        }
    }
    private static boolean loaded(ServerLevel level, OverlayDefinition.Position position) {
        return level.areEntitiesLoaded(ChunkPos.pack((int) Math.floor(position.x()) >> 4, (int) Math.floor(position.z()) >> 4));
    }
    public static void memberDied(Hilichurl member) {
        if (!(member.level() instanceof ServerLevel level)) return;
        var data = OverlaySavedData.get(level.getServer());
        var definition = data.definition();
        if (definition == null) return;
        for (String tag : member.entityTags()) if (tag.startsWith(MEMBER_TAG)) {
            String id = tag.substring(MEMBER_TAG.length());
            for (var camp : definition.camps()) for (int index = 0; index < camp.members().size(); index++) {
                if (id.equals(camp.id() + "." + index) && member.getUUID().equals(definition.entityId(id))) {
                    data.memberState(id, System.currentTimeMillis() + camp.respawnMillis());
                    return;
                }
            }
        }
    }
    /** The port's heightmap can put a new vanilla spawn at Y−2031; use authored feet before damage. */
    public static void arrive(ServerPlayer player) {
        if (!ManagedWorld.isManaged(player.level())) return;
        var server = player.level().getServer();
        var data = OverlaySavedData.get(server);
        if (data.definition() == null || !data.enter(player.getUUID())) return;
        if (PartySavedData.get(server).get(player.getUUID()) == null)
            travel(player, data.definition(), data.definition().arrival());
    }
    public static boolean interact(ServerPlayer player, String pointId) {
        if (!ManagedWorld.isManaged(player.level()) || !player.isAlive() || player.isSpectator()) return false;
        var data = OverlaySavedData.get(player.level().getServer());
        var definition = data.definition();
        var point = definition == null ? null : definition.point(pointId);
        if (point == null || player.level() != level(player.level().getServer(), definition)
                || player.position().distanceToSqr(vec(point.position())) > ADAPTED_INTERACTION_RADIUS * ADAPTED_INTERACTION_RADIUS) return false;
        if (data.activate(player.getUUID(), pointId, System.currentTimeMillis())) {
            player.sendSystemMessage(Component.literal("Activated " + point.name()));
            var entity = player.level().getEntityInAnyDimension(definition.entityId(pointId));
            if (entity instanceof OverlayMarker marker) marker.activated(true);
        }
        openList(player);
        return true;
    }
    public static TeleportListPayload list(ServerPlayer player) {
        var data = OverlaySavedData.get(player.level().getServer());
        var entries = new ArrayList<TeleportListPayload.Entry>();
        if (data.definition() != null) for (var point : data.definition().points())
            if (data.activated(player.getUUID(), point.id())) entries.add(new TeleportListPayload.Entry(point.id(), point.name(), point.statue()));
        return new TeleportListPayload(entries);
    }
    public static void openList(ServerPlayer player) {
        if (!ManagedWorld.isManaged(player.level()) || !player.isAlive() || player.isSpectator()) return;
        if (player.connection != null && player.connection.isAcceptingMessages())
            player.connection.send(new ClientboundCustomPayloadPacket(list(player)));
    }
    public static boolean teleport(ServerPlayer player, String pointId) {
        if (!ManagedWorld.isManaged(player.level()) || !player.isAlive() || player.isSpectator()) return false;
        var data = OverlaySavedData.get(player.level().getServer());
        var definition = data.definition();
        var point = definition == null ? null : definition.point(pointId);
        if (point == null || !data.activated(player.getUUID(), pointId)) return false;
        if (!CombatRuntime.get(player.level().getServer()).session(player).party().activeMember().alive()) return false;
        travel(player, definition, point.arrival());
        return true;
    }
    private static void travel(ServerPlayer player, OverlayDefinition definition, OverlayDefinition.Position position) {
        var runtime = CombatRuntime.get(player.level().getServer());
        runtime.session(player).endForTeleport();
        player.stopRiding();
        player.teleportTo(level(player.level().getServer(), definition), position.x(), position.y(), position.z(), Set.of(), position.yaw(), 0, false);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
    }
    public static void rememberWipe(ServerPlayer player) {
        var data = OverlaySavedData.get(player.level().getServer());
        var definition = data.definition();
        if (definition == null) return;
        OverlayDefinition.TravelPoint nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        if (player.level() == level(player.level().getServer(), definition)) for (var point : definition.points()) {
            if (!data.activated(player.getUUID(), point.id())) continue;
            double candidate = player.position().distanceToSqr(vec(point.position()));
            if (candidate < distance || candidate == distance && nearest != null && point.id().compareTo(nearest.id()) < 0) {
                distance = candidate;
                nearest = point;
            }
        }
        data.rememberWipe(player.getUUID(), nearest == null ? "" : nearest.id());
    }
    public static void respawn(ServerPlayer player) {
        var data = OverlaySavedData.get(player.level().getServer());
        var definition = data.definition();
        if (definition == null) return;
        String id = data.consumeWipe(player.getUUID());
        var point = id == null ? null : definition.point(id);
        travel(player, definition, point == null ? definition.arrival() : point.arrival());
    }
    public static void recoverNearStatue(ServerPlayer player, long tick) {
        if (!ManagedWorld.isManaged(player.level()) || OverlaySavedData.get(player.level().getServer()).definition() == null) return;
        SERVERS.computeIfAbsent(player.level().getServer(), ignored -> new OverlayRuntime()).recover(player, tick);
    }
    private void recover(ServerPlayer player, long tick) {
        var data = OverlaySavedData.get(player.level().getServer());
        var definition = data.definition();
        OverlayDefinition.TravelPoint nearby = null;
        if (player.isAlive() && !player.isSpectator() && player.level() == level(player.level().getServer(), definition))
            for (var point : definition.points()) if (point.statue()
                    && player.position().distanceToSqr(vec(point.position())) <= ADAPTED_HEAL_RADIUS * ADAPTED_HEAL_RADIUS) { nearby = point; break; }
        if (nearby == null) { dwell.remove(player.getUUID()); return; }
        var previous = dwell.get(player.getUUID());
        if (previous == null || !previous.statue.equals(nearby.id()) || previous.playerEntityId != player.getId()) {
            previous = new Dwell(nearby.id(), tick, player.getId());
            dwell.put(player.getUUID(), previous);
        }
        heal(player, tick - previous.since >= HEAL_DWELL_TICKS && data.activated(player.getUUID(), nearby.id()));
    }
    /** Locked Statues revive too. Reserve healing follows the separate two-second continuous dwell. */
    private static void heal(ServerPlayer player, boolean healFromReserve) {
        var data = OverlaySavedData.get(player.level().getServer());
        var session = CombatRuntime.get(player.level().getServer()).session(player);
        boolean changed = false;
        for (int slot = 0; slot < io.github.brainage04.genshininminecraft.rules.Party.SIZE; slot++) {
            if (!session.party().allocated(slot)) continue;
            var member = session.party().members().get(slot);
            if (!member.alive()) { member.setHp(Math.round(member.maxHp() * PartySave.WIPE_REVIVE_HP_FRACTION)); changed = true; }
            if (healFromReserve) {
                double amount = data.spend(member.maxHp() - member.hp());
                if (amount > 0) { member.setHp(member.hp() + amount); changed = true; }
            }
        }
        if (changed) session.resourcesChanged();
    }
}

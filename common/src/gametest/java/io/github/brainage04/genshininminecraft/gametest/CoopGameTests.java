package io.github.brainage04.genshininminecraft.gametest;

import com.mojang.authlib.GameProfile;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.*;
import io.github.brainage04.genshininminecraft.rules.*;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Two actual ServerPlayer entities exercise the shared authoritative adapters, on both loaders. */
public final class CoopGameTests {
    private CoopGameTests() {}
    private interface Pair { void run(CombatRuntime runtime, ServerPlayer host, ServerPlayer guest); }
    private static ServerPlayer player(GameTestHelper context, long id, Vec3 position) {
        var profile = new GameProfile(new UUID(id, id), "coop-test-" + id);
        var player = new ServerPlayer(context.getLevel().getServer(), context.getLevel(), profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(context.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(profile, false));
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        player.setGameMode(GameType.SURVIVAL); player.snapTo(position); player.setYRot(0);
        context.getLevel().addNewPlayer(player);
        return player;
    }
    private static void pair(GameTestHelper context, Pair test) {
        HilichurlGameTests.withManaged(context, (runtime, host) -> {
            runtime.session(host);
            ServerPlayer guest = player(context, 1, host.position().add(1, 0, 0));
            try { runtime.reconcileCoop(context.getLevel().getServer()); test.run(runtime, host, guest); }
            finally { runtime.forget(guest.getUUID()); context.getLevel().removePlayerImmediately(guest, Entity.RemovalReason.DISCARDED); }
        });
        context.succeed();
    }
    private static Hilichurl enemy(GameTestHelper context, Vec3 position) {
        var enemy = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
        enemy.setGenshinLevel(20); enemy.snapTo(position); enemy.setCamp(position, position); enemy.setNoAi(true);
        context.assertTrue(context.getLevel().addFreshEntity(enemy), "Co-op enemy spawned"); return enemy;
    }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .001, message + ": expected " + expected + ", actual " + actual);
    }
    public static void rosterMembershipRestoresSavedResourcesAndScalesExistingHp(GameTestHelper context) {
        pair(context, (runtime, host, guest) -> {
            var owner = runtime.session(host); var other = runtime.session(guest);
            context.assertValueEqual(owner.party().allocatedMask(), 3, "Two players allocate first two owned slots each");
            context.assertValueEqual(other.party().allocatedMask(), 3, "Overworld duplicate Traveler/Amber are permitted");
            context.assertFalse(runtime.receive(host, Intent.SWITCH_3), "Dormant Kaeya is not an extra combat slot");
            owner.party().kit(2).setHp(1234.5); owner.party().kit(2).grantEnergy(17);
            context.assertTrue(runtime.pickRoster(host, 12), "Owner can choose Kaeya/Lisa outside combat");
            context.assertValueEqual(owner.party().activeSlot(), 2, "Removed active body replaced by first living allocated slot");
            var enemy = enemy(context, host.position().add(0, 0, 2)); var target = runtime.target(enemy);
            close(context, target.maxHp(), 1327.8000024, "Two-player HP =13.584×65.1649×1.50 (datamine.md)");
            enemy.setHealth(10);
            var third = player(context, 2, host.position()); var fourth = player(context, 3, host.position());
            try {
                context.getLevel().removePlayerImmediately(fourth, Entity.RemovalReason.DISCARDED);
                runtime.reconcileCoop(context.getLevel().getServer());
                context.assertValueEqual(owner.party().allocatedCount(), 2, "Three-player host gets two");
                context.assertValueEqual(other.party().allocatedCount(), 1, "Three-player guest gets one");
                close(context, target.maxHp(), 1770.4000032, "Three-player HP ×2.00"); close(context, target.hp(), 885.2000016, "Existing enemy preserves half HP");
                fourth = player(context, 4, host.position()); runtime.reconcileCoop(context.getLevel().getServer());
                context.assertValueEqual(owner.party().allocatedCount(), 1, "Four-player host gets one");
                close(context, target.maxHp(), 2213.000004, "Four-player HP ×2.50, not historical ATK scaling");
            } finally {
                runtime.forget(third.getUUID()); runtime.forget(fourth.getUUID());
                context.getLevel().removePlayerImmediately(third, Entity.RemovalReason.DISCARDED);
                context.getLevel().removePlayerImmediately(fourth, Entity.RemovalReason.DISCARDED);
            }
            context.getLevel().removePlayerImmediately(guest, Entity.RemovalReason.DISCARDED); runtime.forget(guest.getUUID());
            runtime.reconcileCoop(context.getLevel().getServer());
            context.assertValueEqual(owner.party().allocatedMask(), 15, "Leaving restores all four owned members");
            close(context, owner.party().kit(2).hp(), 1234.5, "Dormant HP unchanged"); close(context, owner.party().kit(2).energy(), 17, "Dormant energy unchanged");
            close(context, target.maxHp(), 885.2000016, "Existing enemy returns to solo ×1.00"); close(context, target.hp(), 442.6000008, "Leaving cannot reset/heal enemy");
            runtime.forget(host.getUUID()); var restored = runtime.session(host);
            close(context, restored.party().kit(2).hp(), 1234.5, "Same UUID restores item22 saved HP");
            close(context, restored.party().kit(2).energy(), 17, "Rejoin preserves item22 energy"); enemy.discard();
        });
    }
    public static void particlesRouteNearbyOwnAllocationsAndReactionUsesTriggerOwner(GameTestHelper context) {
        pair(context, (runtime, host, guest) -> {
            var owner = runtime.session(host); var other = runtime.session(guest);
            context.assertTrue(runtime.pickRoster(guest, 6), "Guest chooses Amber/Kaeya");
            var enemy = enemy(context, host.position().add(0, 0, 2)); long start = owner.party().frame();
            context.assertTrue(owner.intent(Intent.SKILL_PRESS, start), "Battery skill accepted"); owner.intent(Intent.SKILL_RELEASE, start);
            owner.advanceTo(start + 32);
            close(context, owner.party().kit(0).energy(), 6, "On-field matching: two ×3");
            close(context, owner.party().kit(1).energy(), 1.6, "Own two-member off-field: two ×0.8");
            close(context, other.party().kit(1).energy(), 2, "Nearby guest on-field different: two ×1");
            close(context, other.party().kit(2).energy(), 1.6, "Nearby guest off-field different: two ×0.8");
            close(context, other.party().kit(0).energy(), 0, "Dormant Traveler never receives particles");
            guest.snapTo(host.position().add(40, 0, 0));
            owner.intent(Intent.SKILL_PRESS, start + 400); owner.intent(Intent.SKILL_RELEASE, start + 400); owner.advanceTo(start + 432);
            close(context, other.party().kit(1).energy(), 2, "No session-wide battery credit beyond adapted32 blocks");
            guest.snapTo(host.position());
            owner.advanceTo(start + 800);
            context.assertTrue(runtime.pickRoster(host, 5), "Choose Traveler/Kaeya after adapted out-of-combat delay");
            context.assertTrue(owner.intent(Intent.SWITCH_3, start + 800), "Select own Kaeya");
            context.assertTrue(owner.intent(Intent.SKILL_PRESS, start + 800), "Host's real Frostgnaw applies shared Cryo");
            owner.advanceTo(start + 828);
            context.assertTrue(runtime.target(enemy).aura().gauge(Element.CRYO) > 0, "Another player can consume host skill aura");
            double before = runtime.target(enemy).hp();
            context.assertTrue(other.intent(Intent.ATTACK_PRESS, start + 900), "Guest Amber aims on another owner's Cryo aura");
            other.intent(Intent.ATTACK_RELEASE, start + 1000); other.advanceTo(start + 1000);
            // datamine.md: (18.6984*2.569 + 37.6075*2.275), unascended20/20; same Melt/DEF/RES/Slingshot factors.
            close(context, before - runtime.target(enemy).hp(), (48.0361896 + 85.5570625) * 1.24 * 2 * .5 * .9 * 1.36,
                    "Cross-player Melt uses the trigger's own stats, not the aura applier");
            context.assertValueEqual(enemy.getLastHurtByPlayer(), guest, "Reaction attack credited to guest");
            enemy.discard();
        });
    }
    public static void everyStarterDamagePathExcludesTeammateAndPuppet(GameTestHelper context) {
        pair(context, (runtime, host, guest) -> {
            var owner = runtime.session(host); var other = runtime.session(guest);
            var dummy = new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW, context.getLevel());
            dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000); dummy.setHealth(1000000); dummy.setNoAi(true);
            dummy.snapTo(host.position().add(0, 0, 2)); context.getLevel().addFreshEntity(dummy);
            var puppet = new BaronBunny(GenshinEntities.BARON_BUNNY, context.getLevel()); puppet.setNoAi(true);
            puppet.snapTo(dummy.position()); context.getLevel().addFreshEntity(puppet); guest.snapTo(dummy.position());
            double hp = other.kit().hp(); float bunnyHp = puppet.getHealth();
            try {
                long frame = owner.party().frame();
                for (int slot = 0; slot < 4; slot++) {
                    owner.party().allocate((1 << slot) | (1 << ((slot + 1) % 4)), frame);
                    if (owner.party().activeSlot() != slot) owner.party().switchTo(slot, frame + 60);
                    owner.resourcesChanged(); frame += 120;
                    context.assertTrue(owner.intent(Intent.ATTACK_PRESS, frame), "Normal accepted for kit" + slot);
                    owner.intent(Intent.ATTACK_RELEASE, frame); owner.advanceTo(frame + 300);
                    context.assertTrue(owner.intent(Intent.ATTACK_PRESS, frame + 400), "Charged accepted for kit" + slot);
                    owner.intent(Intent.ATTACK_RELEASE, frame + 500); owner.advanceTo(frame + 800);
                    context.assertTrue(owner.intent(Intent.SKILL_PRESS, frame + 1000), "Skill accepted for kit" + slot);
                    owner.intent(Intent.SKILL_RELEASE, frame + 1120); owner.advanceTo(frame + 1900);
                    owner.kit().grantEnergy(80);
                    context.assertTrue(owner.intent(Intent.BURST_PRESS, frame + 2000), "Burst accepted for kit" + slot);
                    owner.advanceTo(frame + 3100); frame += 4000;
                    close(context, other.kit().hp(), hp, "Normal/charged/skill/burst/fields never hurt guest for kit" + slot);
                    close(context, puppet.getHealth(), bunnyHp, "Every target shape excludes even an untracked player puppet for kit" + slot);
                }
                context.assertFalse(guest.hurtServer(context.getLevel(), context.getLevel().damageSources().playerAttack(host), 10), "Direct vanilla melee exclusion");
                context.assertFalse(puppet.hurtServer(context.getLevel(), context.getLevel().damageSources().playerAttack(host), 10), "Direct vanilla puppet exclusion");
                var arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW, context.getLevel()); arrow.setOwner(host);
                context.assertFalse(guest.hurtServer(context.getLevel(), context.getLevel().damageSources().arrow(arrow, host), 10), "Player-owned projectile exclusion");
                context.assertFalse(puppet.hurtServer(context.getLevel(), context.getLevel().damageSources().explosion(host, host), 10), "Player-owned direct explosion exclusion");
                context.assertFalse(runtime.enemyHit(guest, host, 100, 1, 20), "Enemy adapter cannot bypass ally exclusion");
                context.assertTrue(puppet.hurtServer(context.getLevel(), context.getLevel().damageSources().generic(), 1), "Environmental damage is not blanket ally immunity");
                context.assertTrue(runtime.target(dummy).hp() < runtime.target(dummy).maxHp(), "Real attacks still damage overlapping enemy");
            } finally { dummy.discard(); puppet.discard(); }
        });
    }
    public static void statuesSpendOneHostReserveForBothAllocatedRosters(GameTestHelper context) {
        pair(context, (runtime, host, guest) -> {
            var server = host.level().getServer();
            var original = io.github.brainage04.genshininminecraft.world.OverlaySavedData.get(server);
            var data = new io.github.brainage04.genshininminecraft.world.OverlaySavedData();
            server.overworld().getDataStorage().set(io.github.brainage04.genshininminecraft.world.OverlaySavedData.TYPE, data);
            try {
                data.install(OverlayGameTests.fixture(host));
                data.activate(host.getUUID(), "statue", 1); data.activate(guest.getUUID(), "statue", 1);
                close(context, data.reserve(), 5000, "Both activations create one physical Statue reserve");
                for (var player : new ServerPlayer[]{host, guest}) {
                    var session = runtime.session(player);
                    session.party().kit(0).setHp(session.party().kit(0).maxHp() - 1000);
                    session.party().kit(2).setHp(session.party().kit(2).maxHp() - 400);
                    session.resourcesChanged();
                    io.github.brainage04.genshininminecraft.world.OverlayRuntime.recoverNearStatue(player, 0);
                }
                io.github.brainage04.genshininminecraft.world.OverlayRuntime.recoverNearStatue(host, 40);
                io.github.brainage04.genshininminecraft.world.OverlayRuntime.recoverNearStatue(guest, 40);
                close(context, data.reserve(), 3000, "Host and guest each spend1000 from the same world pool");
                for (var player : new ServerPlayer[]{host, guest}) {
                    var party = runtime.session(player).party();
                    close(context, party.kit(0).hp(), party.kit(0).maxHp(), "Eligible allocated member healed");
                    close(context, party.kit(2).hp(), party.kit(2).maxHp() - 400, "Dormant account member is not silently healed");
                }
            } finally { server.overworld().getDataStorage().set(io.github.brainage04.genshininminecraft.world.OverlaySavedData.TYPE, original); }
        });
    }
    @SuppressWarnings("unchecked")
    private static List<ServerPlayer> connectedPlayers(net.minecraft.server.MinecraftServer server) {
        try {
            // NeoForge exposes an unmodifiable view; only this socket-free test fixture mutates the backing list.
            var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
            field.setAccessible(true);
            return (List<ServerPlayer>) field.get(server.getPlayerList());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot register connected mock player", exception);
        }
    }
    public static void connectedRemovedCorpseKeepsAllocationUntilNativeRespawn(GameTestHelper context) {
        pair(context, (runtime, corpse, survivor) -> {
            var server = context.getLevel().getServer();
            var connected = connectedPlayers(server);
            connected.add(corpse);
            var enemy = enemy(context, corpse.position().add(0, 0, 2));
            ServerPlayer third = null;
            try {
                context.assertTrue(runtime.pickRoster(corpse, 5), "First participant selects Traveler/Kaeya");
                var state = runtime.session(corpse);
                double survivorHp = runtime.session(survivor).kit().hp();
                state.party().kit(0).setHp(0); state.party().kit(2).setHp(0); state.resourcesChanged();
                corpse.die(context.getLevel().damageSources().genericKill());
                if (!corpse.isRemoved()) context.getLevel().removePlayerImmediately(corpse, Entity.RemovalReason.KILLED);
                context.assertFalse(context.getLevel().players().contains(corpse), "Dead body leaves level player list");
                for (int tick = 0; tick < 25; tick++) runtime.tick(server);
                context.assertTrue(runtime.session(corpse) == state, "Connected removed corpse retains original session");
                context.assertValueEqual(state.party().allocatedMask(), 5, "Death cannot allocate dormant survivors");
                context.assertValueEqual(runtime.coopCount(), 2, "A dead connected participant still counts in co-op");
                context.assertTrue(state.party().wiped() && corpse.getHealth() == 0 && !corpse.isAlive(), "Native zero HP remains eligible for ordinary respawn");
                close(context, runtime.target(enemy).maxHp(), 1327.8000024, "Death cannot downscale the shared enemy");
                corpse.connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(
                        net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                var replacement = corpse.connection.player;
                context.assertTrue(replacement != corpse && replacement.isAlive(), "Native respawn creates a living replacement");
                var revived = runtime.session(replacement).party();
                context.assertValueEqual(revived.allocatedMask(), 5, "Native replacement keeps chosen allocation");
                for (int slot = 0; slot < 4; slot++)
                    close(context, revived.kit(slot).hp(), revived.allocated(slot) ? Math.round(revived.kit(slot).maxHp() * .35)
                            : revived.kit(slot).maxHp(), "Only allocated members revive; dormant resources survive");
                close(context, runtime.session(survivor).kit().hp(), survivorHp, "Teammate keeps playing through native host respawn");
                third = player(context, 2, survivor.position().add(4, 0, 0));
                runtime.reconcileCoop(server);
                context.assertValueEqual(revived.allocatedCount(), 2, "Respawn does not demote earliest connected host for three players");
                context.assertValueEqual(runtime.session(survivor).party().allocatedCount(), 1, "Independent guest remains the one-slot participant");
            } finally {
                if (third != null) {
                    runtime.forget(third.getUUID());
                    third.level().removePlayerImmediately(third, Entity.RemovalReason.DISCARDED);
                }
                var replacement = corpse.connection.player;
                connected.remove(corpse); connected.remove(replacement);
                server.getPlayerList().getPlayersByUUID().remove(corpse.getUUID(), replacement);
                runtime.forget(corpse.getUUID());
                replacement.level().removePlayerImmediately(replacement, Entity.RemovalReason.DISCARDED);
                enemy.discard();
            }
        });
    }
    public static void aggroTransfersWithoutLeashResetAndLocalWipeDoesNotKillGuest(GameTestHelper context) {
        pair(context, (runtime, host, guest) -> {
            var enemy = enemy(context, host.position().add(0, 0, 5)); enemy.setNoAi(false);
            guest.snapTo(host.position().add(0, 0, -3)); HilichurlGameTests.tick(context, enemy);
            context.assertValueEqual(enemy.getTarget(), host, "Nearest eligible owner acquires aggro");
            double max = runtime.target(enemy).maxHp(); enemy.setHealth(10);
            guest.snapTo(enemy.position().add(0, 0, -3));
            context.assertTrue(enemy.distanceToSqr(guest) < enemy.distanceToSqr(host), "Relocated guest really is closer");
            context.assertTrue(enemy.hasLineOfSight(guest), "Aggro transfer fixture has unobstructed sight");
            for (int tick = 0; tick < 2; tick++) HilichurlGameTests.tick(context, enemy);
            context.assertValueEqual(enemy.getTarget(), guest, "Closer guest can take aggro");
            host.snapTo(enemy.campAnchor().add(30, 0, 0));
            for (int tick = 0; tick < 2; tick++) HilichurlGameTests.tick(context, enemy);
            context.assertFalse(enemy.isReturningToCamp(), "One owner outside leash cannot reset guest's encounter");
            close(context, enemy.getHealth(), 10, "Transfer does not heal encounter");
            enemy.snapTo(enemy.campAnchor().add(0, 0, 2)); // Make the return observable instead of already standing at home.
            guest.snapTo(enemy.campAnchor().add(30, 0, 0));
            for (int tick = 0; tick < 2; tick++) HilichurlGameTests.tick(context, enemy);
            context.assertTrue(enemy.isReturningToCamp(), "No eligible owner inside leash resets camp");
            close(context, runtime.target(enemy).hp(), max, "Only actual leash reset heals");
            var owner = runtime.session(host); var other = runtime.session(guest);
            enemy.setNoAi(true);
            guest.snapTo(enemy.campAnchor().add(0, 0, -3));
            long frame = other.party().frame();
            context.assertTrue(other.intent(Intent.SWITCH_2, frame), "Guest owns Amber, not a host puppet");
            context.assertTrue(other.intent(Intent.SKILL_PRESS, frame), "Guest casts real Baron Bunny");
            other.advanceTo(frame + 45);
            var bunny = context.getLevel().getEntitiesOfClass(BaronBunny.class, guest.getBoundingBox().inflate(8)).getFirst();
            var taunted = enemy(context, bunny.position().add(0, 0, 1.5)); taunted.setNoAi(false);
            HilichurlGameTests.tick(context, taunted);
            context.assertValueEqual(taunted.getTarget(), bunny, "Any player's Bunny taunts the shared camp");
            taunted.discard();
            double guestHp = other.kit().hp();
            owner.party().kit(0).setHp(0); owner.party().kit(1).setHp(0); owner.resourcesChanged();
            context.assertTrue(owner.party().wiped(), "Dormant living Kaeya/Lisa cannot prevent allocated wipe");
            runtime.respawn(host);
            close(context, owner.party().kit(0).hp(), Math.round(2342 * .35), "Local allocated wipe revives at rounded35%");
            close(context, other.kit().hp(), guestHp, "Guest survives unchanged");
            close(context, owner.party().kit(2).hp(), owner.party().kit(2).maxHp(), "Dormant member is neither killed nor healed");
            enemy.discard();
        });
    }
}

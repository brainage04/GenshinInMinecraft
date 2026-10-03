package io.github.brainage04.genshininminecraft.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit.Intent;
import io.github.brainage04.genshininminecraft.world.ManagedWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real command, profile and entity/server tick paths; expectations are independent spec arithmetic. */
public final class HilichurlGameTests {
    private HilichurlGameTests() {}

    public static void campCommandAndSpecDamage(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            context.assertFalse(context.getLevel().getGameRules().get(GameRules.SPAWN_MOBS), "Managed natural spawning is disabled");
            command(context, source(context, player), "genshin camp hilichurl");
            var camp = members(context, player);
            context.assertValueEqual(camp.size(), 3, "Historical introductory camp count is three");
            for (Hilichurl member : camp) {
                var target = runtime.target(member);
                close(context, target.maxHp(), 885.200, "hilichurls.md rounded Lv20 baseline");
                close(context, target.hp(), 885.200, "Command-spawned camp starts full");
                close(context, target.defense(), 5 * 20 + 500, "Spec level-scaled DEF");
                context.assertValueEqual(target.level(), 20, "Camp level matches the Lv20 Traveler");
                for (Element element : Element.values()) close(context, target.resistance(element), .10, "Spec all-element RES");
                context.assertValueEqual(member.campAnchor(), player.position(), "Shared camp anchor is the command player's position");
            }
            for (int index = 1; index < camp.size(); index++) camp.get(index).discard();
            Hilichurl member = camp.getFirst();
            member.snapTo(player.position().add(0, 0, 2));
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            context.assertTrue(runtime.receive(player, Intent.SKILL_PRESS), "Traveler skill accepted");
            context.assertTrue(runtime.receive(player, Intent.SKILL_RELEASE), "Tap release accepted");
            runtime.session(player).advanceTo(start + 31);
            close(context, runtime.target(member).hp(), 885.200, "No damage before Palm Vortex hitmark");
            runtime.session(player).advanceTo(start + 32);
            // damage.md Lv20 ATK45.75 + training sword23, traveler-anemo.md storm176%, enemy DEF600/RES10%.
            double expected = (45.75 + 23) * 1.76 * (5 * (20.0 + 100) / (5 * (20 + 100) + 600)) * (1 - .10);
            close(context, runtime.target(member).hp(), 885.200 - expected, "Traveler applies sourced Genshin damage, not vanilla fallback HP");
            close(context, member.getHealth(), 20 * (885.200 - expected) / 885.200, "Vanilla health mirrors the spec profile fraction");
            try {
                command(context, source(context, player).withPermission(LevelBasedPermissionSet.ALL), "genshin camp hilichurl 1");
                throw new AssertionError("Unprivileged camp command must be rejected");
            } catch (AssertionError expectedDenial) {
                if (!(expectedDenial.getCause() instanceof CommandSyntaxException)) throw expectedDenial;
            }
        });
        context.succeed();
    }

    public static void aggroWindupAndCharacterDamage(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            command(context, source(context, player), "genshin camp hilichurl 1");
            Hilichurl member = members(context, player).getFirst();
            player.snapTo(member.position().add(0, 0, 1.5));
            player.setGameMode(GameType.CREATIVE);
            tick(context, member);
            context.assertTrue(member.getTarget() == null, "Creative players cannot aggro the camp");
            player.setGameMode(GameType.SPECTATOR);
            tick(context, member);
            context.assertTrue(member.getTarget() == null, "Spectators cannot aggro the camp");
            player.setGameMode(GameType.SURVIVAL);
            var session = runtime.session(player);
            tick(context, member);
            context.assertValueEqual(member.getTarget(), player, "Nearby survival player is detected by the entity AI");
            context.assertTrue(member.isWindingUp(), "Visible raised-club telegraph starts before damage");
            close(context, session.kit().hp(), 2342.39, "Telegraph start must not instantly damage Genshin HP");
            for (int tick = 1; tick < 10; tick++) {
                tick(context, member);
                close(context, session.kit().hp(), 2342.39, "No damage during the first nine wind-up ticks");
            }
            tick(context, member);
            // Named ATK adaptation120; sourced Fighter100%; incoming damage.md DEF600/(DEF147.01+600), RES0%.
            double expected = 120 * 1.0 * (5 * 20.0 + 500) / (147.01 + 5 * 20 + 500) * (1 - 0);
            close(context, session.kit().hp(), 2342.39 - expected, "Club hit damages character Genshin HP after half-second wind-up");
            close(context, player.getHealth(), 20 * (2342.39 - expected) / 2342.39, "Genshin damage mirrors to vanilla health");
            context.assertFalse(member.isWindingUp(), "Wind-up ends when the strike lands");
            for (int tick = 0; tick < 29; tick++) tick(context, member);
            close(context, session.kit().hp(), 2342.39 - expected, "Recovery prevents repeated instant melee damage");
        });
        context.succeed();
    }

    public static void leashHealsClearsAuraAndReturns(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            command(context, source(context, player), "genshin camp hilichurl 1");
            Hilichurl member = members(context, player).getFirst();
            Vec3 home = member.idlePosition();
            member.snapTo(home.add(0, 0, 2));
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            runtime.receive(player, Intent.SKILL_PRESS);
            runtime.receive(player, Intent.SKILL_RELEASE);
            runtime.session(player).advanceTo(start + 32);
            var previous = runtime.target(member);
            previous.aura().applyHit(Element.PYRO, 2, start + 32);
            context.assertTrue(previous.hp() < 885.200, "Precondition: camp member was damaged");
            tick(context, member);
            context.assertValueEqual(member.getTarget(), player, "Camp aggros before leashing");
            member.snapTo(home.add(0, 0, 6));
            player.teleportTo(home.x + 30, home.y, home.z);
            tick(context, member);
            context.assertTrue(member.getTarget() == null, "Target outside anchor leash is dropped");
            context.assertTrue(member.isReturningToCamp(), "Leashed member walks home instead of immediately reacquiring");
            var reset = runtime.target(member);
            context.assertTrue(reset != previous, "Reset discards the complete old combat/aura/ICD state");
            close(context, reset.hp(), 885.200, "Leash restores full spec HP");
            close(context, member.getHealth(), 20, "Leash restores the vanilla mirror");
            context.assertValueEqual(reset.auraElements(), 0, "Leash clears aura indicators");
            context.assertFalse(member.isWindingUp(), "Leash cancels a pending melee strike");
            for (int tick = 0; tick < 180 && member.isReturningToCamp(); tick++) tick(context, member);
            context.assertFalse(member.isReturningToCamp(), "Camp navigation completes the leash return: position="
                    + member.position() + ", home=" + home + ", path=" + member.getNavigation().getPath());
            context.assertTrue(member.position().distanceToSqr(home) <= .75 * .75, "Member returns to its own camp idle point");
        });
        context.succeed();
    }

    public static void clubMissAndPlayerDeath(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            command(context, source(context, player), "genshin camp hilichurl 1");
            Hilichurl member = members(context, player).getFirst();
            Vec3 home = member.position();
            player.snapTo(home.add(0, 0, 1.5));
            var session = runtime.session(player);
            tick(context, member);
            player.snapTo(home.add(0, 0, -1.5)); // Dodge behind the fixed swing facing.
            for (int tick = 0; tick < 10; tick++) tick(context, member);
            close(context, session.kit().hp(), 2342.39, "Moving behind the telegraphed arc avoids the strike");
            session.kit().setHp(1);
            player.snapTo(home.add(0, 0, 1.5));
            for (int tick = 0; tick < 41 && player.isAlive(); tick++) tick(context, member);
            close(context, session.kit().hp(), 0, "Lethal enemy hit reaches zero Genshin HP");
            context.assertFalse(player.isAlive(), "Zero character HP uses vanilla player death");
            context.assertValueEqual(player.getKillCredit(), member, "Enemy receives normal vanilla kill attribution");
        });
        context.succeed();
    }

    static void tick(GameTestHelper context, Hilichurl member) { context.getLevel().tickNonPassenger(member); }
    private static List<Hilichurl> members(GameTestHelper context, ServerPlayer player) {
        return context.getLevel().getEntitiesOfClass(Hilichurl.class, new AABB(player.position(), player.position()).inflate(4));
    }
    private static CommandSourceStack source(GameTestHelper context, ServerPlayer player) {
        return context.getLevel().getServer().createCommandSourceStack().withLevel(context.getLevel()).withEntity(player)
                .withPosition(player.position()).withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
    }
    private static int command(GameTestHelper context, CommandSourceStack source, String command) {
        try { return context.getLevel().getServer().getCommands().getDispatcher().execute(command, source); }
        catch (CommandSyntaxException exception) { throw new AssertionError("Command failed: " + command, exception); }
    }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .0001, message + ": expected " + expected + ", actual " + actual);
    }
    static void withManaged(GameTestHelper context, BiConsumer<CombatRuntime, ServerPlayer> test) {
        var level = context.getLevel();
        var server = level.getServer();
        var original = ManagedWorldData.get(server);
        var rules = server.getGameRules().copy(server.overworld().enabledFeatures());
        var data = new ManagedWorldData();
        server.overworld().getDataStorage().set(ManagedWorldData.TYPE, data);
        CombatRuntime.stop(server);
        var profile = new GameProfile(new UUID(0, 0), "hilichurl-test");
        var player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(profile, false));
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        player.setGameMode(GameType.SURVIVAL);
        player.snapTo(context.absoluteVec(new Vec3(1, 2, 1)));
        player.setYRot(0);
        BlockPos floor = player.blockPosition().below();
        List<BlockState> saved = new ArrayList<>(17 * 17);
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            BlockPos pos = floor.offset(x, 0, z);
            saved.add(level.getBlockState(pos));
            level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        try {
            data.setManaged(server, true);
            level.addNewPlayer(player);
            test.accept(CombatRuntime.get(server), player);
        } finally {
            for (Hilichurl member : level.getEntitiesOfClass(Hilichurl.class, new AABB(Vec3.atCenterOf(floor), Vec3.atCenterOf(floor)).inflate(12))) member.discard();
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            int index = 0;
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
                level.setBlock(floor.offset(x, 0, z), saved.get(index++), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            CombatRuntime.stop(server);
            server.getGameRules().setAll(rules, server);
            server.overworld().getDataStorage().set(ManagedWorldData.TYPE, original);
        }
    }
}

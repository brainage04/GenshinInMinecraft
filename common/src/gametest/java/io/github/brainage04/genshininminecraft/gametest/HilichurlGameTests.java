package io.github.brainage04.genshininminecraft.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.AmberKit;
import io.github.brainage04.genshininminecraft.rules.kit.KaeyaKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
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
import io.github.brainage04.genshininminecraft.enemy.BaronBunny;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

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
                close(context, target.maxHp(), 237.820, "hilichurls.md rounded Lv8 baseline");
                close(context, target.hp(), 237.820, "Command-spawned camp starts full");
                close(context, target.defense(), 5 * 8 + 500, "Spec level-scaled DEF");
                context.assertValueEqual(target.level(), 8, "Named camp-level adaptation, independent of Lv20 party");
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
            close(context, runtime.target(member).hp(), 237.820, "No damage before Palm Vortex hitmark");
            runtime.session(player).advanceTo(start + 32);
            // damage.md: ATK45.75 + Harbinger94; storm176%; Lv20→Lv8 DEF120/228; RES10%.
            double expected = (45.75 + 94) * 1.76 * (120.0 / 228) * (1 - .10);
            close(context, runtime.target(member).hp(), 237.820 - expected, "Traveler applies sourced Genshin damage, not vanilla fallback HP");
            close(context, member.getHealth(), 20 * (237.820 - expected) / 237.820, "Vanilla health mirrors the spec profile fraction");
            member.discard();
            command(context, source(context, player), "genshin camp hilichurl 1 20");
            var level20 = members(context, player).getFirst();
            close(context, runtime.target(level20).maxHp(), 885.200, "Explicit level20 keeps the published HP unchanged");
            context.assertValueEqual(level20.genshinLevel(), 20, "Command supplies per-spawn entity level");
            close(context, runtime.target(level20).defense(), 600, "Explicit level20 DEF remains600");
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, context.getLevel().registryAccess());
            context.assertTrue(level20.save(output), "Persistent camp member saves its spawn level");
            level20.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            var reloaded = EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,
                    context.getLevel().registryAccess(), output.buildResult()), context.getLevel(), EntitySpawnReason.LOAD, entity -> entity);
            context.assertTrue(reloaded instanceof Hilichurl && context.getLevel().addFreshEntity(reloaded), "Camp member reloads");
            context.assertValueEqual(((Hilichurl) reloaded).genshinLevel(), 20, "Per-spawn level survives entity persistence");
            close(context, runtime.target((Hilichurl) reloaded).maxHp(), 885.200, "Reloaded HP comes from the saved level");
            reloaded.discard();
            var legacyTag = output.buildResult();
            legacyTag.remove("GenshinLevel");
            var legacy = EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,
                    context.getLevel().registryAccess(), legacyTag), context.getLevel(), EntitySpawnReason.LOAD, entity -> entity);
            context.assertTrue(legacy instanceof Hilichurl && context.getLevel().addFreshEntity(legacy), "Legacy camp reloads");
            context.assertValueEqual(((Hilichurl) legacy).genshinLevel(), 20, "Legacy camp anchor distinguishes an old Lv20 save");
            legacy.discard();
            command(context, source(context, player), "summon genshininminecraft:hilichurl ~ ~ ~");
            var summoned = members(context, player).getFirst();
            context.assertValueEqual(summoned.genshinLevel(), 8, "Fresh summon NBT uses the named Lv8 default, not legacy Lv20");
            close(context, runtime.target(summoned).maxHp(), 237.820, "Fresh summon shares the camp's default HP row");
            summoned.discard();
            for (int invalid : new int[]{0, 21}) {
                try {
                    command(context, source(context, player), "genshin camp hilichurl 1 " + invalid);
                    throw new AssertionError("Unsourced spawn level must be rejected");
                } catch (AssertionError denial) {
                    if (!(denial.getCause() instanceof CommandSyntaxException)) throw denial;
                }
            }
            try {
                command(context, source(context, player).withPermission(LevelBasedPermissionSet.ALL), "genshin camp hilichurl 1");
                throw new AssertionError("Unprivileged camp command must be rejected");
            } catch (AssertionError expectedDenial) {
                if (!(expectedDenial.getCause() instanceof CommandSyntaxException)) throw expectedDenial;
            }
        });
        context.succeed();
    }

    public static void liveNormalsRollSeededCritsAndUpdateHarbingerHpCondition(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            var enemy = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
            enemy.setGenshinLevel(20);
            enemy.setNoAi(true);
            enemy.snapTo(player.position().add(0, 0, 2));
            context.assertTrue(context.getLevel().addFreshEntity(enemy), "Seeded-crit target spawned");
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            int[] recovery = {23, 32, 40, 49, 81}, hitmarks = {13, 13, 16, 30, 25};
            double[] talent = {.445, .434, .530, .583, .708}; // traveler-anemo.md talent1.
            // withManaged uses UUID(0,0), so live Random(0) draws12/13/14=.128897/.146602/.023238.
            // At full HP CR=.05+.14=.19 and CD=.50+.18=.68: precisely these hits crit at1.68×.
            // After hit14, HP=90% disables Harbinger: draw17=.104491 must NOT crit at base5%.
            for (int hit = 0; hit < 17; hit++) {
                if (hit == 14) session.kit().setHp(session.kit().maxHp() * .90);
                double before = target.hp();
                context.assertTrue(session.intent(Intent.ATTACK_PRESS, start), "Live normal intent accepted");
                session.intent(Intent.ATTACK_RELEASE, start);
                session.advanceTo(start + hitmarks[hit % 5]);
                double expected = (45.75 + 94) * talent[hit % 5] * .5 * .9
                        * (hit >= 11 && hit <= 13 ? 1.68 : 1);
                close(context, before - target.hp(), expected, "Live hit" + (hit + 1) + " rolls individual crit, never expectation");
                start += recovery[hit % 5];
            }
            enemy.discard();
        });
        context.succeed();
    }

    public static void aggroWindupAndCharacterDamage(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            command(context, source(context, player), "genshin camp hilichurl 1 20");
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
            context.assertValueEqual(member.visualPhase(), io.github.brainage04.genshininminecraft.rules.EnemyAnimations.Phase.TELEGRAPH,
                    "Actual windup publishes its durable geo telegraph phase");
            int visualOccurrence = member.visualOccurrence();
            long visualStart = member.visualStartFrame();
            close(context, session.kit().hp(), 2342.39, "Telegraph start must not instantly damage Genshin HP");
            for (int tick = 1; tick < 10; tick++) {
                tick(context, member);
                close(context, session.kit().hp(), 2342.39, "No damage during the first nine wind-up ticks");
                context.assertValueEqual(member.visualOccurrence(), visualOccurrence, "Windup does not restart on each AI tick");
                context.assertValueEqual(member.visualStartFrame(), visualStart, "Windup keeps its server start frame");
            }
            tick(context, member);
            // Named ATK adaptation120; sourced Fighter100%; incoming damage.md DEF600/(DEF147.01+600), RES0%.
            double expected = 120 * 1.0 * (5 * 20.0 + 500) / (147.01 + 5 * 20 + 500) * (1 - 0);
            close(context, session.kit().hp(), 2342.39 - expected, "Club hit damages character Genshin HP after half-second wind-up");
            close(context, player.getHealth(), 20 * (2342.39 - expected) / 2342.39, "Genshin damage mirrors to vanilla health");
            context.assertFalse(member.isWindingUp(), "Wind-up ends when the strike lands");
            context.assertValueEqual(member.visualPhase(), io.github.brainage04.genshininminecraft.rules.EnemyAnimations.Phase.STRIKE,
                    "Impact pose publishes exactly on the existing tenth windup tick");
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
            context.assertTrue(previous.hp() < 237.820, "Precondition: level8 camp member was damaged");
            tick(context, member);
            context.assertValueEqual(member.getTarget(), player, "Camp aggros before leashing");
            member.snapTo(home.add(0, 0, 6));
            player.teleportTo(home.x + 30, home.y, home.z);
            tick(context, member);
            context.assertTrue(member.getTarget() == null, "Target outside anchor leash is dropped");
            context.assertTrue(member.isReturningToCamp(), "Leashed member walks home instead of immediately reacquiring");
            var reset = runtime.target(member);
            context.assertTrue(reset != previous, "Reset discards the complete old combat/aura/ICD state");
            close(context, reset.hp(), 237.820, "Leash restores full level8 spec HP");
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
            // A one-character fall now replaces the active member; vanilla death requires a party wipe.
            for (int slot = 1; slot < 4; slot++) session.party().members().get(slot).setHp(0);
            player.snapTo(home.add(0, 0, 1.5));
            for (int tick = 0; tick < 41 && player.isAlive(); tick++) tick(context, member);
            close(context, session.kit().hp(), 0, "Lethal enemy hit reaches zero Genshin HP");
            context.assertFalse(player.isAlive(), "Zero character HP uses vanilla player death");
            context.assertValueEqual(player.getKillCredit(), member, "Enemy receives normal vanilla kill attribution");
        });
        context.succeed();
    }

    public static void sameTickLethalTradeEarlierTravelerWins(GameTestHelper context) {
        context.runAfterDelay(6, () -> {
            sameTickEarlierTravelerHit(context, true);
            context.succeed();
        });
    }

    public static void sameTickEarlierTravelerHitPreventsClub(GameTestHelper context) {
        context.runAfterDelay(6, () -> {
            sameTickEarlierTravelerHit(context, false);
            context.succeed();
        });
    }

    private static void sameTickEarlierTravelerHit(GameTestHelper context, boolean lethalClub) {
        withManaged(context, (runtime, player) -> {
            var member = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
            member.snapTo(player.position().add(0, 0, 1.5));
            member.setCamp(member.position(), member.position());
            member.setHealth(.1F); // Less HP than even a non-critical Traveler N1.
            context.assertTrue(context.getLevel().addFreshEntity(member), "Trade opponent spawned");
            var session = runtime.session(player);
            if (lethalClub) {
                session.kit().setHp(1);
                for (int slot = 1; slot < 4; slot++) session.party().kit(slot).setHp(0);
            }
            double hp = session.kit().hp();
            long clubFrame = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            // The previous END_SERVER_TICK ended at clubFrame-3. Traveler N1 lands
            // at clubFrame-2; the club lands at clubFrame, in the same tick's drain.
            context.assertTrue(clubFrame >= 15, "Server clock permits the preceding attack wind-up");
            context.assertTrue(session.intent(Intent.ATTACK_PRESS, clubFrame - 15), "Traveler N1 queued");
            session.advanceTo(clubFrame - 3);
            tick(context, member);
            context.assertTrue(member.isWindingUp(), "Real entity AI starts its club wind-up");
            for (int tick = 1; tick < Hilichurl.WINDUP_TICKS; tick++) tick(context, member);
            context.assertTrue(member.isAlive() && player.isAlive(), "Both combatants alive before the strike tick");
            close(context, session.kit().hp(), hp, "No club damage before its hitmark");
            tick(context, member); // Entity tick, before the loader's END_SERVER_TICK hook.
            runtime.tick(context.getLevel().getServer());
            context.assertFalse(member.isAlive(),
                    "Earlier-frame Traveler hit must kill the hilichurl before the same-tick club can cancel it");
            context.assertTrue(player.isAlive(), "Later-frame club from the dead hilichurl cannot win the lethal trade");
            close(context, session.kit().hp(), hp, "Later-frame club cannot damage the earlier-frame winner");
            context.assertValueEqual(member.getKillCredit(), player, "Earlier-frame Traveler retains kill attribution");
        });
    }

    public static void sameTickEarlierFreezePreventsClub(GameTestHelper context) {
        context.runAfterDelay(12, () -> {
            sameTickEarlierFreeze(context, false);
            context.succeed();
        });
    }

    public static void sameTickEarlierFreezePreventsPuppetClub(GameTestHelper context) {
        context.runAfterDelay(36, () -> {
            sameTickEarlierFreeze(context, true);
            context.succeed();
        });
    }

    private static void sameTickEarlierFreeze(GameTestHelper context, boolean hitPuppet) {
        withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            var amber = (AmberKit) session.party().kit(1);
            long clubFrame = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            long castFrame = clubFrame - KaeyaKit.SKILL_HIT_FRAME - 2;
            context.assertTrue(castFrame >= (hitPuppet ? 60 : 0), "Server clock permits the preceding casts");
            BaronBunny bunny = null;
            if (hitPuppet) {
                context.assertTrue(session.intent(Intent.SWITCH_2, castFrame - 60), "Select Amber before strike tick");
                context.assertTrue(session.intent(Intent.SKILL_PRESS, castFrame - 60), "Cast the real puppet");
                session.advanceTo(castFrame - 60 + AmberKit.ADAPTED_PUPPET_LANDING_FRAME);
                bunny = context.getLevel().getEntitiesOfClass(BaronBunny.class, player.getBoundingBox().inflate(10),
                        entity -> entity.getType() == GenshinEntities.BARON_BUNNY && !entity.isRemoved()).getFirst();
            }
            var member = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
            member.setGenshinLevel(20);
            member.snapTo(player.position().add(0, 0, hitPuppet ? 4.5 : 1.5));
            member.setCamp(member.position(), member.position());
            context.assertTrue(context.getLevel().addFreshEntity(member), "Freeze opponent spawned");
            var target = runtime.target(member);
            target.aura().applyHit(Element.HYDRO, 2, castFrame);
            context.assertTrue(session.intent(Intent.SWITCH_3, castFrame), "Select Kaeya before strike tick");
            context.assertTrue(session.intent(Intent.SKILL_PRESS, castFrame), "Frostgnaw queued for clubFrame-2");
            session.advanceTo(clubFrame - 3); // Previous END_SERVER_TICK, before the Cryo hitmark.
            double hp = hitPuppet ? amber.puppetHp() : session.kit().hp();
            double playerHp = session.kit().hp();
            tick(context, member);
            context.assertTrue(member.getTarget() == (hitPuppet ? bunny : player), "Real AI selects the intended club victim");
            context.assertTrue(member.isWindingUp(), "Real AI starts a club wind-up before Freeze");
            for (int tick = 1; tick < Hilichurl.WINDUP_TICKS; tick++) tick(context, member);
            context.assertFalse(CombatRuntime.isFrozen(member), "Cryo remains queued when strike tick enters aiStep");
            close(context, target.hp(), target.maxHp(), "Frostgnaw has not landed before the timeline drain");
            tick(context, member); // AI drains the earlier Cryo hit before accepting its later club.
            runtime.tick(context.getLevel().getServer());
            context.assertTrue(member.isAlive() && CombatRuntime.isFrozen(member), "Earlier-frame Frostgnaw freezes the living hilichurl");
            close(context, hitPuppet ? amber.puppetHp() : session.kit().hp(), hp,
                    hitPuppet ? "Later-frame frozen club cannot damage Baron Bunny" : "Later-frame frozen club cannot damage Kaeya");
            close(context, session.kit().hp(), playerHp, "Puppet-targeted club cannot damage its out-of-range owner");
            if (hitPuppet) close(context, bunny.getHealth(), hp, "Suppressed club leaves the puppet's vanilla mirror unchanged");
            context.assertFalse(member.isWindingUp(), "The due strike is consumed, not queued for thaw");
            context.assertValueEqual(member.visualPhase(), io.github.brainage04.genshininminecraft.rules.EnemyAnimations.Phase.TELEGRAPH,
                    "Freeze holds the pre-strike raised club instead of displaying suppressed impact/recovery");
            context.assertTrue(member.frozenFrame() >= 0, "Earlier same-tick Freeze immediately publishes its stopped visual clock");
            for (int tick = 0; tick < Hilichurl.RECOVERY_TICKS; tick++) tick(context, member);
            close(context, hitPuppet ? amber.puppetHp() : session.kit().hp(), hp, "No club damage while Frozen");
            session.advanceTo(clubFrame + 400);
            target.aura().advanceTo(clubFrame + 400);
            context.assertFalse(CombatRuntime.isFrozen(member), "Seeded Freeze naturally expires before puppet lifetime");
            tick(context, member);
            close(context, hitPuppet ? amber.puppetHp() : session.kit().hp(), hp, "Thaw cannot replay the consumed club");
            context.assertTrue(member.isWindingUp(), "Thawed AI must start a fresh telegraph");
            for (int tick = 1; tick < Hilichurl.WINDUP_TICKS; tick++) tick(context, member);
            close(context, hitPuppet ? amber.puppetHp() : session.kit().hp(), hp, "Fresh telegraph does not hit early");
            tick(context, member);
            context.assertTrue((hitPuppet ? amber.puppetHp() : session.kit().hp()) < hp, "A fresh post-thaw strike still deals damage");
        });
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

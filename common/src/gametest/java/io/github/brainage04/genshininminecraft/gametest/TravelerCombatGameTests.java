package io.github.brainage04.genshininminecraft.gametest;

import com.mojang.authlib.GameProfile;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterState;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.world.ManagedWorldData;
import io.github.brainage04.genshininminecraft.world.PartySavedData;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Synchronous independent simulations exercise the real server adapter without advancing the server clock. */
public final class TravelerCombatGameTests {
    private TravelerCombatGameTests() {}

    public static void palmVortexDamageAndCooldown(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            LivingEntity mob = mob(context, player);
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            context.assertTrue(runtime.receive(player, Intent.SKILL_PRESS), "Managed skill intent accepted");
            context.assertTrue(runtime.receive(player, Intent.SKILL_RELEASE), "Tap release accepted");
            session.advanceTo(start + 31);
            close(context, mob.getHealth(), 20, "Storm cannot land before source frame 32");
            session.advanceTo(start + 32);
            // datamine.md:17.808*2.569 +38.7413*2.42; equal-level DEF=.5, RES10%;
            // traveler-anemo.md Initial Storm176%. UUID seed0's first crit roll is noncritical.
            double expected = (45.748752 + 93.753946) * 1.76 * ((20.0 + 100) / (20 + 100 + 20 + 100)) * (1 - .10);
            close(context, runtime.target(mob).hp(), 20 * 100 - expected, "Sourced Initial Storm Genshin damage");
            close(context, mob.getHealth(), 20 - expected / 100, "Vanilla health mirrors target fraction");
            close(context, session.kit().energy(), 2 * 3, "Two same-element particles grant six energy directly");
            context.assertFalse(session.intent(Intent.SKILL_PRESS, start + 326), "Tap cooldown rejected one frame before ready");
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start + 327), "Five seconds from source cooldown frame27 is ready");
            mob.discard();
        });
        context.succeed();
    }

    public static void burstEnergyAndTornado(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            context.assertFalse(runtime.receive(player, Intent.BURST_PRESS), "Burst rejects zero energy");
            session.kit().grantEnergy(60);
            LivingEntity mob = mob(context, player);
            mob.snapTo(player.position().add(0, 0, 3.2));
            context.assertTrue(runtime.receive(player, Intent.BURST_PRESS), "Burst accepts 60 energy");
            close(context, session.kit().energy(), 0, "Burst drains the complete 60 cost");
            context.assertValueEqual(session.kit().burstReadyFrame(), start + 900, "Fifteen-second burst cooldown");
            session.advanceTo(start + 96);
            double expected = (45.748752 + 93.753946) * .808 * .5 * .9;
            close(context, runtime.target(mob).hp(), 2000 - expected, "First sourced Aether tornado hit at96");
            context.assertTrue(mob.getDeltaMovement().lengthSqr() > 0, "Small mob pulled by the moving tornado");
            Vec3 pulledVelocity = mob.getDeltaMovement();
            for (int repeated = 0; repeated < 10; repeated++) session.intent(Intent.ATTACK_RELEASE, start + 96);
            context.assertValueEqual(mob.getDeltaMovement(), pulledVelocity, "Repeated intents in one frame must not amplify tornado pulling");
            mob.discard();
        });
        context.succeed();
    }

    public static void gustSurgeIgnoresBuffGainedAfterCast(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            session.kit().grantEnergy(60);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start), "Cast unbuffed Gust Surge");
            session.advanceTo(start + 95);
            var first = mob(context, player);
            first.snapTo(player.position().add(0, 0, 3.2));
            session.advanceTo(start + 96);
            close(context, runtime.target(first).hp(), 2000 - (45.748752 + 93.753946) * .808 * .5 * .9,
                    "First unbuffed tornado tick");
            first.discard();
            context.assertTrue(session.intent(Intent.SWITCH_4, start + 100), "Switch to Lisa while the tornado persists");
            context.assertTrue(session.intent(Intent.SWITCH_1, start + 160), "Real Lisa-to-Traveler switch grants Thrilling Tales");
            close(context, session.kit().stats().atk(), (45.748752 + 93.753946) * 1.24, "Live Traveler really gains24% ATK");
            session.advanceTo(start + 185);
            var later = mob(context, player);
            later.snapTo(player.position().add(0, 0, 6.2));
            runtime.target(later).aura().applyHit(Element.CRYO, 1, start + 185);
            session.advanceTo(start + 186);
            double talent = (45.748752 + 93.753946) * (.808 + .248) * .5 * .9;
            double swirl = .6 * 80.584775 * .9;
            close(context, runtime.target(later).hp(), 2000 - talent - swirl,
                    "Gust Surge Anemo and newly absorbed Cryo ignore Thrilling Tales gained after cast");
            context.assertValueEqual(session.burstAbsorbedElement(), Element.CRYO, "Absorption can be chosen after the stat snapshot");
            later.discard();
        });
        context.succeed();
    }

    public static void gustSurgeKeepsBuffAfterExpiry(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            context.assertTrue(session.intent(Intent.SWITCH_4, start), "Select Lisa");
            context.assertTrue(session.intent(Intent.SWITCH_1, start + 60), "Grant Traveler the real ten-second buff");
            session.kit().grantEnergy(60);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start + 540), "Cast while the buff remains through first tick");
            session.advanceTo(start + 635);
            var first = mob(context, player);
            first.snapTo(player.position().add(0, 0, 3.2));
            runtime.target(first).aura().applyHit(Element.CRYO, 1, start + 635);
            session.advanceTo(start + 636);
            double talent = (45.748752 + 93.753946) * 1.24 * (.808 + .248) * .5 * .9;
            double swirl = .6 * 80.584775 * .9;
            close(context, runtime.target(first).hp(), 2000 - talent - swirl, "First tick uses buffed Anemo and absorbed damage");
            first.discard();
            session.advanceTo(start + 665);
            close(context, session.kit().stats().atk(), 45.748752 + 93.753946, "Live Traveler has lost Thrilling Tales");
            var later = mob(context, player);
            later.snapTo(player.position().add(0, 0, 4.2));
            session.advanceTo(start + 666);
            close(context, runtime.target(later).hp(), 2000 - talent, "Gust Surge Anemo and absorbed damage retain expired cast-time buff");
            later.discard();
        });
        context.succeed();
    }

    public static void palmVortexHoldDamageRemainsDynamic(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start), "Begin held Palm Vortex without buff");
            session.advanceTo(start + 20);
            var first = mob(context, player);
            session.advanceTo(start + 21);
            close(context, runtime.target(first).hp(), 2000 - (45.748752 + 93.753946) * .12 * .5 * .9, "First cutting hit is unbuffed");
            first.discard();
            new CharacterState(CharacterBaseStats.Character.LISA, 80).switchTo(session.kit().state(), start + 22);
            session.advanceTo(start + 29);
            var later = mob(context, player);
            runtime.target(later).aura().applyHit(Element.CRYO, 1, start + 29);
            session.advanceTo(start + 30);
            close(context, runtime.target(later).hp(),
                    2000 - (45.748752 + 93.753946) * 1.24 * (.12 + .12 * .25) * .5 * .9 - .6 * 80.584775 * .9,
                    "Held Palm Vortex Anemo and absorbed cutting damage use live buffed stats, not a snapshot");
            later.discard();
        });
        context.succeed();
    }

    public static void palmVortexSwirlsAndAbsorbsPyro(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            LivingEntity mob = mob(context, player);
            var target = runtime.target(mob);
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            target.aura().applyHit(Element.PYRO, 1, start);
            runtime.receive(player, Intent.SKILL_PRESS);
            runtime.receive(player, Intent.SKILL_RELEASE);
            session.advanceTo(start + 32);
            // Separate absorbed storm=25% Anemo scaling and Swirl=.6*K(20)*Pyro RES, not ATK damage.
            double stormAndAbsorbed = (45.748752 + 93.753946) * (1.76 + 1.76 * .25) * .5 * .9;
            double swirl = .6 * 80.584775 * .9;
            close(context, target.hp(), 2000 - stormAndAbsorbed - swirl, "Talent, absorbed Pyro and transformative Swirl instances");
            context.assertValueEqual(target.swirlCount(), 1, "One Swirl reaction");
            context.assertValueEqual(session.skillAbsorbedElement(), Element.PYRO, "Palm Vortex locks Pyro before consuming the aura");
            context.assertTrue(target.aura().gauge(Element.PYRO) > 0, "Absorbed storm reapplies Pyro");
            mob.discard();
        });
        context.succeed();
    }

    public static void managedVanillaMeleeCancelled(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            LivingEntity mob = mob(context, player);
            player.attack(mob);
            close(context, mob.getHealth(), 20, "Actual loader attack event cancels vanilla melee in managed world");
            ManagedWorldData.get(context.getLevel().getServer()).setManaged(context.getLevel().getServer(), false);
            context.assertFalse(runtime.receive(player, Intent.SKILL_PRESS), "Unmanaged worlds reject mod combat intents");
            player.attack(mob);
            context.assertTrue(mob.getHealth() < 20, "Unmanaged vanilla melee still damages the mob");
            mob.discard();
        });
        context.succeed();
    }

    public static void targetDeathIsAttributed(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            LivingEntity mob = mob(context, player);
            mob.setHealth(.1F);
            runtime.receive(player, Intent.SKILL_PRESS);
            runtime.receive(player, Intent.SKILL_RELEASE);
            runtime.session(player).advanceTo(Frames.atServerTick(context.getLevel().getServer().getTickCount()) + 32);
            context.assertFalse(mob.isAlive(), "Zero Genshin HP kills the entity");
            context.assertValueEqual(mob.getKillCredit(), player, "Player receives normal kill attribution");
            mob.discard();
        });
        context.succeed();
    }

    public static void managedOffOnDiscardsQueuedElectroCharged(GameTestHelper context) {
        withManaged(context, (runtime, player) -> {
            var server = context.getLevel().getServer();
            long start = Frames.atServerTick(server.getTickCount());
            LivingEntity mob = mob(context, player);
            var oldProfile = runtime.target(mob);
            oldProfile.aura().applyHit(Element.HYDRO, 4, start);
            oldProfile.aura().applyHit(Element.ELECTRO, 4, start);
            // No owner exists for the seeded setup tick; the first owned tick is at start+60.
            oldProfile.aura().tickElectroCharged(start, false);
            runtime.receive(player, Intent.SKILL_PRESS);
            runtime.receive(player, Intent.SKILL_RELEASE);
            var session = runtime.session(player);
            session.advanceTo(start + 32);
            context.assertValueEqual(session.skillAbsorbedElement(), Element.HYDRO, "Hydro absorption installs the EC owner");
            context.assertValueEqual(oldProfile.aura().nextElectroChargedTickFrame().orElseThrow(), start + 60, "Owned EC damage is queued");
            var managed = ManagedWorldData.get(server);
            managed.setManaged(server, false);
            runtime.tick(server); // Unmanaged tick discards profiles, without draining the combat timeline.
            managed.setManaged(server, true);
            mob.setHealth(20);
            var replacement = runtime.target(mob);
            context.assertTrue(replacement != oldProfile, "Managed re-entry creates a fresh target profile");
            session.advanceTo(start + 120);
            close(context, mob.getHealth(), 20, "Discarded profile cannot replay queued EC damage after managed off/on");
            close(context, replacement.hp(), 2000, "Replacement profile retains full Genshin HP");
            close(context, replacement.aura().gauge(Element.HYDRO), 0, "Discarded Hydro aura does not return");
            close(context, replacement.aura().gauge(Element.ELECTRO), 0, "Discarded Electro aura does not return");
            mob.discard();
        });
        context.succeed();
    }

    private static LivingEntity mob(GameTestHelper context, ServerPlayer player) {
        var mob = context.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(1, 2, 1));
        mob.snapTo(player.position().add(0, 0, 2));
        mob.setNoAi(true);
        return mob;
    }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .0001, message + ": expected " + expected + ", actual " + actual);
    }
    private static void withManaged(GameTestHelper context, BiConsumer<CombatRuntime, ServerPlayer> test) {
        var server = context.getLevel().getServer();
        var original = ManagedWorldData.get(server);
        var rules = server.getGameRules().copy(server.overworld().enabledFeatures());
        var data = new ManagedWorldData();
        server.overworld().getDataStorage().set(ManagedWorldData.TYPE, data);
        CombatRuntime.stop(server);
        var originalParties = PartySavedData.get(server);
        server.overworld().getDataStorage().set(PartySavedData.TYPE, new PartySavedData());
        try {
            data.setManaged(server, true);
            var profile = new GameProfile(new UUID(0, 0), "traveler-kit-test");
            var player = new ServerPlayer(server, context.getLevel(), profile, ClientInformation.createDefault());
            player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player,
                    CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            player.snapTo(context.absoluteVec(new Vec3(1, 2, 1)));
            player.setYRot(0);
            test.accept(CombatRuntime.get(server), player);
        } finally {
            CombatRuntime.stop(server);
            server.overworld().getDataStorage().set(PartySavedData.TYPE, originalParties);
            server.getGameRules().setAll(rules, server);
            server.overworld().getDataStorage().set(ManagedWorldData.TYPE, original);
        }
    }
}

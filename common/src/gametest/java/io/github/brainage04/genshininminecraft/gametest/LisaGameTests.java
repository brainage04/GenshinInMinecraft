package io.github.brainage04.genshininminecraft.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.authlib.GameProfile;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterState;
import io.github.brainage04.genshininminecraft.rules.kit.AmberKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Both loaders execute the same real server profiles, input intents and sourced damage arithmetic. */
public final class LisaGameTests {
    private LisaGameTests() {}
    public static void violetTapStacksAndUnascendedCharge(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = now(context);
            context.assertTrue(runtime.receive(player, Intent.SWITCH_4), "Select ascension0 Lisa");
            for (int cast = 0; cast < 4; cast++) {
                tap(context, session, start + cast * 77);
                context.assertValueEqual(target.conductive().stacks(), Math.min(3, cast + 1), "Repeated tap impacts cap Conductive at sourced3");
            }
            close(context, session.kit().energy(), 0, "Tap generates zero particles");
            double before = target.hp();
            context.assertTrue(session.intent(Intent.ATTACK_PRESS, start + 308), "Hold catalyst normal into charge");
            session.advanceTo(start + 397);
            context.assertTrue(target.hp() < before, "Real Electro charged attack damages target");
            context.assertValueEqual(target.conductive().stacks(), 3, "Ascension0 charge cannot add or remove marks");
            target.conductive().consume(start + 397);
            session.intent(Intent.ATTACK_RELEASE, start + 398);
            context.assertTrue(session.intent(Intent.ATTACK_PRESS, start + 490), "Fresh held charge after recovery and stamina regen");
            session.advanceTo(start + 579);
            context.assertValueEqual(target.conductive().stacks(), 0, "Charged hit must not grant locked A1 Induced Aftershock");
        });
        context.succeed();
    }
    public static void violetHoldConsumesThreeStacks(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_4);
            for (int cast = 0; cast < 3; cast++) tap(context, session, start + cast * 77);
            context.assertValueEqual(target.conductive().stacks(), 3, "Precondition: actual tap projectiles built3 marks");
            double before = target.hp();
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start + 231), "Start held Violet Arc");
            context.assertTrue(session.intent(Intent.SKILL_RELEASE, start + 345), "Release at sourced approximately1.9s");
            session.advanceTo(start + 347);
            close(context, target.hp(), before, "Hold hit waits original release-to-hit3 frames");
            session.advanceTo(start + 348);
            close(context, target.hp(), before - lisaTalent(4.872), "Three marks use exact talent1 487.2%, not rounded152% of320%");
            context.assertValueEqual(target.conductive().stacks(), 0, "Hold consumes all Conductive marks");
            close(context, target.aura().gauge(Element.ELECTRO), 1.6, "Hold applies2U with no application ICD");
            close(context, session.kit().energy(), 5 * 3, "Five guaranteed Electro particles give field Lisa15 energy");
            for (int slot = 0; slot < 3; slot++) close(context, session.party().kit(slot).energy(), 5 * .6, "Each off-field different-element member gets3 energy");
            context.assertValueEqual(session.kit().skillReadyFrame(), start + 345 + 16 * 60, "Hold cooldown starts on release");
            context.assertFalse(session.intent(Intent.SKILL_PRESS, start + 1304), "Sixteen-second cooldown not one frame early");
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start + 1305), "Hold skill ready at exact release-relative boundary");
        });
        context.succeed();
    }
    public static void violetTapMultistacksAndOffFieldProjectile(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var first = hilichurl(context, player.position().add(-.3, 0, 2));
            var second = hilichurl(context, player.position().add(.3, 0, 2));
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_4);
            runtime.target(first).aura().applyHit(Element.PYRO, 2, start);
            runtime.target(second).aura().applyHit(Element.PYRO, 2, start);
            tap(context, session, start);
            context.assertValueEqual(runtime.target(first).conductive().stacks(), 3, "Overlapping initial/bounce/Overloaded grants reach cap on first enemy");
            context.assertValueEqual(runtime.target(second).conductive().stacks(), 3, "Second enemy also receives overlapping marks, not primary-only stacking");
            first.discard(); second.discard();
            // Stay inside the fixture's controlled eight-block floor, not a neighboring test volume.
            var distant = hilichurl(context, player.position().add(0, 0, 7));
            double before = runtime.target(distant).hp();
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start + 77), "Launch distant homing tap");
            session.intent(Intent.SKILL_RELEASE, start + 77);
            session.advanceTo(start + 98);
            close(context, runtime.target(distant).hp(), before, "Projectile is still in flight when Lisa leaves");
            context.assertTrue(session.projectileSnapshots().stream().anyMatch(packet ->
                    packet.kind() == io.github.brainage04.genshininminecraft.network.ProjectileVisualPayload.Kind.VIOLET_ORB),
                    "Actual server homing orb publishes render snapshots while in flight");
            context.assertTrue(session.intent(Intent.SWITCH_1, start + 98), "Lisa leaves after projectile launch before impact");
            session.advanceTo(start + 257);
            close(context, runtime.target(distant).hp(), before - lisaTalent(.8), "Launched orb keeps Lisa-owned damage after switch");
            context.assertValueEqual(runtime.target(distant).conductive().stacks(), 0, "Off-field impact must not grant Conductive");
            distant.discard();
        });
        context.succeed();
    }
    public static void lightningRoseEnergyTimingAndSwitch(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var near = hilichurl(context, player.position().add(0, 0, 2));
            var far = hilichurl(context, player.position().add(0, 0, 6));
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_4); session.kit().grantEnergy(79);
            context.assertFalse(runtime.receive(player, Intent.BURST_PRESS), "Rose requires80 energy");
            close(context, session.kit().energy(), 79, "Rejected cast does not drain");
            context.assertValueEqual(session.kit().burstReadyFrame(), 0L, "Rejected cast installs no cooldown");
            session.kit().grantEnergy(1);
            context.assertTrue(runtime.receive(player, Intent.BURST_PRESS), "Lightning Rose accepted with80");
            close(context, session.kit().energy(), 0, "Reserve80 at acceptance");
            session.advanceTo(start + 55); close(context, runtime.target(near).hp(), 885.200, "No placement before original56");
            session.advanceTo(start + 56);
            close(context, runtime.target(near).hp(), 885.200 - lisaTalent(.1), "Placement has weak10% Electro damage");
            context.assertValueEqual(runtime.target(near).auraElements(), 0, "Placement0U must not apply Electro");
            context.assertTrue(near.getDeltaMovement().horizontalDistanceSqr() > 0, "Initial placement knocks back enemies");
            context.assertTrue(session.intent(Intent.SWITCH_2, start + 60), "Rose persists after sourced swap window and party cooldown");
            player.snapTo(player.position().add(20, 0, 0));
            session.advanceTo(start + 118);
            double before = runtime.target(near).hp() + runtime.target(far).hp();
            session.advanceTo(start + 119);
            close(context, runtime.target(near).hp() + runtime.target(far).hp(), before - lisaTalent(.3656),
                    "First random-target discharge uses original approximate119 and sourced36.56%");
            context.assertTrue(session.projectileSnapshots().stream().anyMatch(packet ->
                    packet.kind() == io.github.brainage04.genshininminecraft.network.ProjectileVisualPayload.Kind.ROSE_BOLT),
                    "Actual targeted discharge publishes a Rose-to-target bolt");
            context.assertTrue(runtime.target(near).aura().gauge(Element.ELECTRO) > 0
                            || runtime.target(far).aura().gauge(Element.ELECTRO) > 0, "Discharge applies1U to its selected enemy");
            session.advanceTo(start + 148);
            close(context, runtime.target(near).hp() + runtime.target(far).hp(), before - lisaTalent(.3656),
                    "No discharge before half-second interval");
            session.advanceTo(start + 149);
            context.assertTrue(runtime.target(near).hp() + runtime.target(far).hp() < before - lisaTalent(.3656),
                    "Rose keeps damaging after owner moves and switches");
            session.advanceTo(start + 1019);
            double expiredHp = runtime.target(near).hp() + runtime.target(far).hp();
            var crits = new java.util.Random(0);
            double damage = lisaTalent(.1) * crit(crits) + lisaTalent(.1) * crit(crits);
            for (int arc = 0; arc < 29; arc++) damage += lisaTalent(.3656) * crit(crits);
            close(context, expiredHp, 2 * 885.200 - damage,
                    "Exactly29 sourced arcs across random targets, with independent5%/50% seeded crit arithmetic");
            context.assertTrue(runtime.target(near).hp() < 885.200 - lisaTalent(.1)
                            && runtime.target(far).hp() < 885.200 - lisaTalent(.1),
                    "Random source priority can select both enemies, not just the nearest one");
            session.advanceTo(start + 1200);
            close(context, runtime.target(near).hp() + runtime.target(far).hp(), expiredHp, "Expired field stops damage");
            context.assertValueEqual(session.party().kit(3).burstReadyFrame(), start + 53 + 1200, "Twenty-second burst cooldown from source53 persists off-field");
        });
        context.succeed();
    }
    public static void lightningRoseIgnoresBuffAfterFormation(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = now(context);
            context.assertTrue(session.intent(Intent.SWITCH_4, start), "Select Lisa");
            session.kit().grantEnergy(80);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start), "Cast unbuffed Rose");
            session.advanceTo(start + 59);
            // This starter party has no second catalyst; use the actual server-state weapon buff API.
            new CharacterState(CharacterBaseStats.Character.LISA, 80).switchTo(session.kit().state(), start + 60);
            session.advanceTo(start + 118);
            close(context, session.kit().stats().atk(), (49.87 + 94) * 1.24, "Live Lisa really has24% ATK");
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            session.advanceTo(start + 119);
            close(context, runtime.target(enemy).hp(), 885.200 - lisaTalent(.3656),
                    "Rose ignores Thrilling Tales gained after lantern formation");
            enemy.discard();
        });
        context.succeed();
    }

    public static void lightningRoseSnapshotsFormationAndKeepsExpiredBuff(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = now(context);
            context.assertTrue(session.intent(Intent.SWITCH_4, start), "Select Lisa");
            session.kit().grantEnergy(80);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start), "Cast Rose before the buff");
            session.advanceTo(start + 55);
            var placement = hilichurl(context, player.position().add(0, 0, 2));
            session.advanceTo(start + 56);
            close(context, runtime.target(placement).hp(), 885.200 - lisaTalent(.1), "Placement precedes lantern formation and the buff");
            placement.discard();
            session.advanceTo(start + 58);
            new CharacterState(CharacterBaseStats.Character.LISA, 80).switchTo(session.kit().state(), start + 58);
            session.advanceTo(start + 118);
            var first = hilichurl(context, player.position().add(0, 0, 2));
            session.advanceTo(start + 119);
            double discharge = lisaTalent(.3656) * 1.24;
            close(context, runtime.target(first).hp(), 885.200 - discharge,
                    "Rose snapshots at lantern formation, not burst acceptance or placement");
            first.discard();
            session.advanceTo(start + 658);
            close(context, session.kit().stats().atk(), 49.87 + 94, "Live Lisa loses the ten-second buff");
            var later = hilichurl(context, player.position().add(0, 0, 2));
            session.advanceTo(start + 659);
            close(context, runtime.target(later).hp(), 885.200 - discharge, "Rose retains lantern-formation buff after expiry");
            later.discard();
            session.advanceTo(start + 958);
            var last = hilichurl(context, player.position().add(0, 0, 2));
            session.advanceTo(start + 959);
            close(context, runtime.target(last).hp(), 885.200 - discharge, "Last of29 discharges keeps the formation snapshot");
            last.discard();
        });
        context.succeed();
    }

    public static void electroChargedTicksConsumeBothGauges(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_4);
            context.assertValueEqual(command(context, source(context, player), "genshin aura hydro 2"), 1, "Debug command seeds absent party Hydro");
            session.intent(Intent.SKILL_PRESS, start); session.intent(Intent.SKILL_RELEASE, start + 114);
            session.advanceTo(start + 117);
            double tick = 2 * 80.584775 * .9; // damage.md: current EC coefficient, Lv20 K(L), Electro RES, no DEF/crit.
            close(context, target.hp(), 885.200 - lisaTalent(3.2) - tick, "Initial EC tick is a separate transformative instance");
            close(context, target.aura().gauge(Element.HYDRO), 1.6 - 117.0 / 60 * (1.6 / 12) - .4, "Initial tick consumes0.4U Hydro plus natural decay");
            close(context, target.aura().gauge(Element.ELECTRO), 1.6 - .4, "Initial tick consumes0.4U Electro");
            double before = target.hp();
            session.advanceTo(start + 176); close(context, target.hp(), before, "EC cannot tick one frame before sourced1s interval");
            session.advanceTo(start + 177); close(context, target.hp(), before - tick, "Second EC tick occurs after60 frames");
            close(context, target.aura().gauge(Element.HYDRO), 1.6 - 177.0 / 60 * (1.6 / 12) - .8, "Second tick consumes Hydro again");
            close(context, target.aura().gauge(Element.ELECTRO), 1.6 - 1.6 / 12 - .8, "Second tick consumes Electro again");
            session.advanceTo(start + 237); close(context, target.hp(), before - 2 * tick, "Final regular tick consumes remaining weaker Hydro");
            close(context, target.aura().gauge(Element.HYDRO), 0, "Hydro exhausted");
            context.assertTrue(target.aura().gauge(Element.ELECTRO) > 0, "Other aura survives, not historical unverified residue bug");
            session.advanceTo(start + 600); close(context, target.hp(), before - 2 * tick, "No EC once coexistence ends");
        });
        context.succeed();
    }
    public static void lisaOverloadsAmbersPyro(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            var neighbor = hilichurl(context, enemy.position().add(2, 0, 0));
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_2);
            session.intent(Intent.ATTACK_PRESS, start); session.intent(Intent.ATTACK_RELEASE, start + 86); session.advanceTo(start + 86);
            context.assertTrue(runtime.target(enemy).aura().gauge(Element.PYRO) > 0, "Actual Amber aimed shot seeds Pyro");
            context.assertTrue(session.intent(Intent.SWITCH_4, start + 96), "Switch from Amber to Lisa");
            tap(context, session, start + 96);
            double overloaded = 2.75 * 80.584775 * .9;
            // damage.md: Slingshot86 and its R1 instant-arrow36% bonus; Lisa base49.87 + Thrilling Tales94.
            close(context, runtime.target(enemy).hp(), 885.200 - (48.04 + 86) * 1.24 * 1.36 * .5 * .9 - lisaTalent(.8) - overloaded,
                    "Amber-then-Lisa triggers current Overloaded coefficient independent of talent DEF");
            close(context, runtime.target(neighbor).hp(), 885.200 - overloaded, "Reaction-only neighbor takes zero-gauge Pyro AoE");
            context.assertValueEqual(runtime.target(neighbor).auraElements(), 0, "Overloaded AoE never applies Pyro or Electro");
            context.assertValueEqual(runtime.target(enemy).conductive().stacks(), 2, "Tap Overloaded gives direct enemy2 marks");
            context.assertValueEqual(runtime.target(neighbor).conductive().stacks(), 1, "Reaction-only neighbor gets1 mark");
        });
        context.succeed();
    }
    public static void auraCommandPermissionsAndTargeting(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var nearest = hilichurl(context, player.position().add(1, 0, 0));
            var aimed = hilichurl(context, player.position().add(0, 0, 4));
            var source = source(context, player);
            for (var permission : new LevelBasedPermissionSet[]{LevelBasedPermissionSet.ALL, LevelBasedPermissionSet.MODERATOR}) {
                try {
                    command(context, source.withPermission(permission), "genshin aura hydro 2");
                    throw new AssertionError("Unprivileged aura command must be rejected");
                } catch (AssertionError denial) { if (!(denial.getCause() instanceof CommandSyntaxException)) throw denial; }
            }
            context.assertValueEqual(runtime.target(aimed).auraElements(), 0, "Denied command does not mutate target");
            context.assertValueEqual(command(context, source, "genshin aura hydro 2"), 1, "Operator2 can apply aura");
            close(context, runtime.target(aimed).aura().gauge(Element.HYDRO), 1.6, "Aimed target takes precedence over nearer off-ray enemy");
            context.assertValueEqual(runtime.target(nearest).auraElements(), 0, "Nearer off-ray target unchanged while ray hit exists");
            player.setYRot(180);
            context.assertValueEqual(command(context, source, "genshin aura cryo 1"), 1, "No ray target falls back to nearest combat target");
            close(context, runtime.target(nearest).aura().gauge(Element.CRYO), .8, "Fallback target receives taxed1U");
        });
        context.succeed();
    }
    public static void lisaEffectsNeverDamagePuppetsOrPlayers(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_2); runtime.receive(player, Intent.SKILL_PRESS); session.advanceTo(start + 45);
            var amber = (AmberKit) session.party().kit(1);
            double puppetHp = amber.puppetHp();
            var profile = new GameProfile(new UUID(1, 1), "lisa-friend");
            var other = new ServerPlayer(context.getLevel().getServer(), context.getLevel(), profile, ClientInformation.createDefault());
            other.connection = new ServerGamePacketListenerImpl(context.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), other,
                    CommonListenerCookie.createInitial(profile, false));
            other.snapTo(player.position().add(0, 0, 3)); context.getLevel().addNewPlayer(other);
            try {
                hilichurl(context, player.position().add(.5, 0, 3));
                context.assertTrue(session.intent(Intent.SWITCH_4, start + 60), "Select Lisa while Bunny and other player overlap enemy area");
                tap(context, session, start + 60);
                session.intent(Intent.SKILL_PRESS, start + 137); session.intent(Intent.SKILL_RELEASE, start + 251); session.advanceTo(start + 254);
                session.kit().grantEnergy(80); session.intent(Intent.BURST_PRESS, start + 278); session.advanceTo(start + 427);
                close(context, amber.puppetHp(), puppetHp, "Lisa tap/hold/Rose cannot damage player-owned puppet");
                close(context, other.getHealth(), other.getMaxHealth(), "Lisa effects cannot damage another player");
                for (var member : session.party().members()) close(context, member.hp(), member.maxHp(), "No effect damages any party member");
            } finally { context.getLevel().removePlayerImmediately(other, Entity.RemovalReason.DISCARDED); }
        });
        context.succeed();
    }
    private static void tap(GameTestHelper context, CombatRuntime.Session session, long frame) {
        context.assertTrue(session.intent(Intent.SKILL_PRESS, frame), "Violet Arc tap press accepted");
        context.assertTrue(session.intent(Intent.SKILL_RELEASE, frame), "Violet Arc tap release accepted");
        session.advanceTo(frame + 60);
    }
    private static Hilichurl hilichurl(GameTestHelper context, Vec3 position) {
        var enemy = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
        enemy.setGenshinLevel(20);
        enemy.snapTo(position); enemy.setNoAi(true);
        context.assertTrue(context.getLevel().addFreshEntity(enemy), "Lisa test enemy spawned");
        return enemy;
    }
    private static CommandSourceStack source(GameTestHelper context, ServerPlayer player) {
        return context.getLevel().getServer().createCommandSourceStack().withLevel(context.getLevel()).withEntity(player)
                .withPosition(player.position()).withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
    }
    private static int command(GameTestHelper context, CommandSourceStack source, String command) {
        try { return context.getLevel().getServer().getCommands().getDispatcher().execute(command, source); }
        catch (CommandSyntaxException exception) { throw new AssertionError("Command failed: " + command, exception); }
    }
    private static long now(GameTestHelper context) { return Frames.atServerTick(context.getLevel().getServer().getTickCount()); }
    // damage.md: Lisa20/20 base49.87 + Thrilling Tales20/20 base94; equal-level DEF.5 and RES.9.
    private static double lisaTalent(double multiplier) { return (49.87 + 94) * multiplier * .5 * .9; }
    private static double crit(java.util.Random random) { return random.nextDouble() < .05 ? 1.5 : 1; }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .0001, message + ": expected " + expected + ", actual " + actual);
    }
}

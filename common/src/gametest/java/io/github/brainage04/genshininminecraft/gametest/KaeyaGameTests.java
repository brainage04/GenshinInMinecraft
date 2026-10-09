package io.github.brainage04.genshininminecraft.gametest;

import io.github.brainage04.genshininminecraft.combat.CombatTarget;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** Sourced damage arithmetic and actual world contact/movement paths on both loaders. */
public final class KaeyaGameTests {
    private KaeyaGameTests() {}

    public static void frostgnawDamageAndCryoAura(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            var behind = hilichurl(context, player.position().add(0, 0, -2));
            var far = hilichurl(context, player.position().add(0, 0, 9));
            var high = hilichurl(context, player.position().add(0, 4, 2));
            var session = runtime.session(player);
            long start = now(context);
            context.assertTrue(runtime.receive(player, Intent.SWITCH_3), "Select Kaeya");
            context.assertTrue(runtime.receive(player, Intent.SKILL_PRESS), "Frostgnaw is available");
            session.advanceTo(start + 27);
            var target = runtime.target(enemy);
            close(context, target.hp(), 885.200, "No skill damage before original frame28");
            session.advanceTo(start + 28);
            double damage = talent(target, 48.04 + 94, 1.912, .10);
            close(context, target.hp(), 885.200 - damage, "Talent1 191.2% Cryo uses spec Kaeya ATK and target DEF/RES");
            close(context, target.aura().gauge(Element.CRYO), 2 * .8, "No-ICD 2U skill installs taxed Cryo aura");
            close(context, enemy.getHealth(), 20 * target.hp() / 885.200, "Genshin HP mirrors to vanilla health");
            for (var excluded : new Hilichurl[]{behind, far, high})
                close(context, runtime.target(excluded).hp(), 885.200, "Frontal reach excludes behind/far/high targets");
            close(context, session.kit().energy(), 3 * 3, "Three adapted Cryo particles give on-field Kaeya9 energy");
            for (int slot : new int[]{0, 1, 3}) close(context, session.party().kit(slot).energy(), 3 * .6,
                    "Existing party table gives each off-field different-element recipient1.8");
            context.assertValueEqual(session.kit().skillReadyFrame(), start + 25 + 360, "Six-second cooldown from measured frame25");
            context.assertFalse(session.intent(Intent.SKILL_PRESS, start + 384), "Cooldown cannot finish one frame early");
            enemy.discard();
            high.discard();
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start + 385), "Cooldown boundary accepts skill");
            session.advanceTo(start + 413);
            close(context, session.kit().energy(), 9, "Missed skill grants no particles");
        });
        context.succeed();
    }

    public static void frostgnawSuperconductPhysicalShred(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_4);
            context.assertTrue(runtime.receive(player, Intent.ATTACK_PRESS), "Lisa normal seeds real Electro aura");
            runtime.receive(player, Intent.ATTACK_RELEASE);
            session.advanceTo(start + 26);
            context.assertTrue(target.aura().gauge(Element.ELECTRO) > 0, "Lisa actually applied Electro");
            context.assertTrue(session.intent(Intent.SWITCH_3, start + 60), "Switch to Kaeya");
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start + 60), "Cryo skill accepted on Electro target");
            session.advanceTo(start + 88);
            double lisa = talent(target, 49.87 + 94, .396, .10);
            double frostgnaw = talent(target, (48.04 + 94) * 1.24, 1.912, .10); // Lisa→Kaeya: R1 Thrilling Tales24% ATK.
            // damage.md current Superconduct1.5 * character K(20), bypasses DEF, Cryo RES10%.
            double superconduct = 1.5 * 80.584775 * (1 - .10);
            close(context, target.hp(), 885.200 - lisa - frostgnaw - superconduct, "Superconduct is separate noncritical Cryo damage");
            close(context, target.resistance(Element.PHYSICAL), .10 - .40, "Superconduct updates target Physical RES by40 percentage points");
            close(context, target.resistance(Element.CRYO), .10, "Superconduct does not shred Cryo RES");
            context.assertValueEqual(target.aura().physicalResistanceReductionUntilFrame(), start + 88 + 12 * 60,
                    "Shred lasts sourced12 seconds");
            context.assertTrue(session.intent(Intent.ATTACK_PRESS, start + 113), "Following Kaeya Physical normal accepted");
            session.intent(Intent.ATTACK_RELEASE, start + 113);
            session.advanceTo(start + 127);
            double physical = talent(target, (48.04 + 94) * 1.24, .5375, -.30); // Same ten-second ATK buff.
            close(context, target.hp(), 885.200 - lisa - frostgnaw - superconduct - physical,
                    "Subsequent Physical hit uses negative-RES branch1.15, not unshredded0.9");
            context.assertTrue(physical > talent(target, (48.04 + 94) * 1.24, .5375, .10), "Shred increases actual Physical damage");
            session.advanceTo(start + 807);
            target.aura().advanceTo(start + 807);
            close(context, target.resistance(Element.PHYSICAL), -.30, "Shred remains one frame before expiry");
            session.advanceTo(start + 808);
            target.aura().advanceTo(start + 808);
            close(context, target.resistance(Element.PHYSICAL), .10, "Shred expires at exact12-second boundary");
        });
        context.succeed();
    }

    public static void lisaTriggersSuperconductOnCryo(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_3);
            runtime.receive(player, Intent.SKILL_PRESS);
            session.advanceTo(start + 28);
            context.assertTrue(target.aura().gauge(Element.CRYO) > 0, "Frostgnaw applied Cryo");
            context.assertTrue(session.intent(Intent.SWITCH_4, start + 60), "Switch to Lisa after skill swap window");
            context.assertTrue(session.intent(Intent.ATTACK_PRESS, start + 60), "Lisa Electro normal accepted");
            session.intent(Intent.ATTACK_RELEASE, start + 60);
            session.advanceTo(start + 86);
            double damage = talent(target, 48.04 + 94, 1.912, .10) + talent(target, 49.87 + 94, .396, .10)
                    + 1.5 * 80.584775 * .9;
            close(context, target.hp(), 885.200 - damage, "Electro-on-Cryo also deals current Superconduct damage");
            close(context, target.resistance(Element.PHYSICAL), -.30, "Reverse trigger direction also installs Physical shred");
        });
        context.succeed();
    }

    public static void frostgnawMeltsPyroAura(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            var target = runtime.target(enemy);
            long start = now(context);
            target.aura().applyHit(Element.PYRO, 1, start); // Amber normals are Physical; skill is a later slice.
            runtime.receive(player, Intent.SWITCH_3);
            runtime.receive(player, Intent.SKILL_PRESS);
            runtime.session(player).advanceTo(start + 28);
            close(context, target.hp(), 885.200 - talent(target, 48.04 + 94, 1.912, .10) * 1.5,
                    "Reverse Melt amplifies the eligible Cryo talent by1.5 at zero EM");
            close(context, target.aura().gauge(Element.PYRO), 0, "2U Cryo consumes Pyro at0.5 gauge modifier");
            close(context, target.aura().gauge(Element.CRYO), 0, "Over-consuming trigger does not install leftover Cryo aura");
        });
        context.succeed();
    }

    public static void travelerSwirlsKaeyasCryo(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_3);
            runtime.receive(player, Intent.SKILL_PRESS);
            session.advanceTo(start + 28);
            context.assertTrue(session.intent(Intent.SWITCH_1, start + 60), "Switch to Traveler");
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start + 60), "Palm Vortex accepted");
            session.intent(Intent.SKILL_RELEASE, start + 60);
            session.advanceTo(start + 92);
            double damage = talent(target, 48.04 + 94, 1.912, .10)
                    + talent(target, 45.75 + 94, 1.76, .10)
                    + .6 * 80.584775 * .9
                    + talent(target, 45.75 + 94, 1.76 * .25, .10);
            close(context, target.hp(), 885.200 - damage, "Cryo Swirl uses Cryo RES and separate transformative damage plus absorption");
            context.assertValueEqual(target.swirlCount(), 1, "Traveler Swirls Kaeya's actual Cryo aura");
            context.assertValueEqual(session.skillAbsorbedElement(), Element.CRYO, "Traveler locks Cryo absorption before aura consumption");
        });
        context.succeed();
    }

    public static void glacialWaltzEnergyContactAndSwitch(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            Vec3 original = player.position();
            var enemy = hilichurl(context, original.add(0, 0, 2.5));
            var center = hilichurl(context, original); // Inside radius is not sufficient: contact required.
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_3);
            context.assertFalse(runtime.receive(player, Intent.BURST_PRESS), "Glacial Waltz rejects without60 energy");
            context.assertValueEqual(session.kit().burstReadyFrame(), 0L, "Rejected burst installs no cooldown");
            session.kit().grantEnergy(60);
            context.assertTrue(runtime.receive(player, Intent.BURST_PRESS), "Full-energy burst accepted");
            close(context, session.kit().energy(), 0, "Burst reserves60 energy once");
            session.advanceTo(start + 51);
            close(context, target.hp(), 885.200, "No damage before original first-contact frame52");
            session.advanceTo(start + 52);
            double icicle = talent(target, 48.04 + 94, .776, .10);
            close(context, target.hp(), 885.200 - icicle, "First contacted icicle deals sourced77.6% Cryo");
            context.assertTrue(target.aura().gauge(Element.CRYO) > 0, "Burst first contact applies1U Cryo");
            close(context, runtime.target(center).hp(), 885.200, "Burst is not an unconditional radial tick");
            session.advanceTo(start + 75);
            context.assertFalse(session.intent(Intent.SWITCH_2, start + 75), "Burst swap window is not ready before76");
            context.assertTrue(session.intent(Intent.SWITCH_2, start + 76), "Switch out at sourced first swap trial");
            double oldHp = target.hp();
            player.teleportTo(original.x + 6, original.y, original.z);
            var followed = hilichurl(context, player.position().add(0, 0, 2.5));
            var followedTarget = runtime.target(followed);
            session.advanceTo(start + 88);
            close(context, followedTarget.hp(), 885.200 - icicle,
                    "Waltz follows active Amber and still deals Kaeya-owned damage after switching");
            close(context, target.hp(), oldHp, "Orbit follows active player rather than remaining at cast position");
            session.advanceTo(start + 250);
            context.assertTrue(followedTarget.hp() < 885.200 - icicle, "Icicle contacts deal damage over time off-field");
            context.assertValueEqual(session.party().kit(2).burstReadyFrame(), start + 48 + 900, "Off-field burst keeps15-second cooldown");
            session.advanceTo(start + 529);
            double beforeShatter = followedTarget.hp();
            session.advanceTo(start + 532);
            double shatterDamage = beforeShatter - followedTarget.hp();
            context.assertTrue(Math.abs(shatterDamage - icicle) < .0001 || Math.abs(shatterDamage - icicle * 1.68) < .0001,
                    "Final contact-shatter is one77.6% Cryo hit (possibly crit) despite this icicle's recent ordinary hit lock");
            double endedHp = followedTarget.hp();
            session.advanceTo(start + 700);
            close(context, followedTarget.hp(), endedHp, "Eight-second orbit including final shatter has ended");
        });
        context.succeed();
    }
    public static void glacialWaltzIgnoresBuffGainedAfterCast(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = now(context);
            context.assertTrue(session.intent(Intent.SWITCH_3, start), "Select Kaeya without Thrilling Tales");
            session.kit().grantEnergy(60);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start), "Cast unbuffed Waltz");
            session.advanceTo(start + 51);
            var first = hilichurl(context, player.position().add(0, 0, 2.5));
            session.advanceTo(start + 52);
            double icicle = (48.04 + 94) * .776 * .5 * .9; // kaeya.md / damage.md, Lv20 target.
            close(context, runtime.target(first).hp(), 885.200 - icicle, "Unbuffed cast-time icicle value");
            first.discard();
            context.assertTrue(session.intent(Intent.SWITCH_4, start + 76), "Switch to Lisa while Waltz persists");
            context.assertTrue(session.intent(Intent.SWITCH_3, start + 136), "Real Lisa-to-Kaeya switch grants Thrilling Tales");
            close(context, session.kit().stats().atk(), (48.04 + 94) * 1.24, "Live Kaeya really gains24% ATK");
            session.advanceTo(start + 171);
            var later = hilichurl(context, player.position().add(0, 0, 2.5));
            session.advanceTo(start + 172);
            close(context, runtime.target(later).hp(), 885.200 - icicle,
                    "Waltz ignores Thrilling Tales gained after cast");
            later.discard();
            session.advanceTo(start + 531);
            var shatter = hilichurl(context, player.position().add(0, 0, 2.5));
            session.advanceTo(start + 532);
            close(context, runtime.target(shatter).hp(), 885.200 - icicle, "Final shatter uses the same unbuffed snapshot");
            shatter.discard();
        });
        context.succeed();
    }

    public static void glacialWaltzKeepsBuffAfterExpiry(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = now(context);
            context.assertTrue(session.intent(Intent.SWITCH_4, start), "Select Lisa");
            context.assertTrue(session.intent(Intent.SWITCH_3, start + 60), "Grant Kaeya the real ten-second Thrilling Tales buff");
            session.kit().grantEnergy(60);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start + 600), "Cast Waltz with one second of buff remaining");
            session.advanceTo(start + 651);
            var first = hilichurl(context, player.position().add(0, 0, 2.5));
            session.advanceTo(start + 652);
            double icicle = (48.04 + 94) * 1.24 * .776 * .5 * .9;
            close(context, runtime.target(first).hp(), 885.200 - icicle, "Buffed cast-time icicle value");
            first.discard();
            session.advanceTo(start + 660);
            close(context, session.kit().stats().atk(), 48.04 + 94, "Live Kaeya loses24% ATK at exact buff expiry");
            session.advanceTo(start + 771);
            var later = hilichurl(context, player.position().add(0, 0, 2.5));
            session.advanceTo(start + 772);
            close(context, runtime.target(later).hp(), 885.200 - icicle, "Waltz retains cast-time Thrilling Tales after expiry");
            later.discard();
            session.advanceTo(start + 1131);
            var shatter = hilichurl(context, player.position().add(0, 0, 2.5));
            session.advanceTo(start + 1132);
            close(context, runtime.target(shatter).hp(), 885.200 - icicle, "Final shatter keeps expired cast-time buff");
            shatter.discard();
            session.kit().grantEnergy(60);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start + 1548), "Next Waltz casts after its cooldown with no buff");
            session.advanceTo(start + 1599);
            var recast = hilichurl(context, player.position().add(0, 0, 2.5));
            session.advanceTo(start + 1600);
            close(context, runtime.target(recast).hp(), 885.200 - (48.04 + 94) * .776 * .5 * .9,
                    "A new cast replaces rather than reuses the previous buffed snapshot");
            recast.discard();
        });
        context.succeed();
    }

    public static void simultaneousBurstsKeepCharacterIcdSeparate(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2.5));
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_3);
            session.kit().grantEnergy(60);
            context.assertTrue(runtime.receive(player, Intent.BURST_PRESS), "Kaeya orbit accepted");
            context.assertTrue(session.intent(Intent.SWITCH_1, start + 76), "Switch to Traveler while Waltz persists");
            session.kit().grantEnergy(60);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start + 76), "Traveler burst accepted alongside Waltz");
            session.advanceTo(start + 171);
            context.assertValueEqual(target.swirlCount(), 0, "Traveler first burst tick has not landed before original96-frame offset");
            context.assertTrue(target.aura().gauge(Element.CRYO) > 0, "Off-field Waltz supplies actual Cryo aura before tornado contact");
            session.advanceTo(start + 172);
            context.assertValueEqual(target.swirlCount(), 1, "Both Elemental Burst tags are independent per character, so first Anemo tick Swirls");
            context.assertValueEqual(session.burstAbsorbedElement(), Element.CRYO, "Tornado absorbs off-field Waltz Cryo");
        });
        context.succeed();
    }

    public static void frozenTargetCannotMove(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 2));
            enemy.setNoAi(false);
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = now(context);
            target.aura().applyHit(Element.HYDRO, 2, start);
            runtime.receive(player, Intent.SWITCH_3);
            runtime.receive(player, Intent.SKILL_PRESS);
            session.advanceTo(start + 28);
            context.assertTrue(target.aura().frozenGauge() > 0, "Frostgnaw on Hydro forms rules-layer Frozen gauge");
            // elements.md: Hydro2U starts at1.6U and decays over12s; Frozen F=2*remaining Hydro.
            double expectedFrozen = 2 * (1.6 - 28 * 1.6 / (12 * 60));
            close(context, target.aura().frozenGauge(), expectedFrozen, "Frozen uses actual remaining Hydro without another aura tax");
            Vec3 frozenAt = enemy.position();
            for (int tick = 0; tick < 20; tick++) {
                enemy.setDeltaMovement(.3, .2, 0);
                enemy.move(MoverType.SELF, new Vec3(.3, .2, 0));
                HilichurlGameTests.tick(context, enemy);
                context.assertValueEqual(enemy.position(), frozenAt, "Frozen blocks both AI movement and external impulses");
                context.assertFalse(enemy.isWindingUp(), "Frozen target cannot start a club attack");
            }
            context.assertFalse(enemy.isNoAi(), "Freeze never overwrites the existing NoAI flag");
            long expiry = start + 28 + (long) Math.ceil((2 * Math.sqrt(5 * expectedFrozen + 4) - 4) * 60);
            session.advanceTo(expiry);
            target.aura().advanceTo(expiry);
            context.assertFalse(target.aura().isFrozen(), "Nonlinear Frozen gauge naturally expires");
            enemy.move(MoverType.SELF, new Vec3(.3, 0, 0));
            context.assertTrue(enemy.position().distanceToSqr(frozenAt) > .01, "Target regains movement after gauge reaches zero");
            HilichurlGameTests.tick(context, enemy);
            context.assertTrue(enemy.isWindingUp(), "Unfrozen AI resumes ordinary club behavior");
        });
        context.succeed();
    }

    private static Hilichurl hilichurl(GameTestHelper context, Vec3 position) {
        var enemy = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
        enemy.setGenshinLevel(20); // This existing golden encounter intentionally stays Lv20.
        enemy.snapTo(position);
        enemy.setNoAi(true);
        context.assertTrue(context.getLevel().addFreshEntity(enemy), "Kaeya test hilichurl spawned");
        return enemy;
    }
    private static long now(GameTestHelper context) { return Frames.atServerTick(context.getLevel().getServer().getTickCount()); }
    private static double talent(CombatTarget target, double attack, double multiplier, double resistance) {
        // damage.md outgoing DEF formula written in raw target DEF form; no Damage helper under test.
        // damage.md: Harbinger20/20 base94, Lisa's Thrilling Tales20/20 base94; selected buff is passed explicitly.
        double defenseMultiplier = 5 * (20.0 + 100) / (5 * (20 + 100) + target.defense());
        double resMultiplier = resistance < 0 ? 1 - resistance / 2 : 1 - resistance;
        return attack * multiplier * defenseMultiplier * resMultiplier;
    }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .0001, message + ": expected " + expected + ", actual " + actual);
    }
}

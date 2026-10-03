package io.github.brainage04.genshininminecraft.gametest;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.AmberKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real server aim, puppet AI, rain regions and AuraState reactions. Expectations use spec arithmetic. */
public final class AmberGameTests {
    private AmberGameTests() {}
    public static void chargedAimedShotDamageAndPyro(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 4), false);
            var target = runtime.target(enemy);
            var session = runtime.session(player);
            long start = now(context);
            context.assertTrue(runtime.receive(player, Intent.SWITCH_2), "Select Amber");
            double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            context.assertTrue(runtime.receive(player, Intent.ATTACK_PRESS), "Hold bow attack");
            session.advanceTo(start + 85);
            close(context, target.hp(), 885.200, "Holding aim must not also launch a normal arrow");
            close(context, player.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * .5, "Server aim slows movement by named adaptation");
            context.assertTrue(session.intent(Intent.ATTACK_RELEASE, start + 86), "Release sourced full-charge86");
            session.advanceTo(start + 86);
            close(context, target.hp(), 885.200 - talent(1.24), "Talent1 124% ATK Pyro uses equal-level DEF and10% RES");
            close(context, target.aura().gauge(Element.PYRO), 2 * .8, "Fully charged shot applies sourced2U with aura tax");
            close(context, session.stamina().current(), 100, "Ground bow aim costs zero stamina");
            close(context, player.getAttributeValue(Attributes.MOVEMENT_SPEED), speed, "Release removes server aim slowdown");
            context.assertTrue(session.intent(Intent.ATTACK_PRESS, start + 96), "Second bow hold");
            session.intent(Intent.ATTACK_RELEASE, start + 111);
            session.advanceTo(start + 111);
            close(context, target.hp(), 885.200 - talent(1.24) - talent(.4386), "Uncharged aimed shot is43.86% Physical, not Pyro");
        });
        context.succeed();
    }
    public static void baronBunnyTauntsTakesClubAndExpires(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 4), true);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_2);
            context.assertTrue(runtime.receive(player, Intent.SKILL_PRESS), "Explosive Puppet accepted");
            session.advanceTo(start + 44);
            context.assertTrue(bunnies(context, player.position()).isEmpty(), "No entity before adapted original landing45");
            session.advanceTo(start + 45);
            Rabbit bunny = bunnies(context, player.position()).getFirst();
            close(context, bunny.getMaxHealth(), 2037.88 * .4136, "Bunny HP snapshots sourced41.36% Amber maxHP");
            close(context, session.kit().energy(), 0, "Placement generates no particles");
            HilichurlGameTests.tick(context, enemy);
            context.assertTrue(enemy.getTarget() == bunny, "Actual hilichurl target becomes Baron Bunny");
            for (int tick = 0; tick < 10; tick++) HilichurlGameTests.tick(context, enemy);
            double incoming = 120 * 600 / (129.37 + 600);
            close(context, ((AmberKit) session.kit()).puppetHp(), 2037.88 * .4136 - incoming,
                    "Real telegraphed club damages puppet using adapted Amber DEF inheritance");
            context.assertTrue(session.intent(Intent.SWITCH_3, start + 60), "Puppet persists off-field");
            session.advanceTo(start + 524);
            context.assertTrue(bunny.isAlive() && !bunny.isRemoved(), "Puppet lives until eight seconds from landing");
            close(context, runtime.target(enemy).hp(), 885.200, "No explosion damage before lifetime boundary");
            session.advanceTo(start + 525);
            context.assertTrue(bunny.isRemoved(), "Expired puppet removed");
            close(context, runtime.target(enemy).hp(), 885.200 - talent(1.232), "123.2% ATK explosion remains Amber-owned off-field");
            close(context, runtime.target(enemy).aura().gauge(Element.PYRO), 1.6, "Explosion applies2U Pyro without ICD");
            close(context, session.party().kit(1).energy(), 4 * 1.8, "Off-field Amber gets four same-element particles");
            close(context, session.kit().energy(), 4, "On-field Kaeya gets four different-element particles");
            HilichurlGameTests.tick(context, enemy);
            context.assertTrue(enemy.getTarget() == player, "Taunt ends and live player is reacquired");
        });
        context.succeed();
    }
    public static void destroyedBunnyExplodesOnceAndMissGrantsNoEnergy(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 4), false);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_2);
            runtime.receive(player, Intent.SKILL_PRESS);
            session.advanceTo(start + 45);
            Rabbit bunny = bunnies(context, player.position()).getFirst();
            bunny.hurtServer(context.getLevel(), context.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
            session.advanceTo(start + 46);
            close(context, runtime.target(enemy).hp(), 885.200 - talent(1.232), "World destruction detonates immediately");
            close(context, session.kit().energy(), 12, "Four qualifying Pyro particles give active Amber12");
            session.advanceTo(start + 525);
            close(context, runtime.target(enemy).hp(), 885.200 - talent(1.232), "Old expiry cannot replay destroyed explosion");
            enemy.discard();
            context.assertFalse(session.intent(Intent.SKILL_PRESS, start + 904), "C0 has no spare charge");
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start + 905), "Fifteen-second cooldown from frame5");
            session.advanceTo(start + 1430);
            close(context, session.kit().energy(), 12, "Explosion missing every enemy grants no particles");
        });
        context.succeed();
    }
    public static void fieryRainDamageOverTimePreservesBlocks(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            Vec3 center = player.position().add(0, 0, 3);
            var inner = hilichurl(context, center, false);
            var outer = hilichurl(context, center.add(2.5, 0, 0), false);
            var high = hilichurl(context, center.add(0, 4, 0), false);
            Map<BlockPos, BlockState> blocks = snapshot(context, player.blockPosition());
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_2);
            session.kit().grantEnergy(39);
            context.assertFalse(runtime.receive(player, Intent.BURST_PRESS), "Rain requires40 energy");
            close(context, session.kit().energy(), 39, "Rejected burst never spends energy");
            session.kit().grantEnergy(1);
            context.assertTrue(runtime.receive(player, Intent.BURST_PRESS), "Fiery Rain accepted");
            close(context, session.kit().energy(), 0, "Burst reserves40 on acceptance");
            session.advanceTo(start + 71);
            close(context, runtime.target(inner).hp(), 885.200, "First hit uses original trial72");
            session.advanceTo(start + 72);
            double first = runtime.target(inner).hp();
            close(context, first, 885.200 - talent(.2808), "First inner wave deals talent1 28.08%");
            session.advanceTo(start + 192);
            context.assertTrue(runtime.target(inner).hp() < first, "Rain keeps dealing damage over time");
            var crits = new java.util.Random(0); // Fixture's UUID0; spec base5% crit/50% crit damage.
            double innerDamage = 0, outerDamage = 0;
            for (int wave = 0; wave < 18; wave++) {
                double damage = talent(.2808) * (crits.nextDouble() < .05 ? 1.5 : 1);
                if ((wave & 1) == 0) innerDamage += damage;
                else outerDamage += damage;
            }
            close(context, runtime.target(inner).hp(), 885.200 - innerDamage, "Nine of18 waves reach inner half with sourced seeded crit rolls");
            close(context, runtime.target(outer).hp(), 885.200 - outerDamage, "Other nine waves reach outer half with sourced seeded crit rolls");
            close(context, runtime.target(high).hp(), 885.200, "Elevated target is outside rain damage region");
            context.assertTrue(runtime.target(inner).aura().gauge(Element.PYRO) > 0, "Rain applies1U Pyro using shared Amber ICD");
            context.assertValueEqual(session.kit().burstReadyFrame(), start + 56 + 720, "12-second cooldown from original56");
            assertUnchanged(context, blocks);
        });
        context.succeed();
    }
    public static void amberOverloadsLisasElectro(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 4), false);
            var neighbor = hilichurl(context, enemy.position().add(1.5, 0, 0), false);
            var session = runtime.session(player);
            var target = runtime.target(enemy);
            Map<BlockPos, BlockState> blocks = snapshot(context, player.blockPosition());
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_4);
            runtime.receive(player, Intent.ATTACK_PRESS);
            runtime.receive(player, Intent.ATTACK_RELEASE);
            session.advanceTo(start + 26);
            context.assertTrue(target.aura().gauge(Element.ELECTRO) > 0, "Lisa's real normal applied Electro");
            session.intent(Intent.SWITCH_2, start + 60);
            shoot(session, start + 60);
            double overloaded = 2.75 * 80.584775 * .9; // damage.md current coefficient, no DEF/crit.
            close(context, target.hp(), 885.200 - (49.87 + 23) * .396 * .5 * .9 - talent(1.24) - overloaded,
                    "Pyro on Lisa Electro triggers separate current Overloaded damage");
            close(context, runtime.target(neighbor).hp(), 885.200 - overloaded, "Overloaded has zero-gauge Pyro AoE");
            close(context, runtime.target(neighbor).aura().gauge(Element.PYRO), 0, "Reaction explosion does not seed Pyro aura");
            context.assertTrue(enemy.getDeltaMovement().horizontalDistanceSqr() <= .12 * .12 + 1e-8,
                    "Overloaded uses only minimal adapted horizontal knockback");
            assertUnchanged(context, blocks);
        });
        context.succeed();
    }
    public static void amberForwardMeltsKaeyasCryo(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 4), false);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_3);
            runtime.receive(player, Intent.SKILL_PRESS);
            session.advanceTo(start + 28);
            session.intent(Intent.SWITCH_2, start + 60);
            shoot(session, start + 60);
            close(context, runtime.target(enemy).hp(), 885.200 - talent(1.912) - 2 * talent(1.24),
                    "Forward Melt doubles Amber shot on actual Kaeya Cryo at0 EM");
            close(context, runtime.target(enemy).aura().gauge(Element.CRYO), 0, "Forward Melt consumes Cryo");
        });
        context.succeed();
    }
    public static void travelerSwirlsAmbersPyro(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 3), false);
            var session = runtime.session(player);
            long start = now(context);
            runtime.receive(player, Intent.SWITCH_2);
            shoot(session, start);
            session.intent(Intent.SWITCH_1, start + 96);
            session.intent(Intent.SKILL_PRESS, start + 96);
            session.intent(Intent.SKILL_RELEASE, start + 96);
            session.advanceTo(start + 128);
            double storm = (45.75 + 23) * 1.76 * .5 * .9;
            close(context, runtime.target(enemy).hp(), 885.200 - talent(1.24) - storm - .6 * 80.584775 * .9 - storm * .25,
                    "Traveler deals Pyro Swirl and absorption from actual aimed Pyro");
            context.assertValueEqual(runtime.target(enemy).swirlCount(), 1, "Actual Pyro aura swirls");
            context.assertValueEqual(session.skillAbsorbedElement(), Element.PYRO, "Palm absorbs Pyro");
        });
        context.succeed();
    }
    public static void amberVaporizesSeededHydro(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = hilichurl(context, player.position().add(0, 0, 4), false);
            var session = runtime.session(player);
            long start = now(context);
            runtime.target(enemy).aura().applyHit(Element.HYDRO, 2, start); // Hydro has no gameplay source in this slice.
            runtime.receive(player, Intent.SWITCH_2);
            shoot(session, start);
            close(context, runtime.target(enemy).hp(), 885.200 - 1.5 * talent(1.24), "Pyro on seeded Hydro reverse-Vaporizes1.5");
        });
        context.succeed();
    }
    private static void shoot(CombatRuntime.Session session, long frame) {
        if (!session.intent(Intent.ATTACK_PRESS, frame) || !session.intent(Intent.ATTACK_RELEASE, frame + 86))
            throw new AssertionError("Charged aimed-shot input was rejected");
        session.advanceTo(frame + 86);
    }
    private static Hilichurl hilichurl(GameTestHelper context, Vec3 position, boolean ai) {
        var enemy = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
        enemy.snapTo(position);
        enemy.setNoAi(!ai);
        context.assertTrue(context.getLevel().addFreshEntity(enemy), "Amber test hilichurl spawned");
        return enemy;
    }
    private static java.util.List<Rabbit> bunnies(GameTestHelper context, Vec3 point) {
        return context.getLevel().getEntitiesOfClass(Rabbit.class, new AABB(point, point).inflate(10), entity -> !entity.isRemoved());
    }
    private static Map<BlockPos, BlockState> snapshot(GameTestHelper context, BlockPos center) {
        var result = new HashMap<BlockPos, BlockState>();
        for (int x = -6; x <= 6; x++) for (int y = -1; y <= 5; y++) for (int z = -6; z <= 6; z++) {
            BlockPos pos = center.offset(x, y, z);
            result.put(pos, context.getLevel().getBlockState(pos));
        }
        return result;
    }
    private static void assertUnchanged(GameTestHelper context, Map<BlockPos, BlockState> blocks) {
        blocks.forEach((pos, before) -> {
            BlockState after = context.getLevel().getBlockState(pos);
            context.assertTrue(before.equals(after), "Pyro never destroys or alters blocks at " + pos);
            context.assertFalse(after.is(Blocks.FIRE) || after.is(Blocks.SOUL_FIRE), "No fire blocks in Pyro area at " + pos);
        });
    }
    private static double talent(double multiplier) { return (48.04 + 23) * multiplier * .5 * .9; }
    private static long now(GameTestHelper context) { return Frames.atServerTick(context.getLevel().getServer().getTickCount()); }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .0001, message + ": expected " + expected + ", actual " + actual);
    }
}

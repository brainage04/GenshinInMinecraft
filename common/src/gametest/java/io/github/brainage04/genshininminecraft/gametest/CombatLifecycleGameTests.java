package io.github.brainage04.genshininminecraft.gametest;

import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.Party;
import io.github.brainage04.genshininminecraft.rules.kit.AmberKit;
import io.github.brainage04.genshininminecraft.rules.kit.KaeyaKit;
import io.github.brainage04.genshininminecraft.rules.kit.LisaKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Session lifetime boundaries must cancel casts, not merely suppress hits in the wrong level. */
public final class CombatLifecycleGameTests {
    private CombatLifecycleGameTests() {}

    public static void dimensionChangePermanentlyCancelsFieldObjects(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            Vec3 origin = player.position();
            var enemy = enemy(context, origin.add(0, 0, 3));
            var target = runtime.target(enemy);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            context.assertTrue(session.intent(Intent.SWITCH_2, start), "Select Amber");
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start), "Cast Bunny");
            session.advanceTo(start + 45);
            Rabbit bunny = bunnies(context, origin).getFirst();
            context.assertTrue(session.intent(Intent.SWITCH_4, start + 60), "Select Lisa while Bunny persists");
            session.kit().grantEnergy(80);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start + 60), "Cast Rose");
            context.assertTrue(session.intent(Intent.SWITCH_2, start + 120), "Return to Amber");
            session.kit().grantEnergy(40);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start + 120), "Cast Rain");
            context.assertTrue(session.intent(Intent.SWITCH_3, start + 181), "Select Kaeya while fields persist");
            session.kit().grantEnergy(60);
            context.assertTrue(session.intent(Intent.BURST_PRESS, start + 181), "Cast Waltz");
            session.advanceTo(start + 234);
            context.assertTrue(target.hp() < target.maxHp(), "Precondition: fields have hit the real target");
            var amber = (AmberKit) session.party().kit(1);
            var kaeya = (KaeyaKit) session.party().kit(2);
            var lisa = (LisaKit) session.party().kit(3);
            context.assertTrue(amber.puppetAlive() && kaeya.burstActive(start + 234) && lisa.roseActive(start + 234),
                    "Precondition: Bunny, Waltz and Rose are live before dimension change");
            double hp = target.hp();
            double[][] resources = new double[Party.SIZE][4];
            for (int slot = 0; slot < Party.SIZE; slot++) {
                var kit = session.party().kit(slot);
                resources[slot] = new double[] {kit.hp(), kit.energy(), kit.skillReadyFrame(), kit.burstReadyFrame()};
            }
            ServerLevel other = context.getLevel().getServer().getLevel(Level.NETHER);
            context.assertTrue(other != null, "Test server provides a second dimension");
            teleport(context, player, other, origin);
            session.advanceTo(start + 235);
            context.assertTrue(bunny.isRemoved(), "Leaving the original dimension discards Bunny immediately, before expiry");
            teleport(context, player, context.getLevel(), origin);
            session.advanceTo(start + 236);
            context.assertTrue(runtime.session(player) == session, "Dimension travel retains the party session");
            session.advanceTo(start + 1020);
            close(context, target.hp(), hp, "Returning before expiry cannot resume any cancelled field hits");
            context.assertTrue(bunny.isRemoved() && bunnies(context, origin).isEmpty(), "Dimension change discards the Bunny entity");
            context.assertFalse(amber.puppetAlive(), "Cancelled Bunny is invalidated in its rules kit");
            context.assertFalse(kaeya.burstActive(start + 236), "Cancelled Waltz cannot become active after returning");
            context.assertFalse(lisa.roseActive(start + 236), "Cancelled Rose cannot become active after returning");
            for (int slot = 0; slot < Party.SIZE; slot++) {
                var kit = session.party().kit(slot);
                close(context, kit.hp(), resources[slot][0], "Dimension travel preserves member HP");
                close(context, kit.energy(), resources[slot][1], "Cancelled fields grant no further energy");
                close(context, kit.skillReadyFrame(), resources[slot][2], "Dimension travel preserves skill cooldown deadline");
                close(context, kit.burstReadyFrame(), resources[slot][3], "Dimension travel preserves burst cooldown deadline");
            }
        });
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            Vec3 origin = player.position();
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            session.intent(Intent.SWITCH_2, start);
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start), "Cast a not-yet-landed Bunny");
            teleport(context, player, context.getLevel().getServer().getLevel(Level.NETHER), origin);
            session.advanceTo(start + 1);
            teleport(context, player, context.getLevel(), origin);
            session.advanceTo(start + 45);
            context.assertTrue(bunnies(context, origin).isEmpty(), "A cancelled queued landing cannot create a new Bunny on return");
            context.assertFalse(((AmberKit) session.kit()).puppetAlive(), "Queued landing generation stays invalidated");
        });
        context.succeed();
    }

    public static void partyWipeDiscardsLandedBunny(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            var enemy = enemy(context, player.position().add(0, 0, 3));
            var target = runtime.target(enemy);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            session.intent(Intent.SWITCH_2, start);
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start), "Cast Bunny before party wipe");
            session.advanceTo(start + 45);
            Rabbit bunny = bunnies(context, player.position()).getFirst();
            for (int member = 0; member < Party.SIZE; member++) {
                context.assertTrue(runtime.enemyHit(player, enemy, 120, 1000, 20), "Lethal hit falls the next active member");
            }
            context.assertFalse(player.isAlive(), "Precondition: party wiped and owner remains dead");
            session.advanceTo(start + 526);
            context.assertTrue(bunny.isRemoved() && bunnies(context, player.position()).isEmpty(),
                    "Dead-owner expiry must not leave a landed Rabbit entity behind");
            close(context, target.hp(), target.maxHp(), "Party-wipe cleanup cannot deal explosion damage");
            context.assertFalse(((AmberKit) session.party().kit(1)).puppetAlive(), "Dead party has no live puppet rules state");
        });
        context.succeed();
    }

    private static void teleport(GameTestHelper context, ServerPlayer player, ServerLevel level, Vec3 point) {
        context.assertTrue(player.teleportTo(level, point.x, point.y, point.z, Set.of(), 0, 0, false), "Real dimension teleport succeeds");
        context.assertTrue(player.level() == level, "Owner moved to the requested dimension");
    }
    private static Hilichurl enemy(GameTestHelper context, Vec3 point) {
        var enemy = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
        enemy.snapTo(point);
        enemy.setNoAi(true);
        context.assertTrue(context.getLevel().addFreshEntity(enemy), "Lifetime target spawned");
        return enemy;
    }
    private static java.util.List<Rabbit> bunnies(GameTestHelper context, Vec3 point) {
        return context.getLevel().getEntitiesOfClass(Rabbit.class, new AABB(point, point).inflate(10), entity -> !entity.isRemoved());
    }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .0001, message + ": expected " + expected + ", actual " + actual);
    }
}

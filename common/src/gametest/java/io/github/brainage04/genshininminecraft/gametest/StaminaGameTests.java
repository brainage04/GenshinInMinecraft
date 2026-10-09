package io.github.brainage04.genshininminecraft.gametest;

import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/** Real vanilla movement input, combat scheduling and hilichurl swing paths, on both loaders. */
public final class StaminaGameTests {
    private static final Input FORWARD_SPRINT = new Input(true, false, false, false, false, false, true);
    private StaminaGameTests() {}

    public static void sprintDrainsAndStopsAtZero(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            player.setOnGround(true);
            player.setLastClientInput(FORWARD_SPRINT);
            session.tickMovement(start);
            close(context, session.stamina().current(), 82, "Wiki dash/sprint start costs18 exactly once");
            context.assertTrue(player.isSprinting(), "Holding vanilla sprint starts managed sprinting");
            session.tickMovement(start + 18);
            close(context, session.stamina().current(), 82, "Dash animation does not also incur held-sprint drain");
            for (int tick = 1; tick <= 20; tick++) session.tickMovement(start + 18 + tick * 3L);
            close(context, session.stamina().current(), 64, "Wiki held sprint drains18 per second");
            for (int tick = 21; tick <= 92; tick++) session.tickMovement(start + 18 + tick * 3L);
            close(context, session.stamina().current(), 0, "Continuous sprint drains to zero, not below it");
            context.assertTrue(session.stamina().exhausted(), "Empty pool locks stamina-dependent actions");
            context.assertFalse(player.isSprinting(), "Server force-stops sprint at zero");
            player.setSprinting(true); // A vanilla start-state packet cannot override the exhaustion gate.
            session.tickMovement(start + 297);
            context.assertFalse(player.isSprinting(), "Repeated vanilla sprint state cannot bypass exhaustion");
            context.assertFalse(session.stamina().dash(start + 297), "Dash cannot spend an empty pool");
        });
        context.succeed();
    }

    public static void dashCostsStaminaAndDodgesClub(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var member = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
            member.setGenshinLevel(20);
            member.snapTo(player.position().add(0, 0, 1.5));
            member.setCamp(member.position(), member.position());
            context.assertTrue(context.getLevel().addFreshEntity(member), "Hilichurl spawned for a real swing");
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            HilichurlGameTests.tick(context, member);
            context.assertTrue(member.isWindingUp(), "Precondition: hilichurl telegraphs its club swing");
            for (int tick = 0; tick < 8; tick++) HilichurlGameTests.tick(context, member);
            player.setOnGround(true);
            // Sideways input verifies movement direction, rather than always dashing camera-forward.
            player.setLastClientInput(new Input(false, false, true, false, false, false, true));
            session.tickMovement(start);
            close(context, session.stamina().current(), 82, "Current wiki dash costs18");
            close(context, player.getDeltaMovement().x, .6, "Left strafe at yaw0 receives server-owned +X dash motion");
            close(context, player.getDeltaMovement().z, 0, "Dash follows WASD, not camera-forward");
            Vec3 impulse = player.getDeltaMovement();
            session.tickMovement(start);
            close(context, session.stamina().current(), 82, "Repeated work in one frame cannot charge another dash");
            context.assertValueEqual(player.getDeltaMovement(), impulse, "Repeated work cannot amplify the dash impulse");
            // Offline fixture player remains in the locked melee arc; immunity, not a geometric miss,
            // must prevent the landing. Advance the combat clock alongside genuine enemy AI ticks.
            for (int tick = 1; tick <= 2; tick++) {
                session.advanceTo(start + tick * 3L);
                HilichurlGameTests.tick(context, member);
            }
            context.assertFalse(member.isWindingUp(), "The club actually lands during the active dash window");
            close(context, session.kit().hp(), 2342.391079, "A real hilichurl swing inside dash i-frames deals no damage");
            for (int tick = 3; tick <= 41; tick++) {
                session.advanceTo(start + tick * 3L);
                HilichurlGameTests.tick(context, member);
                close(context, session.kit().hp(), 2342.391079, "Recovery/second wind-up cannot deal early damage");
            }
            session.advanceTo(start + 126);
            HilichurlGameTests.tick(context, member);
            double clubDamage = 301.04993664 * 600 / (147.011025 + 600); // datamine.md:22.608*13.31608;57.225*2.569.
            close(context, session.kit().hp(), 2342.391079 - clubDamage,
                    "A subsequent real swing outside the six-tick window deals spec DEF-mitigated damage");
            member.discard();
            // Camera-decoupled strafing regression: a body that has turned left must not rotate A twice.
            player.setLastClientInput(Input.EMPTY);
            session.tickMovement(start + 129);
            player.setYRot(-90);
            context.assertTrue(runtime.receiveCameraYaw(player, 0), "Managed camera basis is accepted");
            context.assertFalse(runtime.receiveCameraYaw(player, Float.NaN), "NaN camera yaw is rejected");
            context.assertFalse(runtime.receiveCameraYaw(player, Float.POSITIVE_INFINITY), "Infinite camera yaw is rejected");
            player.setLastClientInput(new Input(false, false, true, false, false, false, true));
            session.tickMovement(start + 132);
            close(context, player.getDeltaMovement().x, .6, "A at camera yaw0 still dashes+X with body yaw-90");
            close(context, player.getDeltaMovement().z, 0, "Invalid camera input cannot replace the last finite basis");
            close(context, player.getYRot(), -90, "Camera movement intent never changes server combat-facing yaw");
        });
        context.succeed();
    }

    public static void chargedAttackRequiresStamina(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            context.assertTrue(session.stamina().consume(100, start), "Exhaust the new-player100 pool");
            var target = context.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(1, 2, 1));
            target.snapTo(player.position().add(0, 0, 2));
            target.setNoAi(true);
            try {
                context.assertTrue(runtime.receive(player, Intent.ATTACK_PRESS), "Normal attacks remain free at zero stamina");
                session.advanceTo(start + 49); // N1 at13; held charge would have landed at38 and49.
                double normalDamage = (45.748752 + 93.753946) * .445 * .5 * .9;
                close(context, runtime.target(target).hp(), 2000 - normalDamage,
                        "Zero-stamina hold deals only sourced N1 damage, neither charged hit");
                close(context, session.stamina().current(), 0, "Rejected charge cannot overdraft or reset recovery");
            } finally { target.discard(); }
        });
        context.succeed();
    }

    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .0001, message + ": expected " + expected + ", actual " + actual);
    }
}

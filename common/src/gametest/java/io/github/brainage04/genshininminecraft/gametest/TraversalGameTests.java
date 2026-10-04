package io.github.brainage04.genshininminecraft.gametest;

import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.Traversal;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.combat.TraversalGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;

/** Real collision landings and vanilla input feed the shared managed server adapter on both loaders. */
public final class TraversalGameTests {
    private static final Input UP = new Input(true, false, false, false, false, false, false);
    private static final Input JUMP = new Input(false, false, false, false, true, false, false);
    private TraversalGameTests() {}
    public static void safeFallDoesNotDamage(GameTestHelper context) { fall(context, 4, 0, false); }
    public static void damagingFallLosesHalfMaxHp(GameTestHelper context) { fall(context, 15, .5, false); }
    public static void lethalFallForcesCharacterSwitch(GameTestHelper context) { fall(context, 25, 1, false); }
    public static void waterLandingNegatesFall(GameTestHelper context) { fall(context, 25, 0, true); }
    private static void fall(GameTestHelper context, int height, double fraction, boolean water) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            Vec3 ground = player.position();
            Map<BlockPos, BlockState> saved = new HashMap<>();
            try {
                // Empty test structures still have framework markers above them; clear the complete
                // fall shaft and restore it, so a marker cannot shorten the observed fall.
                for (int y = 0; y <= height + 2; y++) for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++)
                    set(context, saved, player.blockPosition().offset(x, y, z),
                            water && y < 2 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
                player.snapTo(ground.add(0, height, 0));
                player.setOnGround(false);
                player.resetFallDistance();
                // Quarter-block collision steps are exactly representable: expectations are independent
                // of fallDistance rounding. ServerPlayer is client-authoritative, so invoke the same
                // observed-displacement accumulation entrypoint as ServerGamePacketListenerImpl.
                for (int tick = 0; tick <= height * 4 && !player.onGround(); tick++) {
                    double beforeY = player.getY();
                    player.setDeltaMovement(0, -.25, 0);
                    player.move(MoverType.SELF, player.getDeltaMovement());
                    player.doCheckFallDamage(0, player.getY() - beforeY, 0, player.onGround());
                }
                context.assertTrue(player.onGround(), "Fall reaches the solid floor through real collisions");
                close(context, player.getY(), ground.y, "Landing reaches the intended test floor, not a framework marker");
                if (fraction == 1) {
                    close(context, session.party().kit(0).hp(), 0, "25m adapted fall kills full-health Traveler");
                    context.assertValueEqual(session.party().activeSlot(), 1, "Lethal fall immediately switches to Amber");
                    context.assertTrue(player.isAlive(), "Living party prevents vanilla player death");
                    close(context, session.kit().hp(), session.kit().maxHp(), "Replacement member does not inherit landing damage");
                } else close(context, session.kit().hp(), 2342.39 * (1 - fraction),
                        water ? "Above-waist water negates lethal fall" : "Adapted height curve applies max-HP loss, not vanilla hearts");
            } finally { restore(context, saved); }
        });
        context.succeed();
    }
    public static void climbDrainsAndExhaustionFalls(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            Map<BlockPos, BlockState> saved = new HashMap<>();
            BlockPos base = player.blockPosition();
            try {
                for (int y = 0; y < 12; y++) for (int x = -1; x <= 1; x++)
                    set(context, saved, base.offset(x, y, 1), Blocks.STONE.defaultBlockState());
                player.snapTo(base.getX() + .5, base.getY() + 2, base.getZ() + .7);
                player.setOnGround(false);
                var session = runtime.session(player);
                long frame = Frames.atServerTick(context.getLevel().getServer().getTickCount());
                player.setLastClientInput(UP);
                session.tickMovement(frame);
                context.assertValueEqual(session.traversal().mode(), Traversal.Mode.CLIMB, "Moving into full wall attaches");
                double startY = player.getY();
                for (int tick = 1; tick <= 20; tick++) {
                    player.move(MoverType.SELF, player.getDeltaMovement());
                    session.tickMovement(frame + tick * 3L);
                }
                close(context, session.stamina().current(), 92, "Named8 units/s climbing drain");
                close(context, player.getY() - startY, 1.6, "Climb travels .08 blocks per tick");
                context.assertTrue(player.isNoGravity(), "Attached player cannot fall from vanilla gravity");
                session.stamina().consume(session.stamina().current() - .1, frame + 60);
                session.tickMovement(frame + 63);
                close(context, session.stamina().current(), 0, "Continuous climb clamps exhausted stamina to0");
                context.assertValueEqual(session.traversal().mode(), Traversal.Mode.FREE, "Empty stamina detaches");
                context.assertFalse(player.isNoGravity(), "Detachment restores gravity");
                double detachedY = player.getY();
                player.setDeltaMovement(0, -.08, 0);
                player.move(MoverType.SELF, player.getDeltaMovement());
                context.assertTrue(player.getY() < detachedY, "Exhausted climber actually falls, not suspended");
            } finally { restore(context, saved); }
        });
        context.succeed();
    }
    public static void glideDrainsAndDescends(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            Map<BlockPos, BlockState> saved = new HashMap<>();
            BlockPos base = player.blockPosition();
            try {
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 8; z++) for (int y = 0; y <= 12; y++)
                    set(context, saved, base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                var session = runtime.session(player);
                long frame = Frames.atServerTick(context.getLevel().getServer().getTickCount());
                player.snapTo(player.position().add(0, 10, 0));
                player.setOnGround(false);
                player.setLastClientInput(JUMP);
                session.tickMovement(frame);
                context.assertValueEqual(session.traversal().mode(), Traversal.Mode.GLIDE, "Midair jump with clearance opens glider");
                player.setLastClientInput(Input.EMPTY);
                double y = player.getY();
                for (int tick = 1; tick <= 20; tick++) {
                    close(context, player.getDeltaMovement().y, -.11755, "Each gliding tick has adapted historical descent velocity");
                    player.move(MoverType.SELF, player.getDeltaMovement());
                    session.tickMovement(frame + tick * 3L);
                }
                close(context, y - player.getY(), 2.351, "One second gliding descent is2.351 blocks");
                close(context, session.stamina().current(), 97, "Approximate sourced3 units/s even with no steering input");
                context.assertTrue(session.intent(Intent.TRAVERSAL_JUMP, frame + 63), "Quick released Jump intent is retained");
                session.tickMovement(frame + 63);
                context.assertValueEqual(session.traversal().mode(), Traversal.Mode.FREE, "Released-key Jump intent closes glider");
                context.assertFalse(player.isNoGravity(), "Voluntary close restores gravity");
                context.assertTrue(!player.onGround() && TraversalGeometry.clearance(player) >= 2,
                        "Redeploy fixture is genuinely airborne with2m clearance: " + player.position());
                context.assertTrue(session.intent(Intent.TRAVERSAL_JUMP, frame + 96), "After detach lock, a fresh Jump intent is accepted");
                session.tickMovement(frame + 96);
                context.assertValueEqual(session.traversal().mode(), Traversal.Mode.GLIDE, "Released-key Jump intent redeploys with clearance");
                session.stamina().consume(session.stamina().current() - .1, frame + 96);
                session.tickMovement(frame + 99);
                context.assertValueEqual(session.traversal().mode(), Traversal.Mode.FREE, "Exhaustion closes glider");
                context.assertFalse(player.isNoGravity(), "Closed glider resumes gravity");
            } finally { restore(context, saved); }
        });
        context.succeed();
    }
    public static void climbMantlesStepsAndRejectsOverhang(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            Map<BlockPos, BlockState> saved = new HashMap<>();
            BlockPos base = player.blockPosition();
            try {
                set(context, saved, base.south(), Blocks.STONE.defaultBlockState());
                player.snapTo(base.getX() + .5, base.getY(), base.getZ() + .7);
                player.setOnGround(true);
                var session = runtime.session(player);
                long frame = Frames.atServerTick(context.getLevel().getServer().getTickCount());
                player.setLastClientInput(UP);
                session.tickMovement(frame);
                session.tickMovement(frame + 3);
                context.assertValueEqual(session.traversal().mode(), Traversal.Mode.FREE, "Single-block step tops out");
                close(context, player.getY(), base.getY() + 1, "Mantle puts feet on ledge top");
                context.assertTrue(player.getZ() > base.getZ() + 1, "Mantle crosses onto ledge");
                for (int y = 0; y < 6; y++) set(context, saved, base.offset(0, y, 1), Blocks.STONE.defaultBlockState());
                set(context, saved, base.above(3), Blocks.STONE.defaultBlockState());
                player.snapTo(base.getX() + .5, base.getY() + 1.2, base.getZ() + .7);
                player.setOnGround(false);
                session.tickMovement(frame + 36);
                session.tickMovement(frame + 39);
                context.assertValueEqual(session.traversal().mode(), Traversal.Mode.FREE, "Solid roof prevents climbing through overhang");
                context.assertFalse(player.isNoGravity(), "Overhang detach restores falling");
                session.tickMovement(frame + 72);
                context.assertValueEqual(session.traversal().mode(), Traversal.Mode.CLIMB, "Can reattach after delay");
                player.snapTo(player.position().add(-3, 0, 0));
                session.tickMovement(frame + 75);
                context.assertValueEqual(session.traversal().mode(), Traversal.Mode.FREE, "Server rejects attachment no longer adjacent to wall");
            } finally { restore(context, saved); }
        });
        context.succeed();
    }
    private static void set(GameTestHelper context, Map<BlockPos, BlockState> saved, BlockPos pos, BlockState state) {
        saved.putIfAbsent(pos, context.getLevel().getBlockState(pos));
        context.getLevel().setBlock(pos, state, 2 | 16);
    }
    private static void restore(GameTestHelper context, Map<BlockPos, BlockState> saved) {
        saved.forEach((pos, state) -> context.getLevel().setBlock(pos, state, 2 | 16));
    }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .001, message + ": expected " + expected + ", actual " + actual);
    }
}

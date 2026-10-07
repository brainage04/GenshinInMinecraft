package io.github.brainage04.genshininminecraft.rules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocomotionTest {
    private final Locomotion state = new Locomotion();
    private void update(long frame, boolean ground, double vertical, boolean moving, boolean sprint, boolean dash,
            int mode, boolean climbJump, int forward, int side, double height) {
        state.update(frame, ground, vertical, moving, sprint, dash, false, mode, climbJump, forward, side, height);
    }
    @Test void groundedWalkingRunningAndExactlyEighteenFrameDash() {
        update(300, true, 0, false, false, false, 0, false, 0, 0, 0);
        assertEquals(Locomotion.Phase.IDLE, state.phase());
        update(303, true, 0, true, false, false, 0, false, 1, 0, 0);
        assertEquals("locomotion.walk", state.phase().clip());
        update(306, true, 0, true, true, true, 0, false, 1, 0, 0);
        assertEquals(Locomotion.Phase.DASH, state.phase());
        int occurrence = state.occurrence();
        update(321, true, 0, true, true, true, 0, false, 1, 0, 0);
        assertEquals(306, state.startFrame());
        assertEquals(occurrence, state.occurrence());
        assertEquals(.25, state.phase().seconds(321, state.startFrame()), 1e-12);
        update(324, true, 0, true, true, false, 0, false, 1, 0, 0);
        assertEquals(Locomotion.Phase.RUN, state.phase());
        update(327, true, 0, false, false, false, 0, false, 0, 0, 0);
        update(330, true, 0, true, true, true, 0, false, 1, 0, 0);
        assertTrue(state.occurrence() > occurrence);
        assertEquals(330, state.startFrame());
    }
    @Test void ascentFallSoftHardLandAndFiniteRecovery() {
        update(90, false, .2, true, false, false, 0, false, 1, 0, 0);
        assertEquals(Locomotion.Phase.JUMP, state.phase());
        update(108, false, -.2, false, false, false, 0, false, 0, 0, 2);
        assertEquals(Locomotion.Phase.FALL, state.phase());
        update(120, true, 0, false, false, false, 0, false, 0, 0, 2);
        assertEquals(Locomotion.Phase.LAND_SOFT, state.phase());
        update(132, true, 0, false, false, false, 0, false, 0, 0, 0);
        assertEquals(120, state.startFrame());
        update(135, true, 0, false, false, false, 0, false, 0, 0, 0);
        assertEquals(Locomotion.Phase.IDLE, state.phase());
        update(150, false, -.5, false, false, false, 0, false, 0, 0, 10);
        update(180, true, 0, false, false, false, 0, false, 0, 0, 10);
        assertEquals(Locomotion.Phase.LAND_HARD, state.phase());
        assertEquals(.5, state.phase().seconds(999, state.startFrame()), 1e-12);
        update(210, true, 0, false, false, false, 0, false, 0, 0, 0);
        assertEquals(Locomotion.Phase.IDLE, state.phase());
    }
    @Test void everyClimbDirectionJumpAndMantle() {
        update(3, false, 0, false, false, false, 1, false, 0, 0, 0);
        assertEquals(Locomotion.Phase.CLIMB_IDLE, state.phase());
        update(6, false, .08, true, false, false, 1, false, 1, 0, 0);
        assertEquals(Locomotion.Phase.CLIMB_UP, state.phase());
        update(9, false, -.08, true, false, false, 1, false, -1, 0, 0);
        assertEquals(Locomotion.Phase.CLIMB_DOWN, state.phase());
        update(12, false, 0, true, false, false, 1, false, 0, 1, 0);
        assertEquals(Locomotion.Phase.CLIMB_LEFT, state.phase());
        update(15, false, 0, true, false, false, 1, false, 0, -1, 0);
        assertEquals(Locomotion.Phase.CLIMB_RIGHT, state.phase());
        update(18, false, .24, true, false, false, 1, true, 1, 0, 0);
        assertEquals(Locomotion.Phase.CLIMB_JUMP, state.phase());
        state.begin(Locomotion.Phase.MANTLE, 36);
        update(39, true, 0, true, false, false, 0, false, 1, 0, 0);
        assertEquals(Locomotion.Phase.MANTLE, state.phase());
        update(60, true, 0, true, false, false, 0, false, 1, 0, 0);
        assertEquals(Locomotion.Phase.WALK, state.phase());
    }
    @Test void gliderStartLoopStopAndRedeploymentDoNotRestartOnSamples() {
        update(600, false, -.1, false, false, false, 2, false, 0, 0, 0);
        assertEquals(Locomotion.Phase.GLIDE_START, state.phase());
        update(615, false, -.1, false, false, false, 2, false, 0, 0, 0);
        assertEquals(600, state.startFrame());
        update(618, false, -.1, false, false, false, 2, false, 0, 0, 0);
        assertEquals(Locomotion.Phase.GLIDE_LOOP, state.phase());
        update(630, false, -.1, false, false, false, 0, false, 0, 0, 0);
        assertEquals(Locomotion.Phase.GLIDE_STOP, state.phase());
        update(639, false, -.2, false, false, false, 0, false, 0, 0, 1);
        assertEquals(630, state.startFrame());
        update(642, false, -.3, false, false, false, 0, false, 0, 0, 2);
        assertEquals(Locomotion.Phase.FALL, state.phase());
        update(645, false, -.1, false, false, false, 2, false, 0, 0, 0);
        assertEquals(Locomotion.Phase.GLIDE_START, state.phase());
        state.reset(648);
        assertEquals(Locomotion.Phase.IDLE, state.phase());
    }
    @Test void clockAlignmentLateTrackingLoopingAndRepeatedExtraction() {
        double frame = Locomotion.renderFrame(1200, 9000, 9002, .5F);
        assertEquals(1207.5, frame, 1e-12);
        assertEquals(.225, Locomotion.Phase.DASH.seconds(frame, 1194), 1e-12);
        assertEquals(.225, Locomotion.Phase.DASH.seconds(frame, 1194), 1e-12);
        assertEquals(.3, Locomotion.Phase.DASH.seconds(frame + 600, 1194), 1e-12);
        assertEquals(.125, Locomotion.Phase.WALK.seconds(1207.5, 1104), 1e-12);
        assertEquals(0, Locomotion.Phase.DASH.seconds(1100, 1194), 1e-12);
    }
    @Test void swimmingUsesItsOwnLoop() {
        state.update(30, false, 0, true, false, false, true, 0, false, 1, 0, 0);
        assertEquals(Locomotion.Phase.SWIM, state.phase());
        assertTrue(state.phase().loop());
    }
}

package io.github.brainage04.genshininminecraft.rules;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CameraMathTest {
    @Test void forwardUsesMinecraftCardinalYawRatherThanBodyYaw() {
        assertVector(0, 0, 1, 0, 1);
        assertVector(90, 0, 1, -1, 0);
        assertVector(180, 0, 1, 0, -1);
        assertVector(-90, 0, 1, 1, 0);
    }
    @Test void strafingBackwardAndDiagonalsRemainNormalized() {
        assertVector(90, 1, 0, 0, 1);
        assertVector(90, -1, 0, 0, -1);
        assertVector(90, 0, -1, 1, 0);
        assertVector(0, 1, 1, Math.sqrt(.5), Math.sqrt(.5));
        assertVector(0, 0, 0, 0, 0);
        assertVector(0, .3, .4, .3, .4); // Preserve analog/slowdown below unit length.
        assertEquals(45, CameraMath.movementYaw(90, 1, 1));
        assertEquals(-90, CameraMath.movementYaw(0, 1, 0));
    }
    @Test void smoothingUsesShortestWrappedArcAndNeverOvershoots() {
        assertEquals(24, CameraMath.smoothYaw(0, 90, 24));
        assertEquals(-24, CameraMath.smoothYaw(0, -90, 24));
        assertEquals(90, CameraMath.smoothYaw(80, 90, 24));
        assertEquals(190, CameraMath.smoothYaw(170, -170, 24));
        assertEquals(-190, CameraMath.smoothYaw(-170, 170, 24));
        assertEquals(0, CameraMath.smoothYaw(0, 90, 0));
    }
    @Test void lockHasSixBlockSphereAndCameraForwardConeBoundaries() {
        assertEquals(36, CameraMath.lockDistanceSquared(0, 0, 0, 6));
        assertEquals(Double.POSITIVE_INFINITY, CameraMath.lockDistanceSquared(0, 0, 0, 6.01));
        assertEquals(4, CameraMath.lockDistanceSquared(90, -2, 0, 0));
        assertEquals(Double.POSITIVE_INFINITY, CameraMath.lockDistanceSquared(90, 2, 0, 0));
        assertEquals(Double.POSITIVE_INFINITY, CameraMath.lockDistanceSquared(0, 0, 7, 1));
        assertEquals(Double.POSITIVE_INFINITY, CameraMath.lockDistanceSquared(0, 0, 1, 0));
        assertEquals(4, CameraMath.lockDistanceSquared(0, Math.sqrt(3), 0, 1), 1e-10); // Exactly60deg.
        assertEquals(Double.POSITIVE_INFINITY, CameraMath.lockDistanceSquared(0, Math.sqrt(3), 0, .99));
    }
    @Test void nearestEligibleTargetWinsAndEntityIdBreaksOnlyExactTies() {
        assertTrue(CameraMath.preferTarget(4, 20, 9, 1));
        assertFalse(CameraMath.preferTarget(9, 1, 4, 20));
        assertTrue(CameraMath.preferTarget(4, 1, 4, 20));
        assertFalse(CameraMath.preferTarget(4, 20, 4, 1));
        assertFalse(CameraMath.preferTarget(Double.POSITIVE_INFINITY, 1, Double.POSITIVE_INFINITY, 20));
        assertEquals(90, CameraMath.facingYaw(-2, 0));
        assertEquals(-90, CameraMath.facingYaw(2, 0));
    }
    private static void assertVector(float yaw, double left, double forward, double x, double z) {
        assertEquals(x, CameraMath.movementX(yaw, left, forward), 1e-10);
        assertEquals(z, CameraMath.movementZ(yaw, left, forward), 1e-10);
    }
}

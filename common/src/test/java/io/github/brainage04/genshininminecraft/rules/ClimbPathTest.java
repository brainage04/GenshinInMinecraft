package io.github.brainage04.genshininminecraft.rules;

import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;
import static io.github.brainage04.genshininminecraft.rules.ClimbPath.Wall.*;
import static org.junit.jupiter.api.Assertions.*;

class ClimbPathTest {
    private static final ClimbPath.Faces<BiPredicate<Integer, Integer>> MASK = (mask, x, z, wall) -> mask.test(x, z);
    private static final BiPredicate<Integer, Integer> PILLAR = (x, z) -> x >= 0 && x < 3 && z >= 0 && z < 3;
    private static ClimbPath.Step move(BiPredicate<Integer, Integer> mask, double x, double z,
            ClimbPath.Wall wall, int side, double distance) {
        return ClimbPath.resolve(mask, MASK, x, z, wall, side, distance, .3, .18);
    }
    private static void attachment(ClimbPath.Step actual, ClimbPath.Wall wall, double x, double z) {
        assertNotNull(actual);
        assertEquals(wall, actual.wall());
        assertEquals(x, actual.x(), 1e-9);
        assertEquals(z, actual.z(), 1e-9);
    }
    @Test void outsideCornerUsesRemainingDistanceOnPerpendicularFace() {
        attachment(move(PILLAR, 3.28, -.3, SOUTH, 1, .08), WEST, 3.3, -.24);
        attachment(move(PILLAR, -.28, -.3, SOUTH, -1, .08), EAST, -.3, -.24);
    }
    @Test void insideCornerTurnsBeforeBodyHitsProtrudingColumn() {
        BiPredicate<Integer, Integer> corner = (x, z) -> z >= 0 || x >= 3 && z >= -2;
        attachment(move(corner, 2.68, -.3, SOUTH, 1, .08), EAST, 2.7, -.36);
        // A delayed authoritative SOUTH normal must not snap the already-turned player back.
        attachment(move(corner, 2.7, -.36, SOUTH, 1, .08), EAST, 2.7, -.44);
    }
    @Test void outwardOneBlockStepFollowsConnectorThenOriginalNormal() {
        BiPredicate<Integer, Integer> step = (x, z) -> z >= (x < 3 ? 0 : 1);
        attachment(move(step, 2.5, -.3, SOUTH, 1, 2), SOUTH, 3.5, .7);
        attachment(move(step, 3.5, .7, SOUTH, -1, 2), SOUTH, 2.5, -.3);
    }
    @Test void inwardOneBlockStepFollowsConnectorThenOriginalNormal() {
        BiPredicate<Integer, Integer> step = (x, z) -> z >= (x < 3 ? 1 : 0);
        attachment(move(step, 2.5, .7, SOUTH, 1, 2), SOUTH, 3.5, -.3);
        attachment(move(step, 3.5, -.3, SOUTH, -1, 2), SOUTH, 2.5, .7);
    }
    @Test void holdingEitherSideCompletesFourFacesWithoutGainingDistance() {
        for (int side : new int[]{-1, 1}) {
            ClimbPath.Step step = new ClimbPath.Step(SOUTH, 1.5, -.3);
            var faces = java.util.EnumSet.noneOf(ClimbPath.Wall.class);
            for (int tick = 0; tick < 180; tick++) {
                ClimbPath.Step next = move(PILLAR, step.x(), step.z(), step.wall(), side, .08);
                assertNotNull(next, "Continuous contour attachment at tick" + tick);
                assertEquals(.08, Math.abs(next.x() - step.x()) + Math.abs(next.z() - step.z()), 1e-9);
                faces.add(next.wall());
                step = next;
            }
            assertEquals(4, faces.size());
            attachment(step, SOUTH, 1.5, -.3); // Expanded3×3 perimeter:4×(3+2×.3)=14.4 blocks.
        }
    }
    @Test void normalRecoveryAtOutsideCornerSupportsStalePacketAndIdleAttachment() {
        attachment(move(PILLAR, 3.3, .2, SOUTH, 1, .08), WEST, 3.3, .28);
        attachment(move(PILLAR, 3.3, -.3, SOUTH, 1, 0), WEST, 3.3, -.3);
        attachment(move(PILLAR, 3.3, -.3, WEST, 0, 0), WEST, 3.3, -.3);
    }
    @Test void actualVanillaContactSurvivesClearanceEpsilonAtLargeCoordinates() {
        int originX = 7572621, originZ = 12085466;
        BiPredicate<Integer, Integer> pillar = (x, z) -> x >= originX && x < originX + 3 && z >= originZ && z < originZ + 3;
        double radius = (double) .6F / 2 + Traversal.ADAPTED_COLLISION_EPSILON;
        ClimbPath.Step step = ClimbPath.resolve(pillar, MASK, originX + 1.5, originZ - (double) .6F / 2,
                SOUTH, 1, .08, radius, .18);
        assertNotNull(step);
        assertEquals(SOUTH, step.wall());
        assertEquals(originX + 1.58, step.x(), 1e-8);
        assertEquals(originZ - radius, step.z(), 1e-8);
    }
    @Test void MissingFaceOrDisconnectedWallCannotKeepAttachment() {
        assertNull(move((x, z) -> false, 1.5, -.3, SOUTH, -1, .08));
        assertNull(move(PILLAR, 5, -.3, SOUTH, -1, .08));
        assertNull(move(PILLAR, 1.5, -.49, SOUTH, -1, .08));
        assertNull(move((x, z) -> z >= 0 && x != 3, 3.5, -.3, SOUTH, -1, .08));
    }
}

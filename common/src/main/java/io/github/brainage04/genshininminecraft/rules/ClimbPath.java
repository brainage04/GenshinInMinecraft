package io.github.brainage04.genshininminecraft.rules;

/** Walks the exposed block contour expanded by the player's horizontal half-width. */
public final class ClimbPath {
    public enum Wall {
        NORTH(0, -1), SOUTH(0, 1), WEST(-1, 0), EAST(1, 0);
        public final int x, z;
        Wall(int x, int z) { this.x = x; this.z = z; }
        private Wall tangent(int side) {
            return x == 0 ? (z * side > 0 ? EAST : WEST) : (-x * side > 0 ? SOUTH : NORTH);
        }
        private Wall opposite() { return switch (this) {
            case NORTH -> SOUTH; case SOUTH -> NORTH; case WEST -> EAST; case EAST -> WEST;
        }; }
    }
    @FunctionalInterface public interface Faces<T> {
        /** A full vertical collision face, with the normal pointing from the player into the block. */
        boolean full(T context, int x, int z, Wall wall);
    }
    public record Step(Wall wall, double x, double z) {}
    private record Face(int x, int z, Wall wall, double plane, double min, double max) {}
    private static final double EPSILON = Traversal.ADAPTED_COLLISION_EPSILON;
    private ClimbPath() {}
    /** Side is +1 for A/left, -1 for D/right; zero distance validates the current attachment. */
    public static <T> Step resolve(T context, Faces<T> blocks, double x, double z, Wall wall,
            int side, double distance, double radius, double reach) {
        Face face = support(context, blocks, x, z, wall, radius, reach);
        // The owning client can have already crossed a corner before its authoritative wall byte arrives.
        if (face == null) face = support(context, blocks, x, z, wall.tangent(1), radius, reach);
        if (face == null) face = support(context, blocks, x, z, wall.tangent(-1), radius, reach);
        if (face == null) return null;
        wall = face.wall;
        if (side == 0) return new Step(wall, x, z);
        double remaining = distance;
        while (true) {
            Wall tangent = wall.tangent(side);
            double coordinate = wall.x == 0 ? x : z;
            int sign = tangent.x + tangent.z;
            double end = sign > 0 ? face.max : face.min;
            double available = Math.max(0, (end - coordinate) * sign);
            if (available > EPSILON) {
                if (remaining == 0) return new Step(wall, x, z);
                // Close only the validated contact gap, never jump to a disconnected wall.
                double gap = (face.plane - (wall.x == 0 ? z : x)) * (wall.x + wall.z) - radius;
                x += wall.x * gap;
                z += wall.z * gap;
            }
            double travel = Math.min(remaining, available);
            x += tangent.x * travel;
            z += tangent.z * travel;
            remaining -= travel;
            if (available - travel > EPSILON) return new Step(wall, x, z);

            int nextX = face.x + tangent.x, nextZ = face.z + tangent.z;
            Face next;
            if (blocks.full(context, nextX - wall.x, nextZ - wall.z, tangent)) {
                // Concave corner: the protruding column meets us one half-width before the grid edge.
                next = face(context, blocks, nextX - wall.x, nextZ - wall.z, tangent, radius);
            } else {
                next = face(context, blocks, nextX, nextZ, wall, radius);
                if (next == null) {
                    // Convex corner: finish the half-width clearance before turning onto this column's side.
                    next = face(context, blocks, face.x, face.z, tangent.opposite(), radius);
                }
            }
            if (next == null) return null;
            face = next;
            wall = next.wall;
            if (remaining <= EPSILON) return new Step(wall, x, z);
        }
    }
    private static <T> Face support(T context, Faces<T> blocks, double x, double z, Wall wall,
            double radius, double reach) {
        int normal = (int) Math.floor(wall.x == 0 ? z + wall.z * (radius + reach) : x + wall.x * (radius + reach));
        double coordinate = wall.x == 0 ? x : z;
        int along = (int) Math.floor(coordinate);
        Face best = null;
        double bestGap = Double.POSITIVE_INFINITY;
        for (int offset = -1; offset <= 1; offset++) {
            int bx = wall.x == 0 ? along + offset : normal;
            int bz = wall.x == 0 ? normal : along + offset;
            Face candidate = face(context, blocks, bx, bz, wall, radius);
            if (candidate == null || coordinate < candidate.min - EPSILON || coordinate > candidate.max + EPSILON) continue;
            double separation = (candidate.plane - (wall.x == 0 ? z : x)) * (wall.x + wall.z);
            // Radius includes one epsilon of clearance; actual vanilla contact can be that much closer.
            if (separation < radius - 2 * EPSILON || separation > radius + reach + EPSILON) continue;
            int blockAlong = wall.x == 0 ? bx : bz;
            double gap = Math.max(0, Math.max(blockAlong - coordinate, coordinate - blockAlong - 1));
            if (gap < bestGap) { best = candidate; bestGap = gap; }
        }
        return best;
    }
    private static <T> Face face(T context, Faces<T> blocks, int x, int z, Wall wall, double radius) {
        if (!blocks.full(context, x, z, wall) || blocks.full(context, x - wall.x, z - wall.z, wall)) return null;
        int along = wall.x == 0 ? x : z;
        Wall low = wall.x == 0 ? Wall.WEST : Wall.NORTH;
        Wall high = low.opposite();
        double min = along + endpoint(context, blocks, x, z, wall, low, radius);
        double max = along + 1 - endpoint(context, blocks, x, z, wall, high, radius);
        double plane = wall.x == 0 ? z + (wall.z < 0 ? 1 : 0) : x + (wall.x < 0 ? 1 : 0);
        return new Face(x, z, wall, plane, min, max);
    }
    private static <T> double endpoint(T context, Faces<T> blocks, int x, int z, Wall wall, Wall tangent, double radius) {
        if (blocks.full(context, x + tangent.x - wall.x, z + tangent.z - wall.z, tangent)) return radius;
        return blocks.full(context, x + tangent.x, z + tangent.z, wall) ? 0 : -radius;
    }
}

package io.github.brainage04.genshininminecraft.combat;

import io.github.brainage04.genshininminecraft.rules.CameraMath;
import io.github.brainage04.genshininminecraft.rules.ClimbPath;
import io.github.brainage04.genshininminecraft.rules.Traversal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared collision adapter. Only vertical, full collision faces; no invented material blacklist. */
public final class TraversalGeometry {
    private static final Direction[] DIRECTIONS = Direction.values();
    public static Direction wall(int ordinal) { return DIRECTIONS[ordinal]; }
    private static final Direction[] WALLS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private TraversalGeometry() {}
    public static Direction findWall(Player player, float yaw, Input input) {
        int left = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        int forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        double x = CameraMath.movementX(yaw, left, forward), z = CameraMath.movementZ(yaw, left, forward);
        for (Direction wall : WALLS) if (x * wall.getStepX() + z * wall.getStepZ() > Traversal.ADAPTED_WALL_APPROACH_DOT
                && adjacent(player, wall)) return wall;
        return null;
    }
    private static BlockPos wallBlock(Player player, Direction wall, double y) {
        double reach = player.getBbWidth() / 2 + Traversal.ADAPTED_WALL_REACH;
        return BlockPos.containing(player.getX() + wall.getStepX() * reach, y,
                player.getZ() + wall.getStepZ() * reach);
    }
    private static boolean fullFace(Player player, BlockPos pos, Direction wall) {
        return Block.isFaceFull(player.level().getBlockState(pos).getCollisionShape(player.level(), pos), wall.getOpposite());
    }
    public static boolean adjacent(Player player, Direction wall) {
        return fullFace(player, wallBlock(player, wall, player.getY() + Traversal.ADAPTED_WALL_LOWER_SAMPLE), wall)
                || fullFace(player, wallBlock(player, wall, player.getY() + Traversal.ADAPTED_WALL_UPPER_SAMPLE), wall);
    }
    private static final ClimbPath.Faces<Player> CLIMB_FACES = TraversalGeometry::climbFace;
    private static boolean climbFace(Player player, int x, int z, ClimbPath.Wall wall) {
        Direction direction = direction(wall);
        return fullFace(player, BlockPos.containing(x, player.getY() + Traversal.ADAPTED_WALL_LOWER_SAMPLE, z), direction)
                || fullFace(player, BlockPos.containing(x, player.getY() + Traversal.ADAPTED_WALL_UPPER_SAMPLE, z), direction);
    }
    private static ClimbPath.Wall normal(Direction wall) { return switch (wall) {
        case NORTH -> ClimbPath.Wall.NORTH; case SOUTH -> ClimbPath.Wall.SOUTH;
        case WEST -> ClimbPath.Wall.WEST; case EAST -> ClimbPath.Wall.EAST;
        default -> throw new IllegalArgumentException("Climbing needs a vertical wall");
    }; }
    private static Direction direction(ClimbPath.Wall wall) { return switch (wall) {
        case NORTH -> Direction.NORTH; case SOUTH -> Direction.SOUTH;
        case WEST -> Direction.WEST; case EAST -> Direction.EAST;
    }; }
    private static int side(Input input, boolean jumping, int jumpSide) {
        return jumping ? jumpSide : (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
    }
    private static ClimbPath.Step climbStep(Player player, Direction wall, int side, double distance) {
        return ClimbPath.resolve(player, CLIMB_FACES, player.getX(), player.getZ(), normal(wall), side, distance,
                player.getBbWidth() / 2 + Traversal.ADAPTED_COLLISION_EPSILON, Traversal.ADAPTED_WALL_REACH);
    }
    /** Validates the actual position, including a corner crossed ahead of the last synchronized normal. */
    public static Direction attachedWall(Player player, Direction previous, Input input, boolean jumping, int jumpSide) {
        if (previous == null) return null;
        ClimbPath.Step step = climbStep(player, previous, side(input, jumping, jumpSide), 0);
        return step == null ? null : direction(step.wall());
    }
    public static Vec3 mantle(Player player, Direction wall) {
        for (int offset = 0; offset >= -1; offset--) {
            BlockPos block = wallBlock(player, wall, Math.floor(player.getY()) + offset);
            double top = block.getY() + 1;
            if (top < player.getY() || top - player.getY() > Traversal.ADAPTED_MANTLE_REACH || !fullFace(player, block, wall)) continue;
            double x = player.getX(), z = player.getZ();
            if (wall.getAxis() == Direction.Axis.X) x = wall == Direction.EAST
                    ? block.getX() + Traversal.ADAPTED_MANTLE_INSET : block.getX() + 1 - Traversal.ADAPTED_MANTLE_INSET;
            else z = wall == Direction.SOUTH ? block.getZ() + Traversal.ADAPTED_MANTLE_INSET : block.getZ() + 1 - Traversal.ADAPTED_MANTLE_INSET;
            Vec3 target = new Vec3(x, top, z);
            if (player.level().noCollision(player, player.getBoundingBox().move(target.subtract(player.position())))) return target;
        }
        return null;
    }
    public static double clearance(Player player) {
        Vec3 start = player.position();
        var hit = player.level().clip(new ClipContext(start, start.add(0, -Traversal.ADAPTED_GLIDE_MIN_CLEARANCE, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        return hit.getType() == HitResult.Type.MISS ? Traversal.ADAPTED_GLIDE_MIN_CLEARANCE : start.y - hit.getLocation().y;
    }
    public static boolean safeWater(Player player) {
        BlockPos waist = BlockPos.containing(player.getX(), player.getY() + Traversal.ADAPTED_SAFE_WATER_DEPTH, player.getZ());
        var fluid = player.level().getFluidState(waist);
        return fluid.is(FluidTags.WATER) && waist.getY() + fluid.getHeight(player.level(), waist)
                > player.getY() + Traversal.ADAPTED_SAFE_WATER_DEPTH;
    }
    public static Vec3 motion(Player player, Traversal.Mode mode, Direction wall, float yaw, Input input, boolean jumping, int jumpSide) {
        int left = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        int forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        if (mode == Traversal.Mode.GLIDE) {
            // No keys means camera-forward drift; WASD explicitly steers relative to the orbit.
            if (left == 0 && forward == 0) forward = 1;
            return new Vec3(CameraMath.movementX(yaw, left, forward) * Traversal.ADAPTED_GLIDE_FORWARD_BLOCKS_PER_TICK,
                    -Traversal.ADAPTED_GLIDE_DESCENT_BLOCKS_PER_TICK,
                    CameraMath.movementZ(yaw, left, forward) * Traversal.ADAPTED_GLIDE_FORWARD_BLOCKS_PER_TICK);
        }
        if (mode != Traversal.Mode.CLIMB || wall == null) return Vec3.ZERO;
        double speed = jumping ? Traversal.ADAPTED_CLIMB_JUMP_BLOCKS_PER_TICK : Traversal.ADAPTED_CLIMB_BLOCKS_PER_TICK;
        if (jumping) { left = jumpSide; forward = left == 0 ? 1 : 0; }
        double norm = Math.max(1, Math.hypot(left, forward));
        if (left != 0) {
            ClimbPath.Step step = climbStep(player, wall, Integer.signum(left), Math.abs(left) * speed / norm);
            return step == null ? Vec3.ZERO : new Vec3(step.x() - player.getX(), forward * speed / norm, step.z() - player.getZ());
        }
        return new Vec3(wall.getStepX() * Traversal.ADAPTED_WALL_PRESS_BLOCKS_PER_TICK, forward * speed / norm,
                wall.getStepZ() * Traversal.ADAPTED_WALL_PRESS_BLOCKS_PER_TICK);
    }
}

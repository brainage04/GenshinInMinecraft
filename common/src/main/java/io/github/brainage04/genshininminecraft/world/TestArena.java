package io.github.brainage04.genshininminecraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Destructive developer command: only the fixed 50 x 50 x 6 volume is rewritten. */
public final class TestArena {
    public static final int FLOOR_SIZE = 48;
    public static final int CLEAR_HEIGHT = 5;
    public static final int MIN_OFFSET = -FLOOR_SIZE / 2 - 1;
    public static final int MAX_OFFSET = FLOOR_SIZE / 2;

    private TestArena() {
    }

    public static BlockPos build(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos position = player.blockPosition();
        int floorY = Mth.clamp(position.getY() - 1, level.getMinY(), level.getMaxY() - CLEAR_HEIGHT - 1);
        BlockPos center = new BlockPos(position.getX(), floorY, position.getZ());
        ManagedWorldData.get(level.getServer()).setManaged(level.getServer(), true);
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState border = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = MIN_OFFSET; x <= MAX_OFFSET; x++) {
            for (int z = MIN_OFFSET; z <= MAX_OFFSET; z++) {
                boolean edge = x == MIN_OFFSET || x == MAX_OFFSET || z == MIN_OFFSET || z == MAX_OFFSET;
                for (int y = 0; y <= CLEAR_HEIGHT; y++) {
                    cursor.set(center.getX() + x, floorY + y, center.getZ() + z);
                    BlockState state = y == 0 ? floor : y == 1 && edge ? border : air;
                    level.setBlock(cursor, state, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                }
            }
        }
        player.teleportTo(center.getX(), floorY + 1, center.getZ());
        player.resetFallDistance();
        player.setGameMode(GameType.ADVENTURE);
        return center;
    }
}

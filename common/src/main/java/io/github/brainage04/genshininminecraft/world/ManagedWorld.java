package io.github.brainage04.genshininminecraft.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

/** Server-authoritative policy used by both loaders and the shared placement/trampling hooks. */
public final class ManagedWorld {
    private ManagedWorld() {
    }

    public static boolean isManaged(Level level) {
        return level instanceof ServerLevel serverLevel
                && ManagedWorldData.get(serverLevel.getServer()).isManaged();
    }

    public static boolean preventsBlockModification(Level level, Player player) {
        return player != null
                && (player.gameMode() == GameType.SURVIVAL || player.gameMode() == GameType.ADVENTURE)
                && isManaged(level);
    }
}

package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {
    @Inject(method = "turnToDirt", at = @At("HEAD"), cancellable = true)
    private static void genshin$preventTrampling(Entity entity, BlockState state, Level level, BlockPos pos, CallbackInfo callback) {
        if (entity instanceof Player player && ManagedWorld.preventsBlockModification(level, player)) {
            callback.cancel(); // Keep vanilla fall damage; only refuse conversion to dirt.
        }
    }
}

package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void genshin$preventFluidPickupAndPlacement(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> callback) {
        if (ManagedWorld.preventsBlockModification(level, player)) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void genshin$preventFluidPlacement(LivingEntity entity, Level level, BlockPos pos, BlockHitResult hit,
            CallbackInfoReturnable<Boolean> callback) {
        if (entity instanceof Player player && ManagedWorld.preventsBlockModification(level, player)) {
            callback.setReturnValue(false);
        }
    }
}

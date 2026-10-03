package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void genshin$preventPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (ManagedWorld.preventsBlockModification(context.getLevel(), context.getPlayer())) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }
}

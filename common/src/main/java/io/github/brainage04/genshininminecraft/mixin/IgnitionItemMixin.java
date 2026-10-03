package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FlintAndSteelItem.class, FireChargeItem.class})
public abstract class IgnitionItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void genshin$preventFirePlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (ManagedWorld.preventsBlockModification(context.getLevel(), context.getPlayer())) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }
}

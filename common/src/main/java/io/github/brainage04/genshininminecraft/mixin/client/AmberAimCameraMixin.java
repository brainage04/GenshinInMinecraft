package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.CombatInput;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Visual prediction only; the server owns shot charge, movement speed and damage. */
@Mixin(Camera.class)
public abstract class AmberAimCameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void genshin$aimZoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (CombatInput.aiming()) cir.setReturnValue(cir.getReturnValueF() * CombatInput.ADAPTED_AIM_FOV_MULTIPLIER);
    }
}

package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class CameraMovementMixin {
    @Inject(method = "applyInput", at = @At("RETURN"))
    private void genshin$cameraRelative(CallbackInfo ci) {
        ManagedCamera.applyMovement((LocalPlayer) (Object) this);
    }
}

package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class CameraMouseMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void genshin$orbitMouse(LocalPlayer player, double dx, double dy) {
        if (ManagedCamera.controls(player)) ManagedCamera.turn(dx, dy);
        else player.turn(dx, dy);
    }
}

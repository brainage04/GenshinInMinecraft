package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Visual prediction only; the server owns shot charge, movement speed and damage. */
@Mixin(Camera.class)
public abstract class ManagedCameraMixin {
    @Shadow private Entity entity;
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow private boolean detached;
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void move(float forward, float up, float left);
    @Shadow private float getMaxZoom(float distance) { throw new AssertionError(); }

    @Inject(method = "alignWithEntity", at = @At("HEAD"), cancellable = true)
    private void genshin$orbit(float partialTick, CallbackInfo ci) {
        if (!ManagedCamera.controls(entity)) return;
        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double y = Mth.lerp(partialTick, entity.yo, entity.getY()) + Mth.lerp(partialTick, eyeHeightOld, eyeHeight);
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
        setPosition(x, y, z);
        detached = true;
        float yaw = ManagedCamera.yaw();
        float pitch = ManagedCamera.pitch();
        boolean aiming = CombatInput.aiming();
        if (aiming) {
            // Reuse vanilla's eight-ray hull clip for BOTH the lateral shoulder and backward leg.
            setRotation(yaw - 90, 0);
            float offset = getMaxZoom(ManagedCamera.AIM_RIGHT_OFFSET);
            double angle = Math.toRadians(yaw);
            setPosition(x - Math.cos(angle) * offset, y, z - Math.sin(angle) * offset);
        }
        setRotation(yaw, pitch);
        move(-getMaxZoom(aiming ? ManagedCamera.AIM_DISTANCE : ManagedCamera.ORBIT_DISTANCE), 0, 0);
        ci.cancel();
    }
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void genshin$aimZoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (CombatInput.aiming()) cir.setReturnValue(cir.getReturnValueF() * CombatInput.ADAPTED_AIM_FOV_MULTIPLIER);
    }
}

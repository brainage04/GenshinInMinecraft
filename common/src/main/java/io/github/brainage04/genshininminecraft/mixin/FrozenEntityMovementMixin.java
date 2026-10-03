package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Also block external pushes/impulses, rather than relying on an AI speed penalty to freeze. */
@Mixin(Entity.class)
public abstract class FrozenEntityMovementMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void genshin$freezeDisplacement(MoverType type, Vec3 movement, CallbackInfo callback) {
        if ((Object) this instanceof LivingEntity entity && CombatRuntime.isFrozen(entity)) {
            entity.setDeltaMovement(Vec3.ZERO);
            callback.cancel();
        }
    }
}

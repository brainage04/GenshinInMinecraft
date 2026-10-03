package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Frozen gauge owns immobilization; never change or overwrite the entity's saved NoAI flag. */
@Mixin(LivingEntity.class)
public abstract class FrozenLivingEntityMixin {
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void genshin$freezeMovementAndAi(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!CombatRuntime.isFrozen(entity)) return;
        entity.setDeltaMovement(Vec3.ZERO);
        callback.cancel();
    }
}

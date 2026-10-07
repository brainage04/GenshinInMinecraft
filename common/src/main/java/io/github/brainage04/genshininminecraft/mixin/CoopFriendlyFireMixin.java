package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.enemy.BaronBunny;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Direct player-owned melee/projectile/explosion sources cannot hurt allies. Environment remains independent. */
@Mixin(LivingEntity.class)
public abstract class CoopFriendlyFireMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void genshin$excludeAllies(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        Object victim = this;
        if (ManagedWorld.isManaged(level) && (victim instanceof Player || victim instanceof BaronBunny)
                && (source.getEntity() instanceof Player || source.getEntity() instanceof BaronBunny))
            callback.setReturnValue(false);
    }
}

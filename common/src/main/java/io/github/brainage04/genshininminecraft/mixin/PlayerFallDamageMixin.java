package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replace the server-accumulated landing loss, not client-reported health or a damage multiplier. */
@Mixin(Player.class)
public abstract class PlayerFallDamageMixin extends LivingEntity {
    protected PlayerFallDamageMixin(EntityType<? extends LivingEntity> type, Level level) { super(type, level); }
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void genshin$fallHpLoss(double distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player
                && CombatRuntime.get(player.level().getServer()).fallDamage(player, distance, source,
                        calculateFallDamage(distance, multiplier) > 0))
            callback.setReturnValue(false);
    }
}

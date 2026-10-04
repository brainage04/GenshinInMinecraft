package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replace the server-accumulated landing loss, not client-reported health or a damage multiplier. */
@Mixin(Player.class)
public abstract class PlayerFallDamageMixin {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void genshin$fallHpLoss(double distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player
                && CombatRuntime.get(player.level().getServer()).fallDamage(player, distance, source))
            callback.setReturnValue(false);
    }
}

package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Character death must precede vanilla entity death; a surviving member retains this player entity. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMixin {
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void genshin$replaceFallenCharacter(DamageSource source, CallbackInfo callback) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (CombatRuntime.get(player.level().getServer()).handleDeath(player)) callback.cancel();
    }
}

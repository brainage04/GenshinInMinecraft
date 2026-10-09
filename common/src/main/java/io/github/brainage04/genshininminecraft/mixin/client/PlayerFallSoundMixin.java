package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.rules.CombatAudio;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The owner already predicted vanilla fall/block audio; the later HP-loss receipt must not add another oof. */
@Mixin(Player.class)
public abstract class PlayerFallSoundMixin {
    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void genshin$noSecondFallHurt(DamageSource source, CallbackInfoReturnable<SoundEvent> callback) {
        if (!CombatAudio.playerHurt(CombatInput.managed(), source.is(DamageTypeTags.IS_FALL))) callback.setReturnValue(null);
    }
}

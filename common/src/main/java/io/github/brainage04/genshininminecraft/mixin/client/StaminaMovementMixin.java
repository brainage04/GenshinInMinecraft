package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops vanilla's latched/double-tap sprint when the managed key is released or stamina locks it. */
@Mixin(LocalPlayer.class)
public abstract class StaminaMovementMixin {
    @Inject(method = "isSprintingPossible", at = @At("HEAD"), cancellable = true)
    private void genshin$staminaGate(boolean flying, CallbackInfoReturnable<Boolean> cir) {
        if (CombatInput.managed() && (CombatInput.aiming() || CombatInput.state().staminaExhausted()
                || !Minecraft.getInstance().options.keySprint.isDown())) cir.setReturnValue(false);
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void genshin$heldSprint(CallbackInfo ci) {
        if (!CombatInput.managed()) return;
        LocalPlayer player = (LocalPlayer) (Object) this;
        Minecraft client = Minecraft.getInstance();
        boolean allowed = !CombatInput.aiming() && client.gui.screen() == null && player.isAlive() && !player.isSpectator()
                && !player.isPassenger() && !player.getAbilities().flying && !player.isInWater()
                && !player.isFallFlying() && !player.input.keyPresses.shift()
                && player.input.getMoveVector().lengthSquared() > .001F
                && client.options.keySprint.isDown() && !CombatInput.state().staminaExhausted()
                && (CombatInput.state().staminaDraining() || CombatInput.state().stamina() >= Stamina.SPRINT_START_COST);
        // No local resource spend or dash prediction: the server sends all dash velocity.
        player.setSprinting(allowed);
    }
}

package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.CombatInput;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class CombatInputMixin {
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void genshin$intentBeforeVanilla(CallbackInfo ci) { CombatInput.beforeKeybinds((Minecraft) (Object) this); }
    @Inject(method = "tick", at = @At("HEAD"))
    private void genshin$releaseOnScreen(CallbackInfo ci) { CombatInput.tick((Minecraft) (Object) this); }
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void genshin$noVanillaAttack(CallbackInfoReturnable<Boolean> cir) {
        if (CombatInput.managed()) cir.setReturnValue(false);
    }
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void genshin$noVanillaBreaking(boolean attacking, CallbackInfo ci) {
        if (CombatInput.managed()) ci.cancel();
    }
    @Inject(method = "clearClientLevel", at = @At("HEAD"))
    private void genshin$forgetSession(CallbackInfo ci) { CombatInput.reset(); }
}

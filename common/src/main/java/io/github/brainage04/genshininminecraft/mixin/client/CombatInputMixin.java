package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.character.PlayerVisuals;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.GameLoadCookie;
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
    @Inject(method = "reloadResourcePacks(ZLnet/minecraft/client/GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private void genshin$reloadCharacters(boolean force, GameLoadCookie cookie, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        cir.getReturnValue().thenRun(() -> ((Minecraft) (Object) this).execute(PlayerVisuals::invalidateViews));
    }
}

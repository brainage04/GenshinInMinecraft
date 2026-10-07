package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.character.PlayerVisuals;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class CharacterGameRendererMixin {
    @Inject(method = "extract(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("RETURN"))
    private void genshin$armsSnapshot(DeltaTracker delta, boolean renderLevel, CallbackInfo ci) {
        PlayerVisuals.extractArms(delta.getGameTimeDeltaPartialTick(false));
        io.github.brainage04.genshininminecraft.client.enemy.BunnyVisuals.extract(delta.getGameTimeDeltaPartialTick(false));
    }
}

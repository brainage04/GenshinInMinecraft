package io.github.brainage04.genshininminecraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brainage04.genshininminecraft.client.enemy.BunnyVisuals;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class BunnyLevelRendererMixin {
    @Inject(method = "submitEntities", at = @At("RETURN"))
    private void genshin$bunnyContinuations(PoseStack pose, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
        BunnyVisuals.submit(pose, state, collector);
        io.github.brainage04.genshininminecraft.client.ProjectileVisuals.submit(pose, state, collector);
    }
}

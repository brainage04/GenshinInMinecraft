package io.github.brainage04.genshininminecraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brainage04.genshininminecraft.client.character.CharacterAvatarState;
import io.github.brainage04.genshininminecraft.client.character.PlayerVisuals;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class CharacterLivingRendererMixin extends EntityRenderer<LivingEntity, LivingEntityRenderState> {
    protected CharacterLivingRendererMixin(EntityRendererProvider.Context context) { super(context); }
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void genshin$body(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!(state instanceof CharacterAvatarState holder) || holder.genshin$characterState() == null) return;
        PlayerVisuals.submitBody(holder.genshin$characterState(), pose, collector, camera);
        // Invoke the actual base once: a virtual call to this.submit would recurse.
        super.submit(state, pose, collector, camera);
        ci.cancel();
    }
    @Inject(method = "getBoundingBoxForCulling(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"), cancellable = true)
    private void genshin$wingBounds(LivingEntity entity, CallbackInfoReturnable<AABB> cir) {
        if (entity instanceof AbstractClientPlayer player && PlayerVisuals.usesCharacter(player))
            cir.setReturnValue(cir.getReturnValue().inflate(1.1 * player.getScale(), .45 * player.getScale(), 1.1 * player.getScale()));
    }
}

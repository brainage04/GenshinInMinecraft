package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.character.CharacterAvatarState;
import io.github.brainage04.genshininminecraft.client.character.PlayerVisuals;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class CharacterAvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("RETURN"))
    private void genshin$extract(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        var holder = (CharacterAvatarState) state;
        holder.genshin$characterState(null);
        if (entity instanceof AbstractClientPlayer player) holder.genshin$characterState(PlayerVisuals.extract(player, state, partialTick, false));
    }
}

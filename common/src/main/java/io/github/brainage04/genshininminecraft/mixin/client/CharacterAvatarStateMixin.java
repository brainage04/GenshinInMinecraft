package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.character.CharacterAvatarState;
import io.github.brainage04.genshininminecraft.client.character.CharacterGeoRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class CharacterAvatarStateMixin implements CharacterAvatarState {
    @Unique private CharacterGeoRenderState genshin$character;
    @Override public CharacterGeoRenderState genshin$characterState() { return genshin$character; }
    @Override public void genshin$characterState(CharacterGeoRenderState state) { genshin$character = state; }
}

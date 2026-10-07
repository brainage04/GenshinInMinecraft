package io.github.brainage04.genshininminecraft.client.character;

/** Client-only duck on AvatarRenderState; null means the unchanged vanilla path. */
public interface CharacterAvatarState {
    CharacterGeoRenderState genshin$characterState();
    void genshin$characterState(CharacterGeoRenderState state);
}

package io.github.brainage04.genshininminecraft.client.character;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public final class CharacterGeoModel extends GeoModel<CharacterAnimatable> {
    private static final Identifier[] MODELS = new Identifier[4];
    private static final Identifier[] TEXTURES = new Identifier[4];
    static {
        String[] names = {"aether", "amber", "kaeya", "lisa"};
        for (int i = 0; i < names.length; i++) {
            MODELS[i] = Identifier.fromNamespaceAndPath("genshininminecraft", "character/" + names[i]);
            TEXTURES[i] = Identifier.fromNamespaceAndPath("genshininminecraft", "textures/entity/character/" + names[i] + ".png");
        }
    }
    @Override public Identifier getModelResource(GeoRenderState state) { return MODELS[((CharacterGeoRenderState) state).input().slot()]; }
    @Override public Identifier getTextureResource(GeoRenderState state) { return TEXTURES[((CharacterGeoRenderState) state).input().slot()]; }
    @Override public Identifier getAnimationResource(CharacterAnimatable animatable) { return MODELS[animatable.slot()]; }
}

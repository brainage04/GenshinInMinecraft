package io.github.brainage04.genshininminecraft.client.enemy;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

/** One original atlas/model/animation set per enemy, baked and cached by GeckoLib. */
public final class EnemyGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final Identifier model, texture, animation;
    public EnemyGeoModel(String name) {
        model = id("enemy/" + name);
        texture = id("textures/entity/enemy/" + name + ".png");
        animation = model;
    }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("genshininminecraft", path); }
    @Override public Identifier getModelResource(GeoRenderState state) { return model; }
    @Override public Identifier getTextureResource(GeoRenderState state) { return texture; }
    @Override public Identifier getAnimationResource(T animatable) { return animation; }
}

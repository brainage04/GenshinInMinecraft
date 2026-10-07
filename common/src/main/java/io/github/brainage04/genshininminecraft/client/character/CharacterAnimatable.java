package io.github.brainage04.genshininminecraft.client.character;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

/** One owned cache per player/view/accepted character generation, never one singleton per skin. */
public final class CharacterAnimatable implements GeoAnimatable {
    private final int slot;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, false);
    public CharacterAnimatable(int slot) { this.slot = slot; }
    public int slot() { return slot; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new SyncedLocomotionController());
        controllers.add(new SyncedCombatController());
        controllers.add(new SyncedCombatController(true));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

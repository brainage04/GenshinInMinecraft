package io.github.brainage04.genshininminecraft.client.character;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.brainage04.genshininminecraft.rules.Locomotion;

/** Pinned to GeckoLib 5.5.5/5.5.6: seek AFTER new timeline initialization, including first extraction. */
final class SyncedLocomotionController extends AnimationController<CharacterAnimatable> {
    private static final RawAnimation[] CLIPS;
    static {
        var phases = Locomotion.Phase.values();
        CLIPS = new RawAnimation[phases.length];
        for (var phase : phases) CLIPS[phase.ordinal()] = phase.loop()
                ? RawAnimation.begin().thenLoop(phase.clip()) : RawAnimation.begin().thenPlayAndHold(phase.clip());
    }
    private int occurrence = -1;
    SyncedLocomotionController() {
        super("locomotion", 0, test -> test.setAndContinue(CLIPS[test.getData(CharacterGeoRenderState.INPUT).phase().ordinal()]));
    }
    @Override protected boolean checkControllerState(CharacterAnimatable animatable, GeoRenderState state,
            AnimatableManager<CharacterAnimatable> manager, GeoModel<CharacterAnimatable> model) {
        var input = state.getGeckolibData(CharacterGeoRenderState.INPUT);
        if (occurrence != input.occurrence()) { reset(); occurrence = input.occurrence(); }
        super.checkControllerState(animatable, state, manager, model);
        if (timeline != null) {
            timelineTime = Math.min(input.elapsedSeconds(), input.phase().frames() / 60.0 - .000001);
            transitionFromPoint = null;
            animationPoint = timeline.createAnimationPoint(timelineTime, null, easingOverride);
        }
        return isAnimatingBones();
    }
}

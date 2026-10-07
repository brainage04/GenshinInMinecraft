package io.github.brainage04.genshininminecraft.client.character;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.brainage04.genshininminecraft.rules.CombatAnimations;
import io.github.brainage04.genshininminecraft.rules.CombatVisual.Action;

/** Same pinned post-initialization absolute seek as locomotion; no markers replay gameplay or sounds. */
final class SyncedCombatController extends AnimationController<CharacterAnimatable> {
    private static final RawAnimation[][] CLIPS = new RawAnimation[4][Action.values().length];
    static {
        for (int slot = 0; slot < 4; slot++) for (var action : Action.values()) {
            var timing = CombatAnimations.timing(slot, action);
            if (timing != null) CLIPS[slot][action.ordinal()] = timing.loop()
                    ? RawAnimation.begin().thenLoop(action.clip()) : RawAnimation.begin().thenPlayAndHold(action.clip());
        }
    }
    private final boolean hurt;
    private int occurrence = -1;
    private Action action = Action.NONE;
    SyncedCombatController() { this(false); }
    SyncedCombatController(boolean hurt) {
        super(hurt ? "hurt" : "combat", 0, test -> {
            var input = test.getData(CharacterGeoRenderState.INPUT);
            var selected = hurt ? input.hurtSeconds() < .3 ? Action.HURT : Action.NONE : input.action();
            return selected == Action.NONE ? PlayState.STOP : test.setAndContinue(CLIPS[input.slot()][selected.ordinal()]);
        });
        this.hurt = hurt;
        if (hurt) additiveAnimations();
    }
    @Override protected boolean checkControllerState(CharacterAnimatable animatable, GeoRenderState state,
            AnimatableManager<CharacterAnimatable> manager, GeoModel<CharacterAnimatable> model) {
        var input = state.getGeckolibData(CharacterGeoRenderState.INPUT);
        int nextOccurrence = hurt ? input.hurtOccurrence() : input.actionOccurrence();
        var nextAction = hurt ? input.hurtSeconds() < .3 ? Action.HURT : Action.NONE : input.action();
        if (occurrence != nextOccurrence || action != nextAction) {
            reset(); occurrence = nextOccurrence; action = nextAction;
        }
        super.checkControllerState(animatable, state, manager, model);
        if (action == Action.NONE) return false;
        if (timeline != null) {
            timelineTime = hurt ? Math.min(input.hurtSeconds(), .3 - .000001) : input.actionSeconds();
            transitionFromPoint = null;
            animationPoint = timeline.createAnimationPoint(timelineTime, null, easingOverride);
        }
        return isAnimatingBones();
    }
}

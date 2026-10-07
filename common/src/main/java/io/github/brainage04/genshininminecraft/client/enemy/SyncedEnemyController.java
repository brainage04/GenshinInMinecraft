package io.github.brainage04.genshininminecraft.client.enemy;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.brainage04.genshininminecraft.rules.EnemyAnimations.Phase;

/** GeckoLib 5.5.5/5.5.6 post-initialization seek, shared by entities and cosmetic Bunny continuations. */
public final class SyncedEnemyController<T extends GeoAnimatable> extends AnimationController<T> {
    private static final RawAnimation[] CLIPS = new RawAnimation[Phase.values().length];
    static {
        for (var phase : Phase.values()) CLIPS[phase.ordinal()] = phase.loop()
                ? RawAnimation.begin().thenLoop(phase.clip()) : RawAnimation.begin().thenPlayAndHold(phase.clip());
    }
    private final boolean hurt;
    private int occurrence = -1;
    private Phase phase;
    public SyncedEnemyController(boolean hurt) {
        super(hurt ? "hurt" : "phase", 0, test -> {
            var input = test.getData(EnemyRenderInput.TICKET);
            if (hurt && (input.phase() == Phase.DEATH || input.hurtSeconds() >= .3)) return PlayState.STOP;
            return test.setAndContinue(CLIPS[(hurt ? Phase.HURT : input.phase()).ordinal()]);
        });
        this.hurt = hurt;
        if (hurt) additiveAnimations();
    }
    @Override protected boolean checkControllerState(T animatable, GeoRenderState state,
            AnimatableManager<T> manager, GeoModel<T> model) {
        var input = state.getGeckolibData(EnemyRenderInput.TICKET);
        var next = hurt ? Phase.HURT : input.phase();
        int serial = hurt ? input.hurtOccurrence() : input.occurrence();
        if (phase != next || occurrence != serial) { reset(); phase = next; occurrence = serial; }
        super.checkControllerState(animatable, state, manager, model);
        if (hurt && (input.phase() == Phase.DEATH || input.hurtSeconds() >= .3)) return false;
        if (timeline != null) {
            timelineTime = next.seconds(hurt ? input.hurtSeconds() : input.seconds());
            transitionFromPoint = null;
            animationPoint = timeline.createAnimationPoint(timelineTime, null, easingOverride);
        }
        return isAnimatingBones();
    }
}

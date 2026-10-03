package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import java.util.function.Consumer;

/** Only shared normal-input lifecycle, not a template for character skills or bursts. */
abstract class NormalAttackKit implements CharacterKit {
    public static final long ADAPTED_COMBO_RESET_FRAMES = 90;
    protected final EventTimeline timeline;
    protected final Stamina stamina;
    protected final CharacterState state;
    protected final Consumer<Hit> hits;
    private final Weapon weapon;
    private long generation;
    private long switchReady;
    private boolean held;

    NormalAttackKit(CharacterBaseStats.Character character, Weapon weapon, double weaponAtk, double burstCost,
            EventTimeline timeline, Stamina stamina, Consumer<Hit> hits) {
        this.state = new CharacterState(character, weaponAtk, burstCost);
        this.weapon = weapon;
        this.timeline = timeline;
        this.stamina = stamina;
        this.hits = hits;
        stamina.advanceTo(timeline.frame());
    }
    @Override public final CharacterState state() { return state; }
    @Override public final Stamina stamina() { return stamina; }
    @Override public final Weapon weapon() { return weapon; }
    @Override public final long frame() { return timeline.frame(); }
    @Override public final void advanceTo(long frame) { timeline.advanceTo(frame); stamina.advanceTo(frame); }
    @Override public boolean intent(Intent intent, long frame) {
        advanceTo(frame);
        if (intent == Intent.ATTACK_RELEASE) { held = false; return true; }
        if (intent != Intent.ATTACK_PRESS || !state.alive() || held || frame < state.actionReady) return false;
        if (frame >= state.comboReset) state.combo = 0;
        held = true;
        ++generation;
        switchReady = frame;
        normal(frame, state.combo);
        return true;
    }
    protected abstract void normal(long frame, int index);
    protected final void combo(long frame, int index, int count, int recovery) {
        state.combo = (index + 1) % count;
        state.actionReady = frame + recovery;
        state.comboReset = state.actionReady + ADAPTED_COMBO_RESET_FRAMES;
    }
    protected final void hit(long frame, Kind kind, double multiplier, Element element, double gauge, String tag, long cast) {
        hit(frame, kind, multiplier, element, gauge, tag, 0, cast);
    }
    protected final void hit(long frame, Kind kind, double multiplier, Element element, double gauge, String tag,
            int particles, long cast) {
        long expected = generation;
        timeline.schedule(frame, at -> {
            if (state.alive() && expected == generation)
                hits.accept(new Hit(at, kind, multiplier, element, gauge, tag, false, false, particles, cast));
        });
    }
    protected final void startTalent(long frame, int recovery, int swap) {
        leaveField(frame); // Cancel held normals/charges, not a character's persistent burst.
        state.actionReady = frame + recovery;
        switchReady = frame + swap;
    }
    protected final void charge(long at, double cost, int recovery, int firstSwapFrame, java.util.function.LongConsumer action) {
        long expected = generation;
        timeline.schedule(at, frame -> {
            if (!held || expected != generation || !state.alive() || !stamina.consume(cost, frame)) return;
            state.combo = 0;
            state.actionReady = frame + recovery;
            switchReady = frame + firstSwapFrame;
            action.accept(frame);
        });
    }
    @Override public final boolean canSwitch(long frame) { return frame >= switchReady; }
    @Override public void leaveField(long frame) {
        held = false;
        ++generation;
        state.combo = 0;
        state.comboReset = 0;
        state.actionReady = frame;
        switchReady = frame;
    }
}

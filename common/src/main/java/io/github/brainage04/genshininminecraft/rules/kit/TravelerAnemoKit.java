package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.CombatVisual;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Energy;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import io.github.brainage04.genshininminecraft.rules.Stats;
import java.util.function.Consumer;

/** Aether, C0, ascension 0, talent level 1. All timestamps are reference frames. */
public final class TravelerAnemoKit implements CharacterKit {
    public static final int STARTER_LEVEL = CharacterState.STARTER_LEVEL;
    public static final double BURST_COST = 60;
    public static final long COMBO_RESET_FRAMES = 90;
    public static final long FULL_HOLD_RELEASE_FRAME = 108;
    public static final long STRONG_HOLD_THRESHOLD = 30;
    public static final long BURST_DURATION_FRAMES = 360;
    public static final long CHARGED_SWITCH_FRAME = 44;
    public static final long TAP_SWITCH_FRAME = 66;
    public static final long BURST_SWITCH_FRAME = 100;
    public static final long ADAPTED_HOLD_RELEASE_SWITCH_FRAMES = 39;
    public static final int CHARGED_FIRST_HIT_FRAME = 10;
    public static final int CHARGED_SECOND_HIT_FRAME = 21;
    public static final int CHARGED_RECOVERY_FRAMES = 55;
    public static final int TAP_HIT_FRAME = 32;
    public static final int TAP_RECOVERY_FRAMES = 74;
    public static final int HOLD_HIT_OFFSET = 5;
    public static final int HOLD_RECOVERY_OFFSET = 48;
    public static final int BURST_FIRST_HIT_FRAME = 96;
    public static final int BURST_RECOVERY_FRAMES = 111;
    private static final double[] NORMAL_MULTIPLIERS = {.445, .434, .530, .583, .708};
    private static final int[] NORMAL_HIT_FRAMES = {13, 13, 16, 30, 25};
    private static final int[] NORMAL_RECOVERY_FRAMES = {23, 32, 40, 49, 81};
    private static final int[] CHARGE_TRANSITION_FRAMES = {28, 28, 36, 45};
    private static final int[] CUTTING_FRAMES = {21, 30, 51, 60, 81, 90};
    private final CharacterState state = new CharacterState(CharacterBaseStats.Character.TRAVELER_ANEMO,
            BURST_COST);
    private final EventTimeline timeline;
    private final Consumer<Hit> hits;
    private final Stamina stamina;
    private long switchReady;
    private boolean attackHeld;
    private long attackGeneration;
    private boolean skillHeld;
    private long skillStart;
    private long skillGeneration;
    private long actionGeneration;
    private long burstGeneration;
    private Stats burstStats;

    private final CombatVisual visual = new CombatVisual();
    public static int normalStrike(int index) { return NORMAL_HIT_FRAMES[index]; }
    public static int normalRecovery(int index) { return NORMAL_RECOVERY_FRAMES[index]; }
    @Override public CombatVisual visual() { return visual; }
    public TravelerAnemoKit(Consumer<Hit> hits) { this(new EventTimeline(), new Stamina(), hits); }
    public TravelerAnemoKit(EventTimeline timeline, Consumer<Hit> hits) { this(timeline, new Stamina(), hits); }
    public TravelerAnemoKit(EventTimeline timeline, Stamina stamina, Consumer<Hit> hits) {
        this.timeline = java.util.Objects.requireNonNull(timeline);
        this.hits = java.util.Objects.requireNonNull(hits);
        this.stamina = java.util.Objects.requireNonNull(stamina);
        stamina.advanceTo(timeline.frame());
    }
    @Override public CharacterState state() { return state; }
    @Override public Stamina stamina() { return stamina; }
    @Override public Weapon weapon() { return Weapon.SWORD; }
    @Override public long frame() { return timeline.frame(); }
    public boolean skillHeld() { return skillHeld; }
    public Stats burstStats() { return burstStats; }
    public void grantParticles(int count) {
        grantEnergy(count * Energy.received(Energy.Item.PARTICLE, Element.ANEMO, Element.ANEMO, true, 1, 1));
    }
    @Override public void advanceTo(long frame) { timeline.advanceTo(frame); stamina.advanceTo(frame); }
    @Override public boolean canSwitch(long frame) { return !skillHeld && frame >= switchReady; }
    @Override public long switchRemaining(long frame) { return Math.max(0, switchReady - frame); }
    @Override public boolean actionBlocked() { return skillHeld || CharacterKit.super.actionBlocked(); }
    @Override public void leaveField(long frame) {
        visual.clear(frame);
        attackHeld = false;
        skillHeld = false;
        ++attackGeneration;
        ++skillGeneration;
        ++actionGeneration;
        state.combo = 0;
        state.comboReset = 0;
        state.actionReady = frame;
        switchReady = frame;
    }
    @Override public void cancelCasts(long frame) {
        leaveField(frame);
        ++burstGeneration;
        burstStats = null;
    }
    @Override public boolean intent(Intent intent, long frame) {
        advanceTo(frame);
        if (intent == Intent.ATTACK_RELEASE) { attackHeld = false; return true; }
        if (intent == Intent.SKILL_RELEASE) {
            if (!skillHeld) return false;
            releaseSkill(frame);
            return true;
        }
        if (!state.alive()) return false;
        return switch (intent) {
            case ATTACK_PRESS -> attack(frame);
            case SKILL_PRESS -> skill(frame);
            case BURST_PRESS -> burst(frame);
            default -> false;
        };
    }
    private boolean attack(long frame) {
        if (attackHeld || skillHeld || frame < state.actionReady) return false;
        if (frame >= state.comboReset) state.combo = 0;
        int hit = state.combo;
        visual.begin(io.github.brainage04.genshininminecraft.rules.CombatVisual.Action.normal(hit), frame,
                NORMAL_HIT_FRAMES[hit], NORMAL_RECOVERY_FRAMES[hit]);
        state.combo = (state.combo + 1) % 5;
        state.comboReset = frame + NORMAL_RECOVERY_FRAMES[hit] + COMBO_RESET_FRAMES;
        state.actionReady = frame + NORMAL_RECOVERY_FRAMES[hit];
        switchReady = frame; // Sourced normal attacks can be swap-cancelled at any time.
        attackHeld = true;
        long press = ++attackGeneration;
        long action = ++actionGeneration;
        emit(frame + NORMAL_HIT_FRAMES[hit], Kind.NORMAL, NORMAL_MULTIPLIERS[hit], Element.PHYSICAL,
                0, null, false, false, 0, frame, action);
        if (hit < 4) timeline.schedule(frame + CHARGE_TRANSITION_FRAMES[hit], at -> {
            if (!attackHeld || press != attackGeneration || action != actionGeneration || !state.alive()) return;
            if (!stamina.chargedAttack(at)) return;
            state.combo = 0;
            state.actionReady = at + CHARGED_RECOVERY_FRAMES;
            switchReady = at + CHARGED_SWITCH_FRAME;
            visual.begin(io.github.brainage04.genshininminecraft.rules.CombatVisual.Action.CHARGED, at,
                    CHARGED_FIRST_HIT_FRAME, CHARGED_RECOVERY_FRAMES);
            emit(at + CHARGED_FIRST_HIT_FRAME, Kind.CHARGED, .559, Element.PHYSICAL, 0, null, false, false, 0, frame, action);
            emit(at + CHARGED_SECOND_HIT_FRAME, Kind.CHARGED, .607, Element.PHYSICAL, 0, null, false, false, 0, frame, action);
        });
        return true;
    }
    private boolean skill(long frame) {
        if (skillHeld || frame < state.skillReady || frame < state.actionReady) return false;
        attackHeld = false;
        ++actionGeneration;
        state.combo = 0;
        skillHeld = true;
        skillStart = frame;
        visual.hold(io.github.brainage04.genshininminecraft.rules.CombatVisual.Action.SKILL_START, frame);
        timeline.schedule(frame + CUTTING_FRAMES[0], at -> {
            if (skillHeld && skillStart == frame)
                visual.hold(io.github.brainage04.genshininminecraft.rules.CombatVisual.Action.SKILL_HOLD, at);
        });
        long generation = ++skillGeneration;
        for (int i = 0; i < CUTTING_FRAMES.length; i++) {
            int tick = i;
            timeline.schedule(frame + CUTTING_FRAMES[i], at -> {
                if (!skillHeld || generation != skillGeneration || !state.alive()) return;
                hits.accept(new Hit(at, Kind.CUTTING, tick < 2 ? .12 : .168, Element.ANEMO, 1,
                        "Elemental Skill Anemo", true, tick != 0, 0, frame));
            });
        }
        timeline.schedule(frame + FULL_HOLD_RELEASE_FRAME, at -> {
            if (skillHeld && generation == skillGeneration) releaseSkill(at);
        });
        return true;
    }
    private void releaseSkill(long frame) {
        skillHeld = false;
        long duration = frame - skillStart;
        boolean strong = duration >= STRONG_HOLD_THRESHOLD;
        boolean tap = duration < CUTTING_FRAMES[0];
        long storm = tap ? Math.max(skillStart + TAP_HIT_FRAME, frame) : frame + HOLD_HIT_OFFSET;
        long cooldownStart = tap ? skillStart + 27 : frame;
        state.skillCooldown(cooldownStart, strong ? 480 : 300);
        state.actionReady = tap ? skillStart + TAP_RECOVERY_FRAMES : frame + HOLD_RECOVERY_OFFSET;
        switchReady = tap ? skillStart + TAP_SWITCH_FRAME : frame + ADAPTED_HOLD_RELEASE_SWITCH_FRAMES;
        visual.begin(tap ? io.github.brainage04.genshininminecraft.rules.CombatVisual.Action.SKILL_TAP
                : io.github.brainage04.genshininminecraft.rules.CombatVisual.Action.SKILL_RELEASE,
                tap ? skillStart : frame, tap ? (int) (storm - skillStart) : HOLD_HIT_OFFSET,
                tap ? TAP_RECOVERY_FRAMES : HOLD_RECOVERY_OFFSET);
        emit(storm, Kind.STORM, strong ? 1.92 : 1.76, Element.ANEMO, 1,
                null, true, true, strong ? 3 : 2, skillStart, actionGeneration);
    }
    private boolean burst(long frame) {
        if (skillHeld || frame < state.burstReady || state.energy < BURST_COST || frame < state.actionReady) return false;
        // traveler-anemo.md: both Anemo and later absorbed talent damage snapshot for this use.
        burstStats = state.stats(frame);
        state.energy -= BURST_COST;
        state.burstCooldown(frame, 900);
        state.actionReady = frame + BURST_RECOVERY_FRAMES;
        visual.begin(io.github.brainage04.genshininminecraft.rules.CombatVisual.Action.BURST, frame,
                BURST_FIRST_HIT_FRAME, BURST_RECOVERY_FRAMES);
        switchReady = frame + BURST_SWITCH_FRAME;
        attackHeld = false;
        state.combo = 0;
        ++actionGeneration;
        // Burst persists independently of later actions or leaving the field.
        long generation = ++burstGeneration;
        for (int tick = 0; tick < 9; tick++) timeline.schedule(frame + BURST_FIRST_HIT_FRAME + tick * 30, at -> {
            if (state.alive() && generation == burstGeneration) hits.accept(new Hit(at, Kind.TORNADO, .808, Element.ANEMO, 1,
                    "Elemental Burst", true, true, 0, frame));
        });
        return true;
    }
    private void emit(long frame, Kind kind, double multiplier, Element element, double gauge,
            String tag, boolean absorb, boolean absorbedHit, int particles, long cast, long generation) {
        timeline.schedule(frame, at -> {
            if (state.alive() && generation == actionGeneration) hits.accept(new Hit(at, kind, multiplier,
                    element, gauge, tag, absorb, absorbedHit, particles, cast));
        });
    }
}

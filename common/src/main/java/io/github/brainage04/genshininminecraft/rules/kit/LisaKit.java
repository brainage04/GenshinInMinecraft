package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import java.util.function.Consumer;

/** Lisa, C0/ascension 0/talent 1. Charged attacks do not grant the locked A1 passive. */
public final class LisaKit extends NormalAttackKit {
    public static final double BURST_COST = 80;
    public static final double CHARGED_STAMINA_COST = 50;
    public static final int ADAPTED_CHARGED_HIT_FRAMES = 58;
    public static final int ADAPTED_CHARGED_RECOVERY_FRAMES = 77;
    public static final int ADAPTED_CHARGED_SWITCH_FRAMES = 76;
    public static final String NORMAL_ICD_TAG = "Lisa Electro DMG";
    public static final String CHARGED_ICD_TAG = null; // Wiki/current guide no ICD; TCL alternative is 30 frames.
    public static final double TAP_MULTIPLIER = .8;
    public static final int TAP_LAUNCH_FRAME = 17;
    public static final int TAP_FIRST_IMPACT_FRAME = 22;
    public static final int TAP_COOLDOWN_FRAMES = 60;
    public static final int TAP_RECOVERY_FRAME = 38;
    public static final int TAP_SWITCH_FRAME = 20;
    public static final int TAP_LIFETIME_FRAMES = 180;
    public static final int HOLD_CHARGE_FRAMES = 114; // Approximately 1.9 seconds, not an exact measured boundary.
    public static final int CRYO_HOLD_CHARGE_FRAMES = 132;
    public static final int HOLD_MAX_FRAMES = 240;
    public static final int HOLD_COOLDOWN_FRAMES = 960;
    public static final int HOLD_HIT_OFFSET = 3; // Original first trial: cooldown/release113, hit116.
    public static final int HOLD_RECOVERY_OFFSET = 27;
    public static final int HOLD_SWITCH_OFFSET = 4;
    public static final int HOLD_PARTICLES = 5;
    public static final double ROSE_PLACEMENT_MULTIPLIER = .1;
    public static final double ROSE_DISCHARGE_MULTIPLIER = .3656;
    public static final int ROSE_PLACEMENT_FRAME = 56;
    public static final int ADAPTED_ROSE_FORMATION_FRAME = 59; // Last arc119+28*30 is formation+900.
    public static final int ROSE_FIRST_DISCHARGE_FRAME = 119;
    public static final int ROSE_DISCHARGE_INTERVAL_FRAMES = 30;
    public static final int ROSE_DISCHARGE_COUNT = 29;
    public static final int ROSE_DURATION_FRAMES = 900;
    public static final int BURST_COOLDOWN_START_FRAME = 53;
    public static final int BURST_COOLDOWN_FRAMES = 1200;
    public static final int BURST_RECOVERY_FRAME = 85;
    public static final int BURST_SWITCH_FRAME = 56;
    private static final double[] HOLD_MULTIPLIERS = {3.2, 3.68, 4.24, 4.872};
    private static final double[] MULTIPLIERS = {.396, .3592, .428, .5496};
    private static final int[] HIT_FRAMES = {26, 17, 17, 31};
    private static final int[] RECOVERY_FRAMES = {30, 20, 34, 57};
    private static final int[] CHARGE_FRAMES = {31, 24, 40};
    private long skillHeld = -1;
    private long roseCast = -1;
    private int holdThreshold = HOLD_CHARGE_FRAMES;

    public LisaKit(Consumer<Hit> hits) { this(new EventTimeline(), new Stamina(), hits); }
    public LisaKit(EventTimeline timeline, Stamina stamina, Consumer<Hit> hits) {
        super(CharacterBaseStats.Character.LISA, Weapon.CATALYST, BURST_COST, timeline, stamina, hits);
    }
    public static double holdMultiplier(int stacks) { return HOLD_MULTIPLIERS[stacks]; }
    public void setCryoSlowed(boolean slowed) { holdThreshold = slowed ? CRYO_HOLD_CHARGE_FRAMES : HOLD_CHARGE_FRAMES; }
    @Override public boolean intent(Intent intent, long frame) {
        if (intent != Intent.SKILL_PRESS && intent != Intent.SKILL_RELEASE && intent != Intent.BURST_PRESS)
            return super.intent(intent, frame);
        advanceTo(frame);
        if (intent == Intent.SKILL_RELEASE) {
            if (skillHeld < 0 || !state.alive()) return false;
            releaseSkill(frame);
            return true;
        }
        if (!state.alive() || skillHeld >= 0 || frame < state.actionReady) return false;
        if (intent == Intent.SKILL_PRESS) {
            if (frame < state.skillReady) return false;
            startTalent(frame, HOLD_MAX_FRAMES + HOLD_RECOVERY_OFFSET, HOLD_MAX_FRAMES + HOLD_SWITCH_OFFSET);
            skillHeld = frame;
            timeline.schedule(frame + HOLD_MAX_FRAMES, at -> {
                if (skillHeld == frame && state.alive()) releaseSkill(at);
            });
        } else {
            if (frame < state.burstReady || state.energy < BURST_COST) return false;
            startTalent(frame, BURST_RECOVERY_FRAME, BURST_SWITCH_FRAME);
            state.energy -= BURST_COST; // Reserve at acceptance, not original drain63.
            state.burstReady = frame + BURST_COOLDOWN_START_FRAME + BURST_COOLDOWN_FRAMES;
            roseCast = frame;
            roseEvent(frame + ROSE_PLACEMENT_FRAME, Kind.ROSE_PLACE, ROSE_PLACEMENT_MULTIPLIER, 0, frame);
            for (int index = 0; index < ROSE_DISCHARGE_COUNT; index++)
                roseEvent(frame + ROSE_FIRST_DISCHARGE_FRAME + index * ROSE_DISCHARGE_INTERVAL_FRAMES,
                        Kind.ROSE_DISCHARGE, ROSE_DISCHARGE_MULTIPLIER, 1, frame);
        }
        return true;
    }
    private void releaseSkill(long frame) {
        long cast = skillHeld;
        boolean hold = frame - cast >= holdThreshold;
        int recovery = hold ? HOLD_RECOVERY_OFFSET : (int) Math.max(0, cast + TAP_RECOVERY_FRAME - frame);
        int swap = hold ? HOLD_SWITCH_OFFSET : (int) Math.max(0, cast + TAP_SWITCH_FRAME - frame);
        startTalent(frame, recovery, swap);
        if (hold) {
            state.skillReady = frame + HOLD_COOLDOWN_FRAMES;
            hit(frame + HOLD_HIT_OFFSET, Kind.VIOLET_HOLD, 0, Element.ELECTRO, 2, null, HOLD_PARTICLES, cast);
        } else {
            // Delayed early release shifts launch rather than firing an orb while still holding.
            long launch = Math.max(cast + TAP_LAUNCH_FRAME, frame);
            state.skillReady = launch + TAP_COOLDOWN_FRAMES;
            hit(launch, Kind.VIOLET_ORB, TAP_MULTIPLIER, Element.ELECTRO, 1, NORMAL_ICD_TAG, cast);
        }
    }
    private void roseEvent(long at, Kind kind, double multiplier, double gauge, long cast) {
        timeline.schedule(at, frame -> {
            if (state.alive() && roseCast == cast)
                hits.accept(new Hit(frame, kind, multiplier, Element.ELECTRO, gauge,
                        gauge == 0 ? null : "Elemental Burst", false, false, 0, cast));
        });
    }
    public boolean roseActive(long frame) {
        return state.alive() && roseCast >= 0 && frame >= roseCast + ADAPTED_ROSE_FORMATION_FRAME
                && frame <= roseCast + ADAPTED_ROSE_FORMATION_FRAME + ROSE_DURATION_FRAMES;
    }
    @Override public void leaveField(long frame) { skillHeld = -1; super.leaveField(frame); }
    @Override public void cancelCasts(long frame) {
        leaveField(frame);
        roseCast = -1;
    }
    @Override protected void normal(long frame, int index) {
        combo(frame, index, 4, RECOVERY_FRAMES[index]);
        hit(frame + HIT_FRAMES[index], Kind.NORMAL, MULTIPLIERS[index], Element.ELECTRO, 1, NORMAL_ICD_TAG, frame);
        if (index < 3) charge(frame + CHARGE_FRAMES[index], CHARGED_STAMINA_COST,
                ADAPTED_CHARGED_RECOVERY_FRAMES, ADAPTED_CHARGED_SWITCH_FRAMES,
                at -> hit(at + ADAPTED_CHARGED_HIT_FRAMES, Kind.CHARGED, 1.7712, Element.ELECTRO, 1, CHARGED_ICD_TAG, frame));
    }
}

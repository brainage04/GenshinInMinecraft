package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import io.github.brainage04.genshininminecraft.rules.Stats;
import java.util.function.Consumer;

/** Kaeya, C0, ascension 0, talent 1. Explicit Frostgnaw and contact-sampled Glacial Waltz. */
public final class KaeyaKit extends NormalAttackKit {
    public static final double BURST_COST = 60;
    public static final long CHARGED_SWITCH_FRAME = 34;
    public static final int CHARGED_HIT_FRAME = 16;
    public static final int CHARGED_RECOVERY_FRAMES = 54;
    public static final double SKILL_MULTIPLIER = 1.912;
    public static final double SKILL_GAUGE = 2;
    public static final int SKILL_HIT_FRAME = 28;
    public static final int SKILL_COOLDOWN_START_FRAME = 25;
    public static final int SKILL_COOLDOWN_FRAMES = 360;
    public static final int SKILL_RECOVERY_FRAME = 53;
    public static final int SKILL_SWITCH_FRAME = 49;
    // Empirical 2/3 outcomes (1:2) do not prove exact RNG probabilities. Choose a possible integer.
    public static final int ADAPTED_SKILL_PARTICLES = 3;
    public static final double BURST_MULTIPLIER = .776;
    public static final int BURST_FIRST_CONTACT_FRAME = 52;
    public static final int BURST_COOLDOWN_START_FRAME = 48;
    public static final int BURST_COOLDOWN_FRAMES = 900;
    public static final int BURST_DURATION_FRAMES = 480;
    public static final int BURST_RECOVERY_FRAME = 77;
    public static final int BURST_SWITCH_FRAME = 76;
    public static final int ICICLE_COUNT = 3;
    // Original KQM 75–100 frame, enemy-independent lock; conflicting guide/wiki claim 30 frames.
    public static final int ICICLE_DAMAGE_LOCK_FRAMES = 75;
    public static final int ADAPTED_REVOLUTION_FRAMES = 120; // Source says approximately two seconds.
    public static final int ADAPTED_CONTACT_SAMPLE_FRAMES = 3;
    private final long[] icicleReady = new long[ICICLE_COUNT];
    private long burstCast = -1;
    private Stats burstStats;
    private static final double[] MULTIPLIERS = {.5375, .5169, .6527, .7086, .8824};
    private static final int[] HIT_FRAMES = {14, 9, 14, 23, 30};
    private static final int[] RECOVERY_FRAMES = {27, 27, 47, 46, 74};
    private static final int[] CHARGE_FRAMES = {36, 31, 55, 54};
    public static int normalStrike(int index) { return HIT_FRAMES[index]; }
    public static int normalRecovery(int index) { return RECOVERY_FRAMES[index]; }
    @Override protected int normalHitFrame(int index) { return normalStrike(index); }
    @Override protected int chargedHitFrame() { return CHARGED_HIT_FRAME; }
    public KaeyaKit(Consumer<Hit> hits) { this(new EventTimeline(), new Stamina(), hits); }
    public KaeyaKit(EventTimeline timeline, Stamina stamina, Consumer<Hit> hits) {
        super(CharacterBaseStats.Character.KAEYA, Weapon.SWORD, BURST_COST, timeline, stamina, hits);
    }
    @Override public boolean intent(Intent intent, long frame) {
        if (intent != Intent.SKILL_PRESS && intent != Intent.BURST_PRESS) return super.intent(intent, frame);
        advanceTo(frame);
        if (!state.alive() || frame < state.actionReady) return false;
        if (intent == Intent.SKILL_PRESS) {
            if (frame < state.skillReady) return false;
            startTalent(frame, SKILL_RECOVERY_FRAME, SKILL_SWITCH_FRAME);
            visual.begin(io.github.brainage04.genshininminecraft.rules.CombatVisual.Action.SKILL_TAP, frame,
                    SKILL_HIT_FRAME, SKILL_RECOVERY_FRAME);
            state.skillCooldown(frame + SKILL_COOLDOWN_START_FRAME, SKILL_COOLDOWN_FRAMES);
            hit(frame + SKILL_HIT_FRAME, Kind.FROSTGNAW, SKILL_MULTIPLIER, Element.CRYO,
                    SKILL_GAUGE, null, ADAPTED_SKILL_PARTICLES, frame);
        } else {
            if (frame < state.burstReady || state.energy < BURST_COST) return false;
            startTalent(frame, BURST_RECOVERY_FRAME, BURST_SWITCH_FRAME);
            visual.begin(io.github.brainage04.genshininminecraft.rules.CombatVisual.Action.BURST, frame,
                    BURST_FIRST_CONTACT_FRAME, BURST_RECOVERY_FRAME);
            // Reserve at acceptance, like Traveler, instead of the measured frame51 drain.
            // kaeya.md: after casting, before draining energy; retain the cached immutable variant.
            burstStats = state.stats(frame);
            state.energy -= BURST_COST;
            state.burstCooldown(frame + BURST_COOLDOWN_START_FRAME, BURST_COOLDOWN_FRAMES);
            burstCast = frame;
            java.util.Arrays.fill(icicleReady, frame + BURST_FIRST_CONTACT_FRAME);
            sampleIcicles(frame + BURST_FIRST_CONTACT_FRAME, frame);
        }
        return true;
    }
    @Override public void cancelCasts(long frame) {
        leaveField(frame);
        burstCast = -1;
        burstStats = null;
    }
    public Stats burstStats() { return burstStats; }
    public boolean burstActive(long frame) {
        return state.alive() && burstCast >= 0 && frame >= burstCast + BURST_FIRST_CONTACT_FRAME
                && frame < burstCast + BURST_FIRST_CONTACT_FRAME + BURST_DURATION_FRAMES;
    }
    /** Per-icicle damage lock is enemy-independent, and starts only on an actual world contact. */
    public boolean connectIcicle(int icicle, long frame) {
        if (!burstActive(frame) || frame < icicleReady[icicle]) return false;
        icicleReady[icicle] = frame + ICICLE_DAMAGE_LOCK_FRAMES;
        return true;
    }
    private void sampleIcicles(long frame, long cast) {
        timeline.schedule(frame, at -> {
            if (!state.alive() || burstCast != cast) return;
            boolean end = at == cast + BURST_FIRST_CONTACT_FRAME + BURST_DURATION_FRAMES;
            hits.accept(new Hit(at, end ? Kind.ICICLES_END : Kind.ICICLES, BURST_MULTIPLIER,
                    Element.CRYO, 1, "Elemental Burst", false, false, 0, cast));
            if (!end) sampleIcicles(at + ADAPTED_CONTACT_SAMPLE_FRAMES, cast);
        });
    }
    @Override protected void normal(long frame, int index) {
        combo(frame, index, 5, RECOVERY_FRAMES[index]);
        hit(frame + HIT_FRAMES[index], Kind.NORMAL, MULTIPLIERS[index], Element.PHYSICAL, 0, null, frame);
        if (index < 4) charge(frame + CHARGE_FRAMES[index], Stamina.SWORD_CHARGED_COST, CHARGED_RECOVERY_FRAMES,
                (int) CHARGED_SWITCH_FRAME, at -> {
                    // Original sheet: 16-frame first hit; second hit has zero offset, not frame zero.
                    hit(at + CHARGED_HIT_FRAME, Kind.CHARGED, .5504, Element.PHYSICAL, 0, null, frame);
                    hit(at + CHARGED_HIT_FRAME, Kind.CHARGED, .731, Element.PHYSICAL, 0, null, frame);
                });
    }
}

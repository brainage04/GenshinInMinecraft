package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import java.util.function.Consumer;

/** Amber, C0, ascension 0, talent 1. Bow aim, landed puppet and explicit rain waves. */
public final class AmberKit extends NormalAttackKit {
    public static final double BURST_COST = 40;
    public static final int ADAPTED_AIM_ENTER_FRAMES = 9;
    public static final int AIMED_MIN_RELEASE_FRAME = 15;
    public static final int FULL_CHARGE_FRAME = 86;
    public static final int AIMED_RECOVERY_FRAMES = 10;
    public static final double AIMED_MULTIPLIER = .4386;
    public static final double CHARGED_MULTIPLIER = 1.24;
    public static final double CHARGED_GAUGE = 2;
    public static final double PUPPET_HP_FRACTION = .4136;
    public static final double PUPPET_MULTIPLIER = 1.232;
    public static final int PUPPET_PARTICLES = 4;
    public static final int PUPPET_LIFETIME_FRAMES = 480;
    public static final int ADAPTED_PUPPET_LANDING_FRAME = 45;
    public static final int SKILL_COOLDOWN_START_FRAME = 5;
    public static final int SKILL_COOLDOWN_FRAMES = 900;
    public static final int SKILL_RECOVERY_FRAME = 32;
    public static final int SKILL_SWITCH_FRAME = 23;
    public static final double BURST_MULTIPLIER = .2808;
    public static final int BURST_FIRST_HIT_FRAME = 72;
    public static final int BURST_DURATION_FRAMES = 120;
    public static final int BURST_COOLDOWN_START_FRAME = 56;
    public static final int BURST_COOLDOWN_FRAMES = 720;
    public static final int BURST_RECOVERY_FRAME = 111;
    public static final int BURST_SWITCH_FRAME = 61;
    // Individual timestamps are unknown. This deliberately non-uniform schedule is NOT duration / 18.
    private static final int[] ADAPTED_RAIN_WAVE_OFFSETS = {0, 6, 12, 18, 24, 30, 36, 42, 48, 60, 66, 72, 78, 84, 90, 96, 108, 119};
    private static final double[] MULTIPLIERS = {.3612, .3612, .4644, .4730, .5934};
    private static final int[] RELEASE_FRAMES = {14, 10, 27, 26, 26};
    private static final int[] RECOVERY_FRAMES = {26, 22, 37, 34, 60};
    private boolean attackHeld;
    private boolean aiming;
    private long attackStart;
    private long aimGeneration;
    private long puppetCast = -1;
    private double puppetHp;
    private long rainCast = -1;

    public AmberKit(Consumer<Hit> hits) { this(new EventTimeline(), new Stamina(), hits); }
    public AmberKit(EventTimeline timeline, Stamina stamina, Consumer<Hit> hits) {
        super(CharacterBaseStats.Character.AMBER, Weapon.BOW, CharacterState.TRAINING_BOW_BASE_ATK,
                BURST_COST, timeline, stamina, hits);
    }
    public boolean aiming() { return aiming; }
    public boolean fullyCharged() { return aiming && frame() - attackStart >= FULL_CHARGE_FRAME; }
    public double puppetHp() { return puppetHp; }
    public boolean puppetAlive() { return puppetCast >= 0 && puppetHp > 0; }
    @Override public boolean intent(Intent intent, long frame) {
        advanceTo(frame);
        if (intent == Intent.ATTACK_RELEASE) {
            if (!attackHeld) return super.intent(intent, frame);
            attackHeld = false;
            if (!aiming) return super.intent(intent, frame);
            aiming = false;
            long release = Math.max(frame, attackStart + AIMED_MIN_RELEASE_FRAME);
            boolean charged = frame - attackStart >= FULL_CHARGE_FRAME;
            startTalent(release, AIMED_RECOVERY_FRAMES, AIMED_RECOVERY_FRAMES);
            hit(release, Kind.CHARGED, charged ? CHARGED_MULTIPLIER : AIMED_MULTIPLIER,
                    charged ? Element.PYRO : Element.PHYSICAL, charged ? CHARGED_GAUGE : 0,
                    charged ? "Charged Attack" : null, attackStart);
            return true;
        }
        if (intent == Intent.ATTACK_PRESS) {
            if (attackHeld || !super.intent(intent, frame)) return false;
            attackHeld = true;
            attackStart = frame;
            long expected = ++aimGeneration;
            timeline.schedule(frame + ADAPTED_AIM_ENTER_FRAMES, at -> {
                if (!attackHeld || expected != aimGeneration || !state.alive()) return;
                // Cancel the unlaunched normal before its earliest (10-frame) arrow release.
                super.leaveField(at);
                aiming = true;
                state.actionReady = Long.MAX_VALUE;
            });
            return true;
        }
        if (intent != Intent.SKILL_PRESS && intent != Intent.BURST_PRESS) return super.intent(intent, frame);
        if (!state.alive() || frame < state.actionReady) return false;
        if (intent == Intent.SKILL_PRESS) {
            if (frame < state.skillReady) return false;
            startTalent(frame, SKILL_RECOVERY_FRAME, SKILL_SWITCH_FRAME);
            state.skillReady = frame + SKILL_COOLDOWN_START_FRAME + SKILL_COOLDOWN_FRAMES;
            long cast = puppetCast = frame;
            timeline.schedule(frame + ADAPTED_PUPPET_LANDING_FRAME, at -> {
                if (puppetCast != cast || !state.alive()) return;
                puppetHp = maxHp() * PUPPET_HP_FRACTION;
                emit(at, Kind.BUNNY_LAND, 0, 0, null, 0, cast);
                timeline.schedule(at + PUPPET_LIFETIME_FRAMES, expiry -> {
                    if (puppetCast == cast) explodePuppet(expiry);
                });
            });
        } else {
            if (frame < state.burstReady || state.energy < BURST_COST) return false;
            startTalent(frame, BURST_RECOVERY_FRAME, BURST_SWITCH_FRAME);
            state.energy -= BURST_COST; // Reserve at authoritative acceptance, not original frame59.
            state.burstReady = frame + BURST_COOLDOWN_START_FRAME + BURST_COOLDOWN_FRAMES;
            rainCast = frame;
            for (int wave = 0; wave < ADAPTED_RAIN_WAVE_OFFSETS.length; wave++) {
                Kind kind = (wave & 1) == 0 ? Kind.RAIN_INNER : Kind.RAIN_OUTER;
                long cast = frame;
                timeline.schedule(frame + BURST_FIRST_HIT_FRAME + ADAPTED_RAIN_WAVE_OFFSETS[wave], at -> {
                    if (rainCast == cast && state.alive()) emit(at, kind, BURST_MULTIPLIER, 1, "Elemental Burst", 0, cast);
                });
            }
        }
        return true;
    }
    public void damagePuppet(double amount, long frame) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid puppet damage");
        advanceTo(frame);
        if (!puppetAlive()) return;
        puppetHp = Math.max(0, puppetHp - amount);
        if (puppetHp == 0) explodePuppet(frame);
    }
    private void explodePuppet(long frame) {
        long cast = puppetCast;
        puppetCast = -1;
        puppetHp = 0;
        emit(frame, Kind.BUNNY_EXPLODE, PUPPET_MULTIPLIER, 2, null, PUPPET_PARTICLES, cast);
    }
    private void emit(long frame, Kind kind, double multiplier, double gauge, String tag, int particles, long cast) {
        hits.accept(new Hit(frame, kind, multiplier, Element.PYRO, gauge, tag, false, false, particles, cast));
    }
    @Override public void leaveField(long frame) {
        attackHeld = false;
        aiming = false;
        ++aimGeneration;
        super.leaveField(frame);
    }
    @Override public void cancelCasts(long frame) {
        leaveField(frame);
        puppetCast = -1;
        puppetHp = 0;
        rainCast = -1;
    }
    @Override protected void normal(long frame, int index) {
        combo(frame, index, 5, RECOVERY_FRAMES[index]);
        hit(frame + RELEASE_FRAMES[index], Kind.NORMAL, MULTIPLIERS[index], Element.PHYSICAL, 0, null, frame);
    }
}

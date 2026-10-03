package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Energy;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Stats;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import java.util.List;
import java.util.function.Consumer;

/** Aether, C0, ascension 0, talent level 1. All timestamps are reference frames. */
public final class TravelerAnemoKit {
    public static final int STARTER_LEVEL = 20;
    public static final double BURST_COST = 60;
    // Adaptations for unsourced input/reset/charge-stage boundaries; see decisions.md.
    public static final long COMBO_RESET_FRAMES = 90;
    public static final double TRAINING_SWORD_BASE_ATK = 23;
    public static final long FULL_HOLD_RELEASE_FRAME = 108;
    public static final long STRONG_HOLD_THRESHOLD = 30;
    public static final long BURST_DURATION_FRAMES = 360;
    private static final double[] NORMAL_MULTIPLIERS = {0.445, 0.434, 0.530, 0.583, 0.708};
    // traveler-anemo.md: Aether original hitmark trials (first trial) and TCL recovery.
    private static final int[] NORMAL_HIT_FRAMES = {13, 13, 16, 30, 25};
    private static final int[] NORMAL_RECOVERY_FRAMES = {23, 32, 40, 49, 81};
    private static final int[] CHARGE_TRANSITION_FRAMES = {28, 28, 36, 45};
    private static final int[] CUTTING_FRAMES = {21, 30, 51, 60, 81, 90};
    private static final List<CharacterBaseStats.Character> STARTER_PARTY = List.of(CharacterBaseStats.Character.TRAVELER_ANEMO);

    public enum Intent { ATTACK_PRESS, ATTACK_RELEASE, SKILL_PRESS, SKILL_RELEASE, BURST_PRESS }
    public enum Kind { NORMAL, CHARGED, CUTTING, STORM, TORNADO }
    public record Hit(long frame, Kind kind, double multiplier, Element element,
            double gauge, String icdTag, boolean mayAbsorb, boolean absorbedHit,
            int particles, long castFrame) {}

    private final EventTimeline timeline;
    private final Consumer<Hit> hits;
    private final Stats stats = starterStats();
    private final Stamina stamina = new Stamina();
    private double hp = stats.hp();
    private double energy;
    private long skillReady;
    private long burstReady;
    private long actionReady;
    private long comboReset;
    private int combo;
    private boolean attackHeld;
    private long attackGeneration;
    private boolean skillHeld;
    private long skillStart;
    private long skillGeneration;
    private long actionGeneration;

    public TravelerAnemoKit(Consumer<Hit> hits) { this(new EventTimeline(), hits); }
    public TravelerAnemoKit(EventTimeline timeline, Consumer<Hit> hits) {
        this.timeline = java.util.Objects.requireNonNull(timeline);
        this.hits = java.util.Objects.requireNonNull(hits);
        stamina.advanceTo(timeline.frame());
    }
    public List<CharacterBaseStats.Character> party() { return STARTER_PARTY; }
    public CharacterBaseStats.Character activeCharacter() { return party().getFirst(); }
    public Stats stats() { return stats; }
    public double hp() { return hp; }
    public double maxHp() { return stats.hp(); }
    public double energy() { return energy; }
    public Stamina stamina() { return stamina; }
    private static Stats starterStats() {
        var base = CharacterBaseStats.at(CharacterBaseStats.Character.TRAVELER_ANEMO, STARTER_LEVEL);
        return new Stats(STARTER_LEVEL, base.hp(), base.atk(), base.def(), TRAINING_SWORD_BASE_ATK,
                0, 0, 0, 0, 0, 0, .05, .5, 0, 0, java.util.Map.of());
    }
    public long frame() { return timeline.frame(); }
    public long skillReadyFrame() { return skillReady; }
    public long burstReadyFrame() { return burstReady; }
    public long skillRemaining() { return skillHeld ? 480 : Math.max(0, skillReady - frame()); }
    public long burstRemaining() { return Math.max(0, burstReady - frame()); }
    public int comboIndex() { return combo; }
    public long comboResetFrame() { return comboReset; }
    public boolean skillHeld() { return skillHeld; }
    public void setHp(double value) { hp = Math.clamp(value, 0, maxHp()); }
    public void grantEnergy(double amount) { energy = Math.clamp(energy + amount, 0, BURST_COST); }
    public void grantParticles(int count) {
        grantEnergy(count * Energy.received(Energy.Item.PARTICLE, Element.ANEMO, Element.ANEMO, true, 1, 1));
    }
    public void advanceTo(long frame) {
        timeline.advanceTo(frame);
        stamina.advanceTo(frame);
    }
    public void schedule(long frame, java.util.function.LongConsumer event) { timeline.schedule(frame, event); }

    public boolean intent(Intent intent, long frame) {
        advanceTo(frame);
        if (intent == Intent.ATTACK_RELEASE) { attackHeld = false; return true; }
        if (intent == Intent.SKILL_RELEASE) {
            if (!skillHeld) return false;
            releaseSkill(frame);
            return true;
        }
        if (hp <= 0) return false;
        return switch (intent) {
            case ATTACK_PRESS -> attack(frame);
            case SKILL_PRESS -> skill(frame);
            case BURST_PRESS -> burst(frame);
            default -> false;
        };
    }

    private boolean attack(long frame) {
        if (attackHeld || skillHeld || frame < actionReady) return false;
        if (frame >= comboReset) combo = 0;
        int hit = combo;
        combo = (combo + 1) % 5;
        comboReset = frame + NORMAL_RECOVERY_FRAMES[hit] + COMBO_RESET_FRAMES;
        actionReady = frame + NORMAL_RECOVERY_FRAMES[hit];
        attackHeld = true;
        long press = ++attackGeneration;
        long action = ++actionGeneration;
        emit(frame + NORMAL_HIT_FRAMES[hit], Kind.NORMAL, NORMAL_MULTIPLIERS[hit], Element.PHYSICAL,
                0, null, false, false, 0, frame, action);
        if (hit < 4) timeline.schedule(frame + CHARGE_TRANSITION_FRAMES[hit], at -> {
            if (!attackHeld || press != attackGeneration || action != actionGeneration || hp <= 0) return;
            if (!stamina.chargedAttack(at)) return;
            combo = 0;
            actionReady = at + 55;
            emit(at + 10, Kind.CHARGED, 0.559, Element.PHYSICAL, 0, null, false, false, 0, frame, action);
            emit(at + 21, Kind.CHARGED, 0.607, Element.PHYSICAL, 0, null, false, false, 0, frame, action);
        });
        return true;
    }

    private boolean skill(long frame) {
        if (skillHeld || frame < skillReady || frame < actionReady) return false;
        attackHeld = false;
        ++actionGeneration;
        combo = 0;
        skillHeld = true;
        skillStart = frame;
        long generation = ++skillGeneration;
        for (int i = 0; i < CUTTING_FRAMES.length; i++) {
            int tick = i;
            timeline.schedule(frame + CUTTING_FRAMES[i], at -> {
                if (!skillHeld || generation != skillGeneration || hp <= 0) return;
                hits.accept(new Hit(at, Kind.CUTTING, tick < 2 ? 0.12 : 0.168, Element.ANEMO, 1,
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
        long storm = tap ? Math.max(skillStart + 32, frame) : frame + 5;
        long cooldownStart = tap ? skillStart + 27 : frame;
        skillReady = cooldownStart + (strong ? 480 : 300);
        actionReady = tap ? skillStart + 74 : frame + 48;
        emit(storm, Kind.STORM, strong ? 1.92 : 1.76, Element.ANEMO, 1,
                null, true, true, strong ? 3 : 2, skillStart, actionGeneration);
    }

    private boolean burst(long frame) {
        if (skillHeld || frame < burstReady || energy < BURST_COST || frame < actionReady) return false;
        energy -= BURST_COST;
        burstReady = frame + 900;
        actionReady = frame + 111;
        attackHeld = false;
        combo = 0;
        ++actionGeneration;
        // Original Aether sheet: first contact at 96; nine 30-frame ticks through frame 336.
        // Burst persists independently of later normal/skill actions.
        for (int tick = 0; tick < 9; tick++) {
            timeline.schedule(frame + 96 + tick * 30, at -> {
                if (hp > 0) hits.accept(new Hit(at, Kind.TORNADO, 0.808, Element.ANEMO, 1,
                        "Elemental Burst", true, true, 0, frame));
            });
        }
        return true;
    }

    private void emit(long frame, Kind kind, double multiplier, Element element, double gauge,
            String tag, boolean absorb, boolean absorbedHit, int particles, long cast, long generation) {
        timeline.schedule(frame, at -> {
            if (hp > 0 && generation == actionGeneration) hits.accept(new Hit(at, kind, multiplier,
                    element, gauge, tag, absorb, absorbedHit, particles, cast));
        });
    }
}

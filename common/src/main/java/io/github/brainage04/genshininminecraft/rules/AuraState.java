package io.github.brainage04.genshininminecraft.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * One target's ordinary player-applied auras, in post-tax U. Not an innate aura/shield model.
 * Single-threaded; all mutations use monotonically increasing 60 fps frames. The caller must
 * schedule each nextElectroChargedTickFrame() on its EventTimeline and drain those events before
 * later hits. EC ownership/EM snapshots and AoE/Swirl spread belong to the caller, not this state.
 */
public final class AuraState {
    public static final double AURA_TAX = 0.8;
    // Current summary formula (elements.md: Aura decay); historical 4U=16.8s disagrees.
    // These named inputs explicitly select the current 2.5u+7 baseline, not hidden precision.
    public static final double FRESH_DURATION_SECONDS_PER_SOURCE_U = 2.5;
    public static final double FRESH_DURATION_BASE_SECONDS = 7;
    public static final long EC_TICK_INTERVAL_FRAMES = 60;
    public static final long EC_FINAL_TICK_EXCLUSION_FRAMES = 30;
    public static final double EC_TICK_GAUGE = 0.4;
    // elements.md: Frozen kinematic summary, not the conflicting historical extension fit.
    public static final double FROZEN_MIN_LOSS_PER_SECOND = 0.4;
    public static final double FROZEN_ACCELERATION = 0.1;
    public static final double UNFROZEN_RECOVERY = 0.2;
    public static final double SHATTER_GAUGE = 8;

    public record Parameters(long electroChargedDamageCooldownFrames, double freezeResistance) {
        // Approximate 0.5s EC cooldown (elements.md: conflicts); deliberately configurable.
        public static final Parameters BASELINE = new Parameters(30, 0);
        public Parameters {
            if (electroChargedDamageCooldownFrames < 0) throw new IllegalArgumentException("Negative cooldown");
            if (!Double.isFinite(freezeResistance) || freezeResistance < 0 || freezeResistance > 1) {
                throw new IllegalArgumentException("Freeze resistance must be in [0,1]");
            }
        }
    }

    public record ElectroChargedTick(boolean dealsDamage, double hydroConsumed,
            double electroConsumed, OptionalLong nextTickFrame) {}

    private static final class Aura {
        double gauge;
        double lossPerFrame;
    }

    private final Aura pyro = new Aura();
    private final Aura cryo = new Aura();
    private final Aura electro = new Aura();
    private final Aura hydro = new Aura();
    private final Parameters parameters;
    private long frame;
    private double frozen;
    private double frozenLossRate = FROZEN_MIN_LOSS_PER_SECOND;
    private long ecNextFrame = -1;
    private long ecRegularFrame = -1;
    private long ecLastDamageFrame = -1;
    private boolean ecEarlyExpiry;
    private long superconductUntilFrame = -1;

    public AuraState() { this(Parameters.BASELINE); }
    public AuraState(Parameters parameters) { this.parameters = java.util.Objects.requireNonNull(parameters); }

    public long frame() { return frame; }
    public double gauge(Element element) { return element.canBeAura() ? aura(element).gauge : 0; }
    public double frozenGauge() { return frozen; }
    public boolean isFrozen() { return frozen > 0; }

    public double physicalResistanceReduction() {
        return frame < superconductUntilFrame ? Reaction.SUPERCONDUCT_PHYSICAL_RES_REDUCTION : 0;
    }

    public long physicalResistanceReductionUntilFrame() { return superconductUntilFrame; }

    public static double freshDurationSeconds(double sourceGauge) {
        return FRESH_DURATION_SECONDS_PER_SOURCE_U * sourceGauge + FRESH_DURATION_BASE_SECONDS;
    }

    public double remainingAuraFrames(Element element) {
        Aura aura = aura(element);
        return aura.gauge == 0 ? 0 : aura.gauge / aura.lossPerFrame;
    }

    public double remainingFrozenSeconds() {
        if (frozen == 0) return 0;
        double resistanceScale = 1 - parameters.freezeResistance;
        return (Math.sqrt(frozenLossRate * frozenLossRate
                + 2 * FROZEN_ACCELERATION * frozen * resistanceScale) - frozenLossRate)
                / FROZEN_ACCELERATION;
    }

    public void advanceTo(long atFrame) {
        if (atFrame < frame) throw new IllegalArgumentException("Time cannot run backwards");
        long elapsed = atFrame - frame;
        decay(pyro, elapsed);
        decay(cryo, elapsed);
        decay(electro, elapsed);
        decay(hydro, elapsed);
        double seconds = Frames.seconds(elapsed);
        if (frozen > 0) {
            double duration = remainingFrozenSeconds();
            double frozenTime = Math.min(seconds, duration);
            double scale = 1 - parameters.freezeResistance;
            frozen = scale == 0 ? 0 : Math.max(0, frozen
                    - (frozenLossRate * frozenTime + 0.5 * FROZEN_ACCELERATION * frozenTime * frozenTime) / scale);
            frozenLossRate += FROZEN_ACCELERATION * frozenTime;
            if (seconds >= duration) frozen = 0;
            seconds -= frozenTime;
        }
        if (frozen == 0) frozenLossRate = Math.max(FROZEN_MIN_LOSS_PER_SECOND,
                frozenLossRate - UNFROZEN_RECOVERY * seconds);
        frame = atFrame;
    }

    /** Zero gauge and Physical/Anemo never create an aura; ICD must be checked by the caller. */
    public List<Reaction> applyHit(Element trigger, double sourceGauge, long atFrame) {
        if (!Double.isFinite(sourceGauge) || sourceGauge < 0) throw new IllegalArgumentException("Invalid gauge");
        advanceTo(atFrame);
        if (sourceGauge == 0 || trigger == Element.PHYSICAL) return List.of();
        if (trigger.canBeAura() && frozen == 0 && !hasOtherAura(trigger)) {
            refill(trigger, sourceGauge);
            return List.of();
        }
        if (trigger == Element.ANEMO && !anyAura() && frozen == 0) return List.of();
        List<Reaction> reactions = new ArrayList<>(2);
        if (trigger == Element.ANEMO) {
            swirl(sourceGauge, reactions);
        } else if (frozen > 0 && (trigger == Element.PYRO || trigger == Element.ELECTRO)) {
            // Frozen is Cryo-like. Non-blunt Pyro cannot also Vaporize underlying Hydro.
            Reaction.Type type = trigger == Element.PYRO ? Reaction.Type.MELT : Reaction.Type.SUPERCONDUCT;
            double amount = sourceGauge * (trigger == Element.PYRO ? 2 : 1);
            double consumed = consumeFrozen(amount) + consume(cryo, amount);
            reactions.add(new Reaction(type, consumed, trigger, Element.CRYO));
        } else {
            ordinaryPairs(trigger, sourceGauge, reactions);
            if (reactions.isEmpty()) refill(trigger, sourceGauge);
        }
        for (Reaction reaction : reactions) {
            if (reaction.type() == Reaction.Type.SUPERCONDUCT) {
                superconductUntilFrame = Math.addExact(frame, Reaction.SUPERCONDUCT_DURATION_FRAMES);
            }
        }
        updateEcSchedule(false);
        return Collections.unmodifiableList(reactions);
    }

    /**
     * Call BEFORE applyHit for a blunt elemental hit. The caller supplies pre-Shatter gauge loss:
     * the poise-to-gauge coefficient is unknown, so no blanket "every blunt contact" overload exists.
     */
    public Optional<Reaction> shatter(long atFrame, double preShatterGaugeConsumption) {
        if (!Double.isFinite(preShatterGaugeConsumption) || preShatterGaugeConsumption < 0) {
            throw new IllegalArgumentException("Invalid pre-Shatter gauge loss");
        }
        advanceTo(atFrame);
        consumeFrozen(preShatterGaugeConsumption);
        if (frozen == 0) return Optional.empty();
        return Optional.of(new Reaction(Reaction.Type.SHATTER, consumeFrozen(SHATTER_GAUGE),
                Element.PHYSICAL, Element.CRYO));
    }

    public OptionalLong nextElectroChargedTickFrame() {
        return ecNextFrame < 0 ? OptionalLong.empty() : OptionalLong.of(ecNextFrame);
    }

    /**
     * Sourced 60-frame ticks and 0.4U per aura ONLY when damage succeeds. damageAccepted lets
     * the adapter reject damage for external reasons. Stale/duplicate scheduled callbacks are inert.
     * Early final ticks use the first integer frame at expiry, ordered before aura removal; the
     * expiry-frame rounding and cooldown boundary ordering are documented Minecraft adaptations.
     */
    public ElectroChargedTick tickElectroCharged(long atFrame, boolean damageAccepted) {
        if (atFrame != ecNextFrame) return new ElectroChargedTick(false, 0, 0, nextElectroChargedTickFrame());
        boolean pairBeforeExpiry = hydro.gauge > 0 && electro.gauge > 0;
        boolean early = ecEarlyExpiry;
        advanceTo(atFrame);
        boolean pair = hydro.gauge > 0 && electro.gauge > 0;
        boolean cooldownReady = ecLastDamageFrame < 0
                || atFrame - ecLastDamageFrame >= parameters.electroChargedDamageCooldownFrames;
        boolean damage = damageAccepted && cooldownReady && (pair || early && pairBeforeExpiry);
        double h = 0;
        double e = 0;
        if (damage) {
            h = consume(hydro, EC_TICK_GAUGE);
            e = consume(electro, EC_TICK_GAUGE);
            ecLastDamageFrame = atFrame;
        }
        ecRegularFrame = pair ? Math.addExact(atFrame, EC_TICK_INTERVAL_FRAMES) : -1;
        updateEcSchedule(false);
        return new ElectroChargedTick(damage, h, e, nextElectroChargedTickFrame());
    }

    private void ordinaryPairs(Element trigger, double source, List<Reaction> reactions) {
        // Stable order is intentional for EC's two exposed auras; do not overwrite one enum slot.
        reactAgainst(Element.PYRO, trigger, source, reactions);
        reactAgainst(Element.CRYO, trigger, source, reactions);
        reactAgainst(Element.ELECTRO, trigger, source, reactions);
        reactAgainst(Element.HYDRO, trigger, source, reactions);
    }

    private void reactAgainst(Element existing, Element trigger, double source, List<Reaction> reactions) {
        Aura old = aura(existing);
        if (old.gauge == 0 || existing == trigger) return;
        Reaction.Type type;
        double factor = 1;
        if ((existing == Element.HYDRO && trigger == Element.ELECTRO)
                || (existing == Element.ELECTRO && trigger == Element.HYDRO)) {
            refill(trigger, source);
            reactions.add(new Reaction(Reaction.Type.ELECTRO_CHARGED, 0, trigger, existing));
            updateEcSchedule(true);
            return;
        } else if ((existing == Element.HYDRO && trigger == Element.CRYO)
                || (existing == Element.CRYO && trigger == Element.HYDRO)) {
            double formed = 2 * Math.min(old.gauge, source);
            // Unknown combined repeat-history + resistance: nonzero resistance uses fresh-only
            // kinematics on each formation. At zero resistance, retain sourced loss-rate history.
            if (parameters.freezeResistance > 0) frozenLossRate = FROZEN_MIN_LOSS_PER_SECOND;
            // Unknown repeat-Freeze refill: baseline uses maximum, not additive gauge.
            frozen = Math.max(frozen, formed);
            if (parameters.freezeResistance == 1) frozen = 0;
            type = Reaction.Type.FROZEN;
        } else if ((existing == Element.CRYO && trigger == Element.PYRO)
                || (existing == Element.PYRO && trigger == Element.CRYO)) {
            type = Reaction.Type.MELT;
            factor = trigger == Element.PYRO ? 2 : 0.5;
        } else if ((existing == Element.PYRO && trigger == Element.HYDRO)
                || (existing == Element.HYDRO && trigger == Element.PYRO)) {
            type = Reaction.Type.VAPORIZE;
            factor = trigger == Element.HYDRO ? 2 : 0.5;
        } else if ((existing == Element.PYRO && trigger == Element.ELECTRO)
                || (existing == Element.ELECTRO && trigger == Element.PYRO)) {
            type = Reaction.Type.OVERLOADED;
        } else if ((existing == Element.CRYO && trigger == Element.ELECTRO)
                || (existing == Element.ELECTRO && trigger == Element.CRYO)) {
            type = Reaction.Type.SUPERCONDUCT;
        } else return;
        reactions.add(new Reaction(type, consume(old, factor * source), trigger, existing));
    }

    private void swirl(double source, List<Reaction> reactions) {
        double amount = 0.5 * source;
        if (electro.gauge > 0 && hydro.gauge > 0) {
            boolean doubleSwirl = amount > electro.gauge;
            swirlAura(Element.ELECTRO, amount, reactions);
            if (doubleSwirl) swirlAura(Element.HYDRO, amount, reactions);
        } else if (frozen > 0 && hydro.gauge > 0) {
            boolean doubleSwirl = amount > hydro.gauge;
            swirlAura(Element.HYDRO, amount, reactions);
            if (doubleSwirl) swirlFrozen(amount, reactions);
        } else {
            swirlAura(Element.PYRO, amount, reactions);
            swirlAura(Element.ELECTRO, amount, reactions);
            swirlAura(Element.HYDRO, amount, reactions);
            if (frozen > 0) swirlFrozen(amount, reactions);
            else swirlAura(Element.CRYO, amount, reactions);
        }
    }

    private void swirlFrozen(double amount, List<Reaction> reactions) {
        reactions.add(new Reaction(Reaction.Type.SWIRL, consumeFrozen(amount) + consume(cryo, amount),
                Element.ANEMO, Element.CRYO));
    }

    private void swirlAura(Element element, double amount, List<Reaction> reactions) {
        Aura aura = aura(element);
        if (aura.gauge > 0) reactions.add(new Reaction(Reaction.Type.SWIRL,
                consume(aura, amount), Element.ANEMO, element));
    }

    private void refill(Element element, double source) {
        Aura aura = aura(element);
        double taxed = AURA_TAX * source;
        boolean fresh = aura.gauge == 0;
        if (taxed > aura.gauge) {
            aura.gauge = taxed;
            // Pyro's Version 3.0 exception changes rate only when the aura amount changes.
            if (fresh || element == Element.PYRO) {
                aura.lossPerFrame = taxed / (freshDurationSeconds(source) * Frames.PER_SECOND);
            }
        }
    }

    private void updateEcSchedule(boolean application) {
        ecEarlyExpiry = false;
        if (hydro.gauge == 0 || electro.gauge == 0) {
            ecNextFrame = -1;
            ecRegularFrame = -1;
            return;
        }
        if (ecRegularFrame < 0 && application) {
            ecRegularFrame = ecLastDamageFrame < 0 ? frame
                    : Math.max(frame, Math.addExact(ecLastDamageFrame, parameters.electroChargedDamageCooldownFrames));
        }
        ecNextFrame = ecRegularFrame;
        if (ecNextFrame < 0) return;
        double expiry = frame + Math.min(hydro.gauge / hydro.lossPerFrame, electro.gauge / electro.lossPerFrame);
        long expiryFrame = (long) Math.ceil(expiry);
        if (expiryFrame < ecNextFrame && ecLastDamageFrame >= 0
                && expiryFrame - ecLastDamageFrame > EC_FINAL_TICK_EXCLUSION_FRAMES) {
            ecNextFrame = expiryFrame;
            ecEarlyExpiry = true;
        }
    }

    private boolean hasOtherAura(Element element) {
        return element != Element.PYRO && pyro.gauge > 0
                || element != Element.CRYO && cryo.gauge > 0
                || element != Element.ELECTRO && electro.gauge > 0
                || element != Element.HYDRO && hydro.gauge > 0;
    }

    private boolean anyAura() { return pyro.gauge > 0 || cryo.gauge > 0 || electro.gauge > 0 || hydro.gauge > 0; }

    private Aura aura(Element element) {
        return switch (element) {
            case PYRO -> pyro;
            case CRYO -> cryo;
            case ELECTRO -> electro;
            case HYDRO -> hydro;
            default -> throw new IllegalArgumentException("Element cannot be an aura");
        };
    }

    private static void decay(Aura aura, long frames) {
        aura.gauge = Math.max(0, aura.gauge - aura.lossPerFrame * frames);
    }

    private static double consume(Aura aura, double amount) {
        double consumed = Math.min(aura.gauge, amount);
        aura.gauge = Math.max(0, aura.gauge - amount);
        return consumed;
    }

    private double consumeFrozen(double amount) {
        double consumed = Math.min(frozen, amount);
        frozen = Math.max(0, frozen - amount);
        return consumed;
    }
}

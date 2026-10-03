package io.github.brainage04.genshininminecraft.rules;

import java.util.Objects;

/** Gauge consumed is post-tax aura U; the triggering gauge supplied to AuraState is untaxed U. */
public record Reaction(Type type, double gaugeConsumed, Element triggeringElement, Element auraElement) {
    public enum Type {
        SWIRL, MELT, VAPORIZE, OVERLOADED, SUPERCONDUCT, ELECTRO_CHARGED, FROZEN, SHATTER
    }

    // spec/mechanics/damage.md: Superconduct secondary effect, independent of damage ICD.
    public static final double SUPERCONDUCT_PHYSICAL_RES_REDUCTION = 0.4;
    public static final long SUPERCONDUCT_DURATION_FRAMES = 720;

    public Reaction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(triggeringElement, "triggeringElement");
        Objects.requireNonNull(auraElement, "auraElement");
    }

    public boolean amplifying() {
        return type == Type.MELT || type == Type.VAPORIZE;
    }

    /** spec/mechanics/damage.md: amplifying directions (not gauge-consumption factors). */
    public double amplificationBase() {
        return switch (type) {
            case MELT -> triggeringElement == Element.PYRO ? 2.0 : 1.5;
            case VAPORIZE -> triggeringElement == Element.HYDRO ? 2.0 : 1.5;
            default -> 1.0;
        };
    }

    /** spec/mechanics/damage.md: Ordinary transformative reactions table. */
    public static double transformativeCoefficient(Type type) {
        return switch (type) {
            case SWIRL -> 0.6;
            case OVERLOADED -> 2.75;
            case SUPERCONDUCT -> 1.5;
            case ELECTRO_CHARGED -> 2.0;
            case SHATTER -> 3.0;
            default -> throw new IllegalArgumentException("Not a damaging transformative reaction");
        };
    }

    public static Element damageElement(Type type, Element swirledElement) {
        return switch (type) {
            case SWIRL -> {
                if (!swirledElement.canBeAura()) throw new IllegalArgumentException("Invalid Swirl");
                yield swirledElement;
            }
            case OVERLOADED -> Element.PYRO;
            case SUPERCONDUCT -> Element.CRYO;
            case ELECTRO_CHARGED -> Element.ELECTRO;
            case SHATTER -> Element.PHYSICAL;
            default -> throw new IllegalArgumentException("No transformative damage element");
        };
    }
}

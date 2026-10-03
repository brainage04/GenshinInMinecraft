package io.github.brainage04.genshininminecraft.rules;

/** Single-party energy, not a co-op ownership/range model. */
public final class Energy {
    public enum Item { PARTICLE, ORB }
    public enum Affinity { SAME, DIFFERENT, NEUTRAL }

    private Energy() {}

    /** spec/mechanics/elements.md: Energy particles and orbs table, before ER. */
    public static double baseEnergy(Item item, Affinity affinity, boolean onField, int partySize) {
        if (partySize < 1 || partySize > 4) throw new IllegalArgumentException("Party size 1–4");
        if (!onField && partySize == 1) throw new IllegalArgumentException("No off-field solo recipient");
        double base = switch (affinity) {
            case SAME -> 3;
            case DIFFERENT -> 1;
            case NEUTRAL -> 2;
        };
        double fieldFactor = onField ? 1 : switch (partySize) {
            case 2 -> 0.8;
            case 3 -> 0.7;
            case 4 -> 0.6;
            default -> throw new IllegalArgumentException("No off-field solo recipient");
        };
        return base * fieldFactor * (item == Item.ORB ? 3 : 1);
    }

    /** Null itemElement denotes a clear/neutral item, never a Physical-element particle. */
    public static double received(Item item, Element itemElement, Element recipientElement,
            boolean onField, int partySize, double energyRecharge) {
        Affinity affinity = itemElement == null ? Affinity.NEUTRAL
                : itemElement == recipientElement ? Affinity.SAME : Affinity.DIFFERENT;
        return baseEnergy(item, affinity, onField, partySize) * energyRecharge;
    }
}

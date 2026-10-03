package io.github.brainage04.genshininminecraft.rules;

public final class CharacterBaseStats {
    public enum Character { TRAVELER_ANEMO, AMBER, KAEYA, LISA }
    public record Entry(Element element, int level, int levelCap, double hp, double atk, double def) {
        public Stats ungearedStats() {
            return Stats.base(level, hp, atk, def);
        }
    }

    // spec/mechanics/damage.md: Starter character base stats. Named published rounded inputs,
    // NOT exact internal floats. Level 20 means 20/20 (before ascension); no True Moon boosts.
    public static final Entry TRAVELER_LEVEL_1 = new Entry(Element.ANEMO, 1, 20, 911.79, 17.81, 57.23);
    public static final Entry TRAVELER_LEVEL_20 = new Entry(Element.ANEMO, 20, 20, 2342.39, 45.75, 147.01);
    public static final Entry AMBER_LEVEL_1 = new Entry(Element.PYRO, 1, 20, 793.26, 18.70, 50.36);
    public static final Entry AMBER_LEVEL_20 = new Entry(Element.PYRO, 20, 20, 2037.88, 48.04, 129.37);
    public static final Entry KAEYA_LEVEL_1 = new Entry(Element.CRYO, 1, 20, 975.62, 18.70, 66.38);
    public static final Entry KAEYA_LEVEL_20 = new Entry(Element.CRYO, 20, 20, 2506.36, 48.04, 170.53);
    public static final Entry LISA_LEVEL_1 = new Entry(Element.ELECTRO, 1, 20, 802.38, 19.41, 48.07);
    public static final Entry LISA_LEVEL_20 = new Entry(Element.ELECTRO, 20, 20, 2061.30, 49.87, 123.49);

    private CharacterBaseStats() {}

    public static Entry at(Character character, int level) {
        if (level != 1 && level != 20) throw new IllegalArgumentException("Sourced levels are 1 and 20");
        return switch (character) {
            case TRAVELER_ANEMO -> level == 1 ? TRAVELER_LEVEL_1 : TRAVELER_LEVEL_20;
            case AMBER -> level == 1 ? AMBER_LEVEL_1 : AMBER_LEVEL_20;
            case KAEYA -> level == 1 ? KAEYA_LEVEL_1 : KAEYA_LEVEL_20;
            case LISA -> level == 1 ? LISA_LEVEL_1 : LISA_LEVEL_20;
        };
    }
}

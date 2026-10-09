package io.github.brainage04.genshininminecraft.rules;

public final class CharacterBaseStats {
    public enum Character { TRAVELER_ANEMO, AMBER, KAEYA, LISA }
    public record Entry(Element element, int level, int levelCap, double hp, double atk, double def) {
        public Stats ungearedStats() {
            return Stats.base(level, hp, atk, def);
        }
    }

    // ExcelBinOutput/{Avatar,AvatarCurve}ExcelConfigData.json @ 792978e5503ecfba73dcb3562ed44a0d35a2abe2.
    // Exported decimal base inputs × Lv20 curve2.569; ascension0, no progression-dependent Traveler boosts.
    public static final Entry TRAVELER_LEVEL_1 = new Entry(Element.ANEMO, 1, 20, 911.791, 17.808, 57.225);
    public static final Entry TRAVELER_LEVEL_20 = new Entry(Element.ANEMO, 20, 20, 2342.391079, 45.748752, 147.011025);
    public static final Entry AMBER_LEVEL_1 = new Entry(Element.PYRO, 1, 20, 793.2582, 18.6984, 50.358);
    public static final Entry AMBER_LEVEL_20 = new Entry(Element.PYRO, 20, 20, 2037.8803158, 48.0361896, 129.369702);
    public static final Entry KAEYA_LEVEL_1 = new Entry(Element.CRYO, 1, 20, 975.6164, 18.6984, 66.381);
    public static final Entry KAEYA_LEVEL_20 = new Entry(Element.CRYO, 20, 20, 2506.3585316, 48.0361896, 170.532789);
    public static final Entry LISA_LEVEL_1 = new Entry(Element.ELECTRO, 1, 20, 802.3761, 19.41072, 48.069);
    public static final Entry LISA_LEVEL_20 = new Entry(Element.ELECTRO, 20, 20, 2061.3042009, 49.86613968, 123.489261);

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

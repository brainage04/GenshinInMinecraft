package io.github.brainage04.genshininminecraft.rules;

import java.util.Map;

/** Fractions, not percentage points. Published base stats remain unrounded during arithmetic. */
public record Stats(
        int level,
        double baseHp, double baseAtk, double baseDef, double weaponBaseAtk,
        double hpPercent, double atkPercent, double defPercent,
        double flatHp, double flatAtk, double flatDef,
        double critRate, double critDamage, double elementalMastery,
        double commonDamageBonus, Map<Element, Double> damageBonuses) {
    public Stats {
        if (level < 1) throw new IllegalArgumentException("Level must be positive");
        damageBonuses = Map.copyOf(damageBonuses);
    }

    public static Stats base(int level, double hp, double atk, double def) {
        // spec/mechanics/damage.md: Crit table; ordinary base CR=5%, CD=50%.
        return new Stats(level, hp, atk, def, 0, 0, 0, 0, 0, 0, 0,
                0.05, 0.5, 0, 0, Map.of());
    }

    public double hp() {
        return baseHp * (1 + hpPercent) + flatHp;
    }

    public double atk() {
        return (baseAtk + weaponBaseAtk) * (1 + atkPercent) + flatAtk;
    }

    public double def() {
        return baseDef * (1 + defPercent) + flatDef;
    }

    public double damageBonus(Element element) {
        return commonDamageBonus + damageBonuses.getOrDefault(element, 0.0);
    }
}

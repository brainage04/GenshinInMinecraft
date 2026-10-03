package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Stats;

/** Mutable server-owned member state; switching never reconstructs this object. */
public final class CharacterState {
    public static final int STARTER_LEVEL = 20;
    public static final double STARTER_ENERGY_RECHARGE = 1;
    public static final double TRAINING_SWORD_BASE_ATK = 23;
    public static final double TRAINING_BOW_BASE_ATK = 23;
    public static final double TRAINING_CATALYST_BASE_ATK = 23;
    private final CharacterBaseStats.Character character;
    private final Stats stats;
    private final double burstCost;
    double hp;
    double energy;
    long skillReady;
    long burstReady;
    long actionReady;
    long comboReset;
    int combo;

    public CharacterState(CharacterBaseStats.Character character, double weaponAttack, double burstCost) {
        this.character = character;
        this.burstCost = burstCost;
        var base = CharacterBaseStats.at(character, STARTER_LEVEL);
        stats = new Stats(STARTER_LEVEL, base.hp(), base.atk(), base.def(), weaponAttack,
                0, 0, 0, 0, 0, 0, .05, .5, 0, 0, java.util.Map.of());
        hp = stats.hp();
    }
    public CharacterBaseStats.Character character() { return character; }
    public Element element() { return CharacterBaseStats.at(character, STARTER_LEVEL).element(); }
    public Stats stats() { return stats; }
    public double hp() { return hp; }
    public double maxHp() { return stats.hp(); }
    public double energy() { return energy; }
    public double energyRecharge() { return STARTER_ENERGY_RECHARGE; }
    public double burstCost() { return burstCost; }
    public long skillReadyFrame() { return skillReady; }
    public long burstReadyFrame() { return burstReady; }
    public int comboIndex() { return combo; }
    public long comboResetFrame() { return comboReset; }
    public boolean alive() { return hp > 0; }
    public void setHp(double value) {
        hp = Math.clamp(value, 0, maxHp());
        if (hp == 0) energy = 0; // party.md: fallen characters lose their Burst energy.
    }
    public void grantEnergy(double amount) { if (alive()) energy = Math.clamp(energy + amount, 0, burstCost); }
}

package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Stats;
import io.github.brainage04.genshininminecraft.rules.StarterLoadout;

/** Mutable server-owned member state; switching never reconstructs this object. */
public final class CharacterState {
    public static final int STARTER_LEVEL = StarterLoadout.LEVEL;
    public static final double STARTER_ENERGY_RECHARGE = 1;
    private final CharacterBaseStats.Character character;
    private final Stats[] stats;
    private final Stats[] closeArrowStats;
    private final Stats[] distantArrowStats;
    private long switchBuffExpires;
    private long switchPassiveReady;
    private final double burstCost;
    double hp;
    double energy;
    long skillReady;
    long burstReady;
    int skillCooldownFrames;
    int burstCooldownFrames;
    long actionReady;
    long comboReset;
    int combo;

    public CharacterState(CharacterBaseStats.Character character, double burstCost) {
        this.character = character;
        this.burstCost = burstCost;
        stats = variants(0);
        boolean bow = StarterLoadout.weapon(character) == StarterLoadout.SLINGSHOT;
        closeArrowStats = bow ? variants(StarterLoadout.SLINGSHOT_CLOSE_BONUS) : stats;
        distantArrowStats = bow ? variants(StarterLoadout.SLINGSHOT_DISTANT_BONUS) : stats;
        hp = maxHp();
    }
    public CharacterBaseStats.Character character() { return character; }
    public Element element() { return CharacterBaseStats.at(character, STARTER_LEVEL).element(); }
    private Stats[] variants(double attackBonus) {
        double buff = StarterLoadout.THRILLING_TALES.switchAtkPercent();
        // Cache the small set of equipment conditions once, never allocate stats per landed hit.
        Stats base = StarterLoadout.stats(character, false, 0, attackBonus);
        Stats buffed = StarterLoadout.stats(character, false, buff, attackBonus);
        boolean harbinger = StarterLoadout.weapon(character) == StarterLoadout.HARBINGER_OF_DAWN;
        return new Stats[]{base, harbinger ? StarterLoadout.stats(character, true, 0, attackBonus) : base,
                buffed, harbinger ? StarterLoadout.stats(character, true, buff, attackBonus) : buffed};
    }
    private int statIndex(long frame) {
        return (hp > maxHp() * StarterLoadout.HARBINGER_HP_THRESHOLD ? 1 : 0)
                + (frame < switchBuffExpires ? 2 : 0);
    }
    public Stats stats(long frame) { return stats[statIndex(frame)]; }
    public Stats normalChargedStats(long frame, long flightFrames) {
        double bonus = StarterLoadout.normalChargedBonus(character, flightFrames);
        return (bonus > 0 ? closeArrowStats : bonus < 0 ? distantArrowStats : stats)[statIndex(frame)];
    }
    public void switchTo(CharacterState incoming, long frame) {
        var weapon = StarterLoadout.weapon(character);
        if (weapon.switchAtkPercent() == 0 || frame < switchPassiveReady) return;
        incoming.switchBuffExpires = frame + StarterLoadout.THRILLING_TALES_DURATION_FRAMES;
        switchPassiveReady = frame + StarterLoadout.THRILLING_TALES_COOLDOWN_FRAMES;
    }
    public double hp() { return hp; }
    public double maxHp() { return stats[0].hp(); }
    public double energy() { return energy; }
    public double energyRecharge() { return STARTER_ENERGY_RECHARGE; }
    public double burstCost() { return burstCost; }
    public long skillReadyFrame() { return skillReady; }
    public long burstReadyFrame() { return burstReady; }
    public int skillCooldownFrames() { return skillCooldownFrames; }
    public int burstCooldownFrames() { return burstCooldownFrames; }
    public long actionReadyFrame() { return actionReady; }
    void skillCooldown(long start, int duration) { skillReady = start + duration; skillCooldownFrames = duration; }
    void burstCooldown(long start, int duration) { burstReady = start + duration; burstCooldownFrames = duration; }
    public int comboIndex() { return combo; }
    public long comboResetFrame() { return comboReset; }
    public boolean alive() { return hp > 0; }
    public void setHp(double value) {
        hp = Math.clamp(value, 0, maxHp());
        if (hp == 0) energy = 0; // party.md: fallen characters lose their Burst energy.
    }
    public void grantEnergy(double amount) { if (alive()) energy = Math.clamp(energy + amount, 0, burstCost); }
}

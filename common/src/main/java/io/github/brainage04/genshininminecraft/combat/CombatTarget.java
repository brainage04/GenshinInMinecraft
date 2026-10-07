package io.github.brainage04.genshininminecraft.combat;

import io.github.brainage04.genshininminecraft.rules.ApplicationIcd;
import io.github.brainage04.genshininminecraft.rules.AuraState;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.ConductiveState;
import io.github.brainage04.genshininminecraft.rules.ReactionDamageIcd;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.HilichurlProfile;
import io.github.brainage04.genshininminecraft.rules.Stats;
import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Server-owned encounter profile, with a vanilla-derived fallback for other training targets. */
public final class CombatTarget {
    public static final double VANILLA_HEALTH_TO_GENSHIN_HP = 100;
    public static final double DEFAULT_RESISTANCE = .10;
    private final LivingEntity entity;
    private final double maxHp;
    private final int level;
    private final double baseResistance;
    private double hp;
    private float mirroredHealth;
    private final AuraState aura = new AuraState();
    private final ConductiveState conductive = new ConductiveState();
    final ApplicationIcd application = new ApplicationIcd();
    final ReactionDamageIcd reactionDamage = new ReactionDamageIcd();
    Stats ecOwner;
    ServerPlayer ecPlayer;
    long scheduledEc = -1;
    private int swirlCount;
    private int syncedAura = -1;
    private int syncedConductive = -1;

    CombatTarget(LivingEntity entity) {
        this.entity = entity;
        boolean hilichurl = entity instanceof Hilichurl;
        level = hilichurl ? ((Hilichurl) entity).genshinLevel() : TravelerAnemoKit.STARTER_LEVEL;
        baseResistance = hilichurl ? HilichurlProfile.RESISTANCE : DEFAULT_RESISTANCE;
        maxHp = hilichurl ? HilichurlProfile.maxHp(level) : entity.getMaxHealth() * VANILLA_HEALTH_TO_GENSHIN_HP;
        hp = maxHp * entity.getHealth() / entity.getMaxHealth();
        mirroredHealth = entity.getHealth();
    }
    public LivingEntity entity() { return entity; }
    public int level() { return level; }
    public double defense() { return HilichurlProfile.defense(level); }
    public double endurance() { return entity instanceof Hilichurl ? HilichurlProfile.PLACEHOLDER_ENDURANCE : 0; }
    public double hp() { return hp; }
    public double maxHp() { return maxHp; }
    public AuraState aura() { return aura; }
    public ConductiveState conductive() { return conductive; }
    public int swirlCount() { return swirlCount; }
    void countSwirl() { swirlCount++; }
    public int auraElements() {
        if (!entity.isAlive()) return 0;
        int mask = 0;
        if (aura.gauge(Element.PYRO) > 0) mask |= 1 << Element.PYRO.ordinal();
        if (aura.gauge(Element.CRYO) > 0 || aura.isFrozen()) mask |= 1 << Element.CRYO.ordinal();
        if (aura.gauge(Element.ELECTRO) > 0) mask |= 1 << Element.ELECTRO.ordinal();
        if (aura.gauge(Element.HYDRO) > 0) mask |= 1 << Element.HYDRO.ordinal();
        return mask;
    }
    boolean auraChanged() {
        int mask = auraElements();
        int stacks = entity.isAlive() ? conductive.stacks() : 0;
        if (mask == syncedAura && stacks == syncedConductive) return false;
        syncedAura = mask;
        syncedConductive = stacks;
        return true;
    }
    public double resistance(Element element) {
        return baseResistance - (element == Element.PHYSICAL ? aura.physicalResistanceReduction() : 0);
    }
    void reconcileVanillaHealth() {
        if (entity.getHealth() != mirroredHealth) hp = maxHp * entity.getHealth() / entity.getMaxHealth();
    }
    boolean acceptsDamage(ServerPlayer player) {
        ServerLevel level = (ServerLevel) entity.level();
        return entity.isAlive() && !entity.isInvulnerableTo(level, level.damageSources().playerAttack(player));
    }
    boolean damage(ServerPlayer player, double amount) {
        ServerLevel level = (ServerLevel) entity.level();
        var source = level.damageSources().playerAttack(player);
        if (!entity.isAlive() || entity.isInvulnerableTo(level, source)) return false;
        reconcileVanillaHealth();
        hp = Math.max(0, hp - amount);
        entity.setLastHurtByPlayer(player, 100);
        entity.setLastHurtByMob(player);
        entity.getCombatTracker().recordDamage(source, (float) (amount * entity.getMaxHealth() / maxHp));
        mirroredHealth = (float) (entity.getMaxHealth() * hp / maxHp);
        entity.setHealth(mirroredHealth);
        level.broadcastDamageEvent(entity, source);
        if (hp > 0 && entity instanceof Hilichurl hilichurl) hilichurl.combatHurtSound();
        if (hp == 0) entity.die(source);
        return true;
    }
}

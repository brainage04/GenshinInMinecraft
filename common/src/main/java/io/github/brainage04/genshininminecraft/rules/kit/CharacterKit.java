package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import io.github.brainage04.genshininminecraft.rules.Stats;

/** Small plain-Java contract; kits retain explicit, character-specific action schedules. */
public interface CharacterKit {
    enum Intent {
        ATTACK_PRESS, ATTACK_RELEASE, SKILL_PRESS, SKILL_RELEASE, BURST_PRESS,
        TRAVERSAL_JUMP,
        SWITCH_1, SWITCH_2, SWITCH_3, SWITCH_4;
        public int switchSlot() { return ordinal() >= SWITCH_1.ordinal() ? ordinal() - SWITCH_1.ordinal() : -1; }
    }
    enum Kind { NORMAL, CHARGED, CUTTING, STORM, TORNADO, FROSTGNAW, ICICLES, ICICLES_END,
        BUNNY_LAND, BUNNY_EXPLODE, RAIN_INNER, RAIN_OUTER, VIOLET_ORB, VIOLET_HOLD, ROSE_PLACE, ROSE_DISCHARGE }
    enum Weapon { SWORD, BOW, CATALYST }
    record Hit(long frame, Kind kind, double multiplier, Element element,
            double gauge, String icdTag, boolean mayAbsorb, boolean absorbedHit,
            int particles, long castFrame) {}

    CharacterState state();
    Stamina stamina();
    Weapon weapon();
    long frame();
    void advanceTo(long frame);
    boolean intent(Intent intent, long frame);
    boolean canSwitch(long frame);
    void leaveField(long frame);
    /** Cancel every queued action and persistent field without resetting party resources. */
    default void cancelCasts(long frame) { leaveField(frame); }
    default Stats stats() { return state().stats(frame()); }
    default double hp() { return state().hp(); }
    default double maxHp() { return state().maxHp(); }
    default double energy() { return state().energy(); }
    default void setHp(double hp) { state().setHp(hp); }
    default void grantEnergy(double amount) { state().grantEnergy(amount); }
    default long skillReadyFrame() { return state().skillReadyFrame(); }
    default long burstReadyFrame() { return state().burstReadyFrame(); }
    default long skillRemaining() { return Math.max(0, skillReadyFrame() - frame()); }
    default long burstRemaining() { return Math.max(0, burstReadyFrame() - frame()); }
    default int comboIndex() { return state().comboIndex(); }
    default long comboResetFrame() { return state().comboResetFrame(); }
}

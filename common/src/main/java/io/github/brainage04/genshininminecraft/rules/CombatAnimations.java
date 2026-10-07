package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.CombatVisual.Action;
import io.github.brainage04.genshininminecraft.rules.kit.*;

/** Cached visual descriptors read from the same kit anchors that schedule gameplay. */
public final class CombatAnimations {
    public record Timing(int strike, int secondStrike, int recovery, boolean loop) {}
    private static final Timing[][] TIMINGS = new Timing[4][Action.values().length];
    static {
        for (int slot = 0; slot < 4; slot++) {
            for (int n = 0; n < (slot == 3 ? 4 : 5); n++) {
                int hit = switch (slot) {
                    case 0 -> TravelerAnemoKit.normalStrike(n); case 1 -> AmberKit.normalStrike(n);
                    case 2 -> KaeyaKit.normalStrike(n); default -> LisaKit.normalStrike(n);
                };
                int recovery = switch (slot) {
                    case 0 -> TravelerAnemoKit.normalRecovery(n); case 1 -> AmberKit.normalRecovery(n);
                    case 2 -> KaeyaKit.normalRecovery(n); default -> LisaKit.normalRecovery(n);
                };
                put(slot, Action.normal(n), hit, -1, recovery, false);
            }
            put(slot, Action.HURT, 3, -1, 18, false);
            put(slot, Action.FALLEN, 30, -1, 60, false);
        }
        put(0, Action.CHARGED, TravelerAnemoKit.CHARGED_FIRST_HIT_FRAME, TravelerAnemoKit.CHARGED_SECOND_HIT_FRAME, TravelerAnemoKit.CHARGED_RECOVERY_FRAMES, false);
        put(0, Action.SKILL_START, -1, -1, 21, false);
        put(0, Action.SKILL_HOLD, -1, -1, 60, true);
        put(0, Action.SKILL_TAP, TravelerAnemoKit.TAP_HIT_FRAME, -1, TravelerAnemoKit.TAP_RECOVERY_FRAMES, false);
        put(0, Action.SKILL_RELEASE, TravelerAnemoKit.HOLD_HIT_OFFSET, -1, TravelerAnemoKit.HOLD_RECOVERY_OFFSET, false);
        put(0, Action.BURST, TravelerAnemoKit.BURST_FIRST_HIT_FRAME, -1, TravelerAnemoKit.BURST_RECOVERY_FRAMES, false);
        put(1, Action.AIM_HOLD, AmberKit.FULL_CHARGE_FRAME, -1, AmberKit.FULL_CHARGE_FRAME, false);
        put(1, Action.AIM_RELEASE, 0, -1, AmberKit.AIMED_RECOVERY_FRAMES, false);
        put(1, Action.SKILL_TAP, AmberKit.SKILL_COOLDOWN_START_FRAME, -1, AmberKit.SKILL_RECOVERY_FRAME, false);
        put(1, Action.BURST, AmberKit.BURST_FIRST_HIT_FRAME, -1, AmberKit.BURST_RECOVERY_FRAME, false);
        put(2, Action.CHARGED, KaeyaKit.CHARGED_HIT_FRAME, KaeyaKit.CHARGED_HIT_FRAME, KaeyaKit.CHARGED_RECOVERY_FRAMES, false);
        put(2, Action.SKILL_TAP, KaeyaKit.SKILL_HIT_FRAME, -1, KaeyaKit.SKILL_RECOVERY_FRAME, false);
        put(2, Action.BURST, KaeyaKit.BURST_FIRST_CONTACT_FRAME, -1, KaeyaKit.BURST_RECOVERY_FRAME, false);
        put(3, Action.CHARGED, LisaKit.ADAPTED_CHARGED_HIT_FRAMES, -1, LisaKit.ADAPTED_CHARGED_RECOVERY_FRAMES, false);
        put(3, Action.SKILL_START, -1, -1, LisaKit.TAP_LAUNCH_FRAME, false);
        put(3, Action.SKILL_HOLD, -1, -1, 60, true);
        put(3, Action.SKILL_TAP, LisaKit.TAP_LAUNCH_FRAME, -1, LisaKit.TAP_RECOVERY_FRAME, false);
        put(3, Action.SKILL_RELEASE, LisaKit.HOLD_HIT_OFFSET, -1, LisaKit.HOLD_RECOVERY_OFFSET, false);
        put(3, Action.BURST, LisaKit.ROSE_PLACEMENT_FRAME, -1, LisaKit.BURST_RECOVERY_FRAME, false);
    }
    private static void put(int slot, Action action, int strike, int second, int recovery, boolean loop) {
        TIMINGS[slot][action.ordinal()] = new Timing(strike, second, recovery, loop);
    }
    public static Timing timing(int slot, Action action) { return TIMINGS[slot][action.ordinal()]; }
    /** Conditional Lisa taps skip windup on late release and retain the kit's shortened remaining recovery. */
    public static double seconds(int slot, Action action, double elapsed, int strike, int recovery) {
        var timing = timing(slot, action);
        double frame = Math.max(0, elapsed);
        if (timing.loop()) frame %= timing.recovery();
        else if (strike >= 0 && timing.strike() >= 0 && recovery >= 0) {
            if (frame < strike && strike > 0) frame *= (double) timing.strike() / strike;
            else frame = timing.strike() + (frame - strike) * (timing.recovery() - timing.strike()) / Math.max(1, recovery - strike);
        }
        return Math.min(frame, timing.recovery() - .00006) / 60;
    }
    private CombatAnimations() {}
}

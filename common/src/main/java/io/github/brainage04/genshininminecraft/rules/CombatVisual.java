package io.github.brainage04.genshininminecraft.rules;

/** Committed presentation only. Kits own transitions; this never schedules damage or changes resources. */
public final class CombatVisual {
    public enum Action {
        NONE, N1, N2, N3, N4, N5, CHARGED, AIM_HOLD, AIM_RELEASE,
        SKILL_START, SKILL_HOLD, SKILL_TAP, SKILL_RELEASE, BURST, HURT, FALLEN;
        private final String clip = "combat." + name().toLowerCase(java.util.Locale.ROOT);
        public String clip() { return clip; }
        public static Action normal(int index) {
            return switch (index) {
                case 0 -> N1; case 1 -> N2; case 2 -> N3; case 3 -> N4; case 4 -> N5;
                default -> throw new IllegalArgumentException("Normal index: " + index);
            };
        }
    }
    private Action action = Action.NONE;
    private int occurrence;
    private long startFrame;
    private long endFrame;
    private int strikeFrame;
    private int recoveryFrames;
    private long hurtStartFrame = -60;
    private int hurtOccurrence;
    public Action action(long frame) { return action != Action.FALLEN && frame >= endFrame ? Action.NONE : action; }
    public int occurrence() { return occurrence; }
    public long startFrame() { return startFrame; }
    public int strikeFrame() { return strikeFrame; }
    public int recoveryFrames() { return recoveryFrames; }
    public long hurtStartFrame() { return hurtStartFrame; }
    public int hurtOccurrence() { return hurtOccurrence; }
    public void begin(Action action, long start, int strike, int recovery) {
        this.action = action;
        startFrame = start;
        strikeFrame = strike;
        recoveryFrames = recovery;
        endFrame = recovery < 0 ? Long.MAX_VALUE : start + recovery;
        occurrence++;
    }
    public void hold(Action action, long start) { begin(action, start, -1, -1); }
    public void clear(long frame) { begin(Action.NONE, frame, -1, 0); }
    public void hurt(long frame) { hurtStartFrame = frame; hurtOccurrence++; }
}

package io.github.brainage04.genshininminecraft.rules;

/** Ordinary traversal, 60fps rules time. ADAPTED values are calibration knobs, not Genshin measurements. */
public final class Traversal {
    public enum Mode { FREE, CLIMB, GLIDE }
    public static final double CLIMB_ENTRY_STAMINA = 5;
    public static final double GLIDE_DRAIN_PER_SECOND = 3; // Approximate public player testing; stamina.md.
    public static final double ADAPTED_BLOCKS_PER_METRE = 1;
    public static final double ADAPTED_CLIMB_DRAIN_PER_SECOND = 5.36; // 8 × .67, owner playtest2026-10-04.
    public static final double ADAPTED_CLIMB_JUMP_COST = 25; // Citation-needed wiki25 conflicts with historical24.
    public static final double ADAPTED_CLIMB_BLOCKS_PER_TICK = .08;
    public static final double ADAPTED_CLIMB_JUMP_BLOCKS_PER_TICK = .24;
    public static final long ADAPTED_CLIMB_JUMP_FRAMES = 18;
    public static final double ADAPTED_JUMP_AWAY_BLOCKS_PER_TICK = .3;
    public static final double ADAPTED_GLIDE_MIN_CLEARANCE = 2;
    public static final double ADAPTED_GLIDE_FORWARD_BLOCKS_PER_TICK = .25;
    public static final double ADAPTED_GLIDE_DESCENT_BLOCKS_PER_TICK = .11755; // Historical2.351m/s, not pin-verified.
    public static final double ADAPTED_WALL_REACH = .18;
    public static final double ADAPTED_WALL_PRESS_BLOCKS_PER_TICK = .04;
    public static final double ADAPTED_WALL_LOWER_SAMPLE = .5;
    public static final double ADAPTED_WALL_UPPER_SAMPLE = 1.3;
    public static final double ADAPTED_WALL_APPROACH_DOT = .5;
    public static final double ADAPTED_COLLISION_EPSILON = .00001;
    public static final double ADAPTED_MANTLE_REACH = 1;
    public static final double ADAPTED_MANTLE_INSET = .35;
    public static final long ADAPTED_REATTACH_DELAY_FRAMES = 30;
    public static final double ADAPTED_SAFE_FALL_BLOCKS = 5;
    public static final double ADAPTED_LETHAL_FALL_BLOCKS = 25;
    public static final double ADAPTED_HORIZONTAL_ENERGY_GRAVITY = 20; // m/s² solely for the adapted damage curve.
    public static final double ADAPTED_SAFE_WATER_DEPTH = .9;
    public static final double ADAPTED_WING_HALF_SPAN = 1.2;
    public static final double ADAPTED_WING_BACK_OFFSET = .35;
    public static final double ADAPTED_WING_HEIGHT = 1.4;
    public static final int ADAPTED_WING_PARTICLE_SAMPLES = 12;
    public static final double ADAPTED_WING_DROOP = .2;

    private final Stamina stamina;
    private Mode mode = Mode.FREE;
    private long reattachReady;
    private long jumpEnd;
    private int jumpSide;
    public Traversal(Stamina stamina) { this.stamina = stamina; }
    public Mode mode() { return mode; }
    public boolean active() { return mode != Mode.FREE; }
    public boolean attach(long frame) {
        stamina.advanceTo(frame);
        if (mode != Mode.FREE || frame < reattachReady || stamina.exhausted()
                || stamina.current() < CLIMB_ENTRY_STAMINA) return false;
        mode = Mode.CLIMB;
        stamina.traversal(0, true, frame);
        return true;
    }
    public boolean openGlider(double clearance, long frame) {
        stamina.advanceTo(frame);
        if (mode != Mode.FREE || frame < reattachReady || clearance < ADAPTED_GLIDE_MIN_CLEARANCE
                || stamina.exhausted() || stamina.current() <= 0) return false;
        mode = Mode.GLIDE;
        stamina.traversal(GLIDE_DRAIN_PER_SECOND, true, frame);
        return true;
    }
    public boolean climbJump(int side, long frame) {
        if (mode != Mode.CLIMB || !stamina.consume(ADAPTED_CLIMB_JUMP_COST, frame)) return false;
        jumpSide = Integer.signum(side);
        jumpEnd = frame + ADAPTED_CLIMB_JUMP_FRAMES;
        return true;
    }
    public boolean jumping(long frame) { return mode == Mode.CLIMB && frame < jumpEnd; }
    public int jumpSide() { return jumpSide; }
    public void tick(boolean moving, boolean valid, long frame) {
        stamina.advanceTo(frame);
        if (active() && (!valid || stamina.current() == 0)) { detach(frame); return; }
        if (mode == Mode.CLIMB) stamina.traversal(moving || jumping(frame) ? ADAPTED_CLIMB_DRAIN_PER_SECOND : 0, true, frame);
    }
    public void detach(long frame) {
        if (!active()) return;
        mode = Mode.FREE;
        jumpEnd = 0;
        reattachReady = frame + ADAPTED_REATTACH_DELAY_FRAMES;
        stamina.traversal(0, false, frame);
    }
    /** Explicit Minecraft adaptation: linear max-HP loss after5m, capped at100% at25m equivalent. */
    public static double fallHpFraction(double height, double horizontalSpeed, boolean safeWater) {
        if (safeWater || height <= 0) return 0;
        double equivalentHeight = height + horizontalSpeed * horizontalSpeed / (2 * ADAPTED_HORIZONTAL_ENERGY_GRAVITY);
        return Math.clamp((equivalentHeight - ADAPTED_SAFE_FALL_BLOCKS)
                / (ADAPTED_LETHAL_FALL_BLOCKS - ADAPTED_SAFE_FALL_BLOCKS), 0, 1);
    }
}

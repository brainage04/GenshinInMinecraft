package io.github.brainage04.genshininminecraft.rules;

/** Ordinary starter stamina; time is 60 fps reference frames, with no Minecraft dependencies. */
public final class Stamina {
    // stamina.md: current wiki, not the conflicting historical KQM calculation.
    public static final double NEW_PLAYER_MAX = 100;
    public static final double STATUE_UPGRADED_CAP = 240;
    public static final double REGEN_PER_SECOND = 25;
    public static final long REGEN_DELAY_FRAMES = 90;
    public static final double SPRINT_START_COST = 18;
    public static final double SPRINT_DRAIN_PER_SECOND = 18;
    public static final double DASH_COST = 18;
    public static final double SWORD_CHARGED_COST = 20;
    // The current restart threshold and dash movement duration are unknown: named adaptations.
    public static final double ADAPTED_EXHAUSTION_RECOVERY_THRESHOLD = 15;
    public static final long ADAPTED_DASH_DURATION_FRAMES = 18;
    public static final double WIKI_DASH_IFRAME_STARTUP_MS = 40;
    public static final long DASH_IFRAME_STARTUP_FRAMES = 3; // ceil(40 ms / 50 ms) Minecraft tick.
    public static final long DASH_IFRAME_ACTIVE_FRAMES = 18; // wiki 300 ms = six ticks.
    private static final double DRAIN_PER_FRAME = SPRINT_DRAIN_PER_SECOND / 60;
    private static final double REGEN_PER_FRAME = REGEN_PER_SECOND / 60;

    private final double maximum;
    private double current;
    private long frame;
    private long regenReady;
    private long dashStart = -1;
    private long dashEnd;
    private boolean sprinting;
    private boolean exhausted;
    private double traversalDrainPerFrame;
    private boolean traversalAttached;

    public Stamina() { this(NEW_PLAYER_MAX); }
    public Stamina(double maximum) {
        if (!Double.isFinite(maximum) || maximum <= ADAPTED_EXHAUSTION_RECOVERY_THRESHOLD
                || maximum > STATUE_UPGRADED_CAP) throw new IllegalArgumentException("Invalid stamina capacity");
        this.maximum = maximum;
        current = maximum;
    }
    public double current() { return current; }
    public double maximum() { return maximum; }
    public long frame() { return frame; }
    public boolean exhausted() { return exhausted; }
    public boolean sprinting() { return sprinting; }
    public boolean draining() { return sprinting || frame < dashEnd || traversalDrainPerFrame > 0; }
    public boolean dashing(long at) { return dashStart >= 0 && at >= dashStart && at < dashEnd; }
    public boolean dashInvulnerable(long at) {
        return dashStart >= 0 && at >= dashStart + DASH_IFRAME_STARTUP_FRAMES
                && at < dashStart + DASH_IFRAME_STARTUP_FRAMES + DASH_IFRAME_ACTIVE_FRAMES;
    }

    public void advanceTo(long at) {
        if (at < frame) throw new IllegalArgumentException("Stamina time cannot move backwards");
        long from = frame;
        if (traversalAttached) {
            current = Math.max(0, current - (at - from) * traversalDrainPerFrame);
            regenReady = at + REGEN_DELAY_FRAMES;
            if (current == 0) exhausted = true;
            frame = at;
            return;
        }
        if (sprinting) {
            long start = Math.max(from, dashEnd);
            long drainingFrames = Math.max(0, at - start);
            if (drainingFrames > 0) {
                long untilEmpty = (long) Math.ceil(current / DRAIN_PER_FRAME);
                long spentFrames = Math.min(drainingFrames, untilEmpty);
                current = Math.max(0, current - spentFrames * DRAIN_PER_FRAME);
                regenReady = start + spentFrames + REGEN_DELAY_FRAMES;
                if (spentFrames == untilEmpty) {
                    current = 0;
                    exhausted = true;
                    sprinting = false;
                }
                from = start + spentFrames;
            }
        }
        if (!sprinting) {
            long recoveryFrames = Math.max(0, at - Math.max(from, regenReady));
            current = Math.min(maximum, current + recoveryFrames * REGEN_PER_FRAME);
            if (current > ADAPTED_EXHAUSTION_RECOVERY_THRESHOLD) exhausted = false;
        }
        frame = at;
    }

    /** Ordinary sprint starts and dashes share a start cost; never bill both for one press. */
    public boolean startSprint(long at) {
        advanceTo(at);
        if (sprinting) return true;
        if (!consume(SPRINT_START_COST, at)) return false;
        sprinting = !exhausted;
        return sprinting;
    }
    public void stopSprint(long at) {
        advanceTo(at);
        if (sprinting) regenReady = Math.max(at, dashEnd) + REGEN_DELAY_FRAMES;
        sprinting = false;
    }
    public boolean dash(long at) {
        advanceTo(at);
        if (dashing(at) || !consume(DASH_COST, at)) return false;
        dashStart = at;
        dashEnd = at + ADAPTED_DASH_DURATION_FRAMES;
        regenReady = dashEnd + REGEN_DELAY_FRAMES;
        sprinting = !exhausted;
        return true;
    }
    public boolean chargedAttack(long at) { return consume(SWORD_CHARGED_COST, at); }

    /** Wall attachment blocks recovery even while stationary; gliding always descends and drains. */
    public void traversal(double drainPerSecond, boolean attached, long at) {
        advanceTo(at);
        if (!Double.isFinite(drainPerSecond) || drainPerSecond < 0)
            throw new IllegalArgumentException("Invalid traversal drain");
        if (traversalAttached && !attached) regenReady = at + REGEN_DELAY_FRAMES;
        traversalAttached = attached;
        traversalDrainPerFrame = attached ? drainPerSecond / 60 : 0;
        if (attached) sprinting = false;
    }

    /** Discrete costs are all-or-nothing, and exhaustion locks every stamina-consuming action. */
    public boolean consume(double cost, long at) {
        if (!Double.isFinite(cost) || cost <= 0) throw new IllegalArgumentException("Invalid stamina cost");
        advanceTo(at);
        if (exhausted || current < cost) return false;
        current -= cost;
        regenReady = Math.max(at, dashEnd) + REGEN_DELAY_FRAMES;
        if (current == 0) {
            exhausted = true;
            sprinting = false;
        }
        return true;
    }
}

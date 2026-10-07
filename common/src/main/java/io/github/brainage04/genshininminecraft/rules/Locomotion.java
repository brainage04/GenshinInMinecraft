package io.github.brainage04.genshininminecraft.rules;

/** Presentation-only phases on the authoritative 60-fps clock; never changes movement or resources. */
public final class Locomotion {
    public enum Phase {
        IDLE("idle", 120, true), WALK("walk", 48, true), RUN("run", 30, true),
        DASH("dash", 18, false), JUMP("jump", 24, false), FALL("fall", 60, true),
        LAND_SOFT("land_soft", 15, false), LAND_HARD("land_hard", 30, false),
        CLIMB_IDLE("climb_idle", 120, true), CLIMB_UP("climb_up", 60, true),
        CLIMB_DOWN("climb_down", 60, true), CLIMB_LEFT("climb_left", 60, true),
        CLIMB_RIGHT("climb_right", 60, true), CLIMB_JUMP("climb_jump", 18, false),
        MANTLE("mantle", 24, false), GLIDE_START("glide_start", 18, false),
        GLIDE_LOOP("glide_loop", 90, true), GLIDE_STOP("glide_stop", 12, false), SWIM("swim", 60, true);

        private final String clip;
        private final int frames;
        private final boolean loop;
        Phase(String name, int frames, boolean loop) {
            this.clip = "locomotion." + name;
            this.frames = frames;
            this.loop = loop;
        }
        public String clip() { return clip; }
        public int frames() { return frames; }
        public boolean loop() { return loop; }
        public double seconds(double renderFrame, long startFrame) {
            double elapsed = Math.max(0, renderFrame - startFrame);
            return (loop ? elapsed % frames : Math.min(elapsed, frames)) / 60;
        }
    }

    private Phase phase = Phase.IDLE;
    private long startFrame;
    private int occurrence;
    private boolean airborne;
    private double fallHeight;
    private boolean gliding;

    public Phase phase() { return phase; }
    public long startFrame() { return startFrame; }
    public int occurrence() { return occurrence; }
    public void reset(long frame) {
        airborne = false;
        gliding = false;
        fallHeight = 0;
        begin(Phase.IDLE, frame);
    }
    public void begin(Phase next, long frame) {
        phase = next;
        startFrame = frame;
        occurrence++;
    }
    public void land(double height, long frame) {
        fallHeight = Math.max(fallHeight, height);
        if (height > .15) begin(height > 5 ? Phase.LAND_HARD : Phase.LAND_SOFT, frame);
        airborne = false;
    }
    public void update(long frame, boolean ground, double vertical, boolean moving, boolean sprinting,
            boolean dashing, boolean swimming, int traversalMode, boolean climbJump,
            int forward, int sideways, double height) {
        fallHeight = Math.max(fallHeight, height);
        Phase next;
        if (dashing) next = Phase.DASH;
        else if (traversalMode == 1) {
            next = climbJump ? Phase.CLIMB_JUMP : forward > 0 ? Phase.CLIMB_UP : forward < 0 ? Phase.CLIMB_DOWN
                    : sideways > 0 ? Phase.CLIMB_LEFT : sideways < 0 ? Phase.CLIMB_RIGHT : Phase.CLIMB_IDLE;
            airborne = false;
            fallHeight = 0;
        } else if (traversalMode == 2) {
            next = !gliding ? Phase.GLIDE_START : phase == Phase.GLIDE_START && frame - startFrame < Phase.GLIDE_START.frames
                    ? Phase.GLIDE_START : Phase.GLIDE_LOOP;
            airborne = false;
            fallHeight = 0;
        } else if (gliding && !ground) next = Phase.GLIDE_STOP;
        else if (swimming) next = Phase.SWIM;
        else if (ground) {
            if (airborne) land(fallHeight, frame);
            boolean landing = phase == Phase.LAND_HARD || phase == Phase.LAND_SOFT || phase == Phase.MANTLE;
            next = landing && frame - startFrame < phase.frames && (!moving || phase == Phase.MANTLE) ? phase
                    : moving ? sprinting ? Phase.RUN : Phase.WALK : Phase.IDLE;
            airborne = false;
            fallHeight = 0;
        } else {
            boolean closing = phase == Phase.GLIDE_STOP && frame - startFrame < Phase.GLIDE_STOP.frames;
            next = closing ? Phase.GLIDE_STOP : vertical > .01 ? Phase.JUMP : Phase.FALL;
            airborne = true;
        }
        gliding = traversalMode == 2;
        if (next != phase) begin(next, frame);
    }
    public static double renderFrame(long sampleFrame, long sampleGameTime, long gameTime, float partialTick) {
        return sampleFrame + 3 * (gameTime - sampleGameTime + (double) partialTick);
    }
    public Locomotion() {}
}

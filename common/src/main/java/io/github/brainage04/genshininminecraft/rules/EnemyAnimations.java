package io.github.brainage04.genshininminecraft.rules;

/** Existing server melee ticks and original cosmetic clip lengths; no animation grants damage. */
public final class EnemyAnimations {
    public static final int WINDUP_TICKS = 10;
    public static final int RECOVERY_TICKS = 30;
    public static final int STRIKE_PRESENTATION_TICKS = 3;
    public static final int HURT_FRAMES = 18;
    public static final int DEATH_FRAMES = 60;
    public static final int BUNNY_LAND_FRAMES = 15;
    public static final int BUNNY_EXPLODE_FRAMES = 24;
    public enum Phase {
        IDLE("idle", 120, true), WALK("walk", 48, true), RUN("run", 30, true),
        TELEGRAPH("telegraph", WINDUP_TICKS * 3, false),
        STRIKE("strike", STRIKE_PRESENTATION_TICKS * 3, false),
        RECOVERY("recovery", (RECOVERY_TICKS - STRIKE_PRESENTATION_TICKS) * 3, false),
        DEATH("death", DEATH_FRAMES, false), LAND("land", BUNNY_LAND_FRAMES, false),
        THROWN("thrown", 40, false), EXPLODE("explode", BUNNY_EXPLODE_FRAMES, false), HURT("hurt", HURT_FRAMES, false);
        private static final Phase[] VALUES = values();
        private final String clip;
        private final int frames;
        private final boolean loop;
        Phase(String name, int frames, boolean loop) { clip = "enemy." + name; this.frames = frames; this.loop = loop; }
        public static Phase fromId(int id) { return VALUES[id]; }
        public String clip() { return clip; }
        public int frames() { return frames; }
        public boolean loop() { return loop; }
        public double seconds(double elapsed) {
            return loop ? Math.max(0, elapsed) % (frames / 60.0) : Math.clamp(elapsed, 0, frames / 60.0 - .000001);
        }
    }
    private EnemyAnimations() {}
}

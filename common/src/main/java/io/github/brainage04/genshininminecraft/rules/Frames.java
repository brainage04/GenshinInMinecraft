package io.github.brainage04.genshininminecraft.rules;

/** Genshin reference time: 60 frames/s; a 20 TPS server tick is exactly three frames. */
public final class Frames {
    public static final long PER_SECOND = 60;
    public static final long PER_SERVER_TICK = 3;

    private Frames() {}

    public static long atServerTick(long tick) {
        if (tick < 0) throw new IllegalArgumentException("Negative tick");
        return Math.multiplyExact(tick, PER_SERVER_TICK);
    }

    public static double seconds(long frames) {
        return frames / (double) PER_SECOND;
    }
}

package io.github.brainage04.genshininminecraft.rules;

/** Per-enemy Violet Arc marks, separate from Electro aura/application ICD. */
public final class ConductiveState {
    public static final int MAX_STACKS = 3;
    public static final int STACK_DURATION_FRAMES = 15 * 60;
    private final long[] expires = new long[MAX_STACKS];
    private int count;
    private long frame;

    public void advanceTo(long at) {
        if (at < frame) throw new IllegalArgumentException("Conductive time cannot move backwards");
        frame = at;
        int kept = 0;
        for (int index = 0; index < count; index++) if (expires[index] > at) expires[kept++] = expires[index];
        count = kept;
    }
    public int stacks() { return count; }
    public void add(long at) {
        advanceTo(at);
        // At cap, replace the oldest mark; precise cap-refresh ordering is unsourced.
        if (count == MAX_STACKS) {
            System.arraycopy(expires, 1, expires, 0, --count);
        }
        expires[count++] = at + STACK_DURATION_FRAMES;
    }
    public int consume(long at) {
        advanceTo(at);
        int consumed = count;
        count = 0;
        return consumed;
    }
}

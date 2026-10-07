package io.github.brainage04.genshininminecraft.rules;

/** coop.md steady-state overworld allocations; join-order host and live rescaling are adaptations. */
public final class Coop {
    public static final int MAX_PLAYERS = 4;
    public static final int FULL_ROSTER = (1 << Party.SIZE) - 1;
    public static final double ADAPTED_ENERGY_RADIUS = 32;
    private Coop() {}
    public static int capacity(int players, int participant) {
        if (players < 1 || players > MAX_PLAYERS || participant < 0 || participant >= players)
            throw new IllegalArgumentException("Invalid co-op membership");
        return players == 1 ? 4 : players == 2 || players == 3 && participant == 0 ? 2 : 1;
    }
    public static double enemyHpMultiplier(int players) {
        if (players < 1 || players > MAX_PLAYERS) throw new IllegalArgumentException("Invalid co-op count");
        return 1 + .5 * (players - 1);
    }
    public static int select(int preferred, int capacity) {
        int mask = 0;
        for (int slot = 0; slot < Party.SIZE && Integer.bitCount(mask) < capacity; slot++)
            if ((preferred & 1 << slot) != 0) mask |= 1 << slot;
        for (int slot = 0; slot < Party.SIZE && Integer.bitCount(mask) < capacity; slot++) mask |= 1 << slot;
        return mask;
    }
}

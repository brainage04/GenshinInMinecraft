package io.github.brainage04.genshininminecraft.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Damage suppression never suppresses a reaction's gauge consumption or stagger. */
public final class ReactionDamageIcd {
    // spec/mechanics/elements.md: Reaction damage ICD table. EC cooldown is separately
    // parameterized in AuraState.Parameters because its exact ordering/timing is unresolved.
    public static final long WINDOW_FRAMES = 30;
    public enum ShatterScope { TARGET, OWNER_AND_TARGET }
    private record Key(UUID owner, UUID target, Reaction.Type type, Element element) {}
    private static final class Hits {
        final long[] frames = new long[2];
        int count;
        long lastAttempt;
    }

    private final Map<Key, Hits> hits = new HashMap<>();
    private final ShatterScope shatterScope;

    /** Owner scope is unknown for Shatter; the documented baseline is target-wide. */
    public ReactionDamageIcd() { this(ShatterScope.TARGET); }
    public ReactionDamageIcd(ShatterScope shatterScope) { this.shatterScope = shatterScope; }

    public boolean allowsDamage(UUID owner, UUID target, Reaction.Type type,
            Element swirledElement, long frame) {
        if (frame < 0) throw new IllegalArgumentException("Negative frame");
        int limit = switch (type) {
            case OVERLOADED -> 1;
            case SUPERCONDUCT, SWIRL, SHATTER -> 2;
            default -> throw new IllegalArgumentException("No sourced damage ICD for this reaction");
        };
        UUID scopedOwner = type == Reaction.Type.SHATTER && shatterScope == ShatterScope.TARGET
                ? null : owner;
        Key key = new Key(scopedOwner, target, type, type == Reaction.Type.SWIRL ? swirledElement : null);
        Hits window = hits.computeIfAbsent(key, ignored -> new Hits());
        if (frame < window.lastAttempt) throw new IllegalArgumentException("Time cannot run backwards");
        window.lastAttempt = frame;
        int remaining = 0;
        for (int i = 0; i < window.count; i++) {
            if (frame - window.frames[i] < WINDOW_FRAMES) window.frames[remaining++] = window.frames[i];
        }
        window.count = remaining;
        if (remaining == limit) return false;
        window.frames[window.count++] = frame;
        return true;
    }

    public void forget(UUID entity) {
        hits.keySet().removeIf(key -> entity.equals(key.owner) || entity.equals(key.target));
    }
}

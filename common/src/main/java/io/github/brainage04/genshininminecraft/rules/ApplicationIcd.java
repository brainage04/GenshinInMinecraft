package io.github.brainage04.genshininminecraft.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Application only: blocked hits still deal talent damage. Single-threaded server-owned state. */
public final class ApplicationIcd {
    public enum Rule {
        STANDARD(150), AMBER(60);
        private final long resetFrames;
        Rule(long resetFrames) { this.resetFrames = resetFrames; }
    }

    private record Key(UUID attacker, String tag, UUID target, Rule rule) {}
    private static final class Window {
        long start;
        long lastHit;
        int hits;
        Window(long frame) { start = frame; lastHit = frame; hits = 1; }
    }
    private final Map<Key, Window> windows = new HashMap<>();

    public boolean allowsApplication(UUID attacker, String tag, UUID target, long frame) {
        return allowsApplication(attacker, tag, target, Rule.STANDARD, frame);
    }

    /**
     * spec/mechanics/elements.md: Standard applying indices 1,4,...,22; finite length 24.
     * Timer-eligible applications reset the window; hit-count applications do NOT reset it.
     * Null tag means no ICD, distinct from a source having zero gauge.
     */
    public boolean allowsApplication(UUID attacker, String tag, UUID target, Rule rule, long frame) {
        if (frame < 0) throw new IllegalArgumentException("Negative frame");
        if (tag == null) return true;
        Key key = new Key(attacker, tag, target, rule);
        Window window = windows.get(key);
        if (window == null) {
            windows.put(key, new Window(frame));
            return true;
        }
        if (frame < window.lastHit) throw new IllegalArgumentException("Time cannot run backwards");
        window.lastHit = frame;
        if (frame - window.start >= rule.resetFrames) {
            window.start = frame;
            window.hits = 1;
            return true;
        }
        window.hits = Math.min(window.hits + 1, 25);
        return window.hits <= 24 && (window.hits - 1) % 3 == 0;
    }

    /** Release all attacker and target references when an entity leaves the combat simulation. */
    public void forget(UUID entity) {
        windows.keySet().removeIf(key -> key.attacker.equals(entity) || key.target.equals(entity));
    }
}

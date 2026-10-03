package io.github.brainage04.genshininminecraft.rules;

import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.LongConsumer;

/**
 * Single-threaded, server-owned min-heap ordered by source frame then insertion sequence.
 * Drain with advanceTo(Frames.atServerTick(tick)); frame 23 executes during tick 8's drain,
 * after frame 22 and before frame 24, with its original timestamp passed to the callback.
 */
public final class EventTimeline {
    private record Event(long frame, long sequence, LongConsumer action)
            implements Comparable<Event> {
        @Override
        public int compareTo(Event other) {
            int order = Long.compare(frame, other.frame);
            return order == 0 ? Long.compare(sequence, other.sequence) : order;
        }
    }

    private final PriorityQueue<Event> events = new PriorityQueue<>();
    private long frame;
    private long sequence;
    private boolean advancing;

    public long frame() {
        return frame;
    }

    public int pendingEvents() {
        return events.size();
    }

    /** Callbacks may enqueue additional events, including at their current frame. */
    public void schedule(long atFrame, LongConsumer action) {
        if (atFrame < frame) throw new IllegalArgumentException("Event is in the past");
        Objects.requireNonNull(action, "action");
        long next = Math.incrementExact(sequence);
        events.add(new Event(atFrame, sequence, action));
        sequence = next;
    }

    public void advanceTo(long toFrame) {
        if (toFrame < frame) throw new IllegalArgumentException("Time cannot run backwards");
        if (advancing) throw new IllegalStateException("Reentrant timeline drain");
        advancing = true;
        try {
            while (!events.isEmpty() && events.peek().frame <= toFrame) {
                Event event = events.remove();
                frame = event.frame;
                event.action.accept(frame);
            }
            frame = toFrame;
        } finally {
            advancing = false;
        }
    }
}

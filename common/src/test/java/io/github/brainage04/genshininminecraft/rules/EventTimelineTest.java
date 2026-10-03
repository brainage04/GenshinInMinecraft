package io.github.brainage04.genshininminecraft.rules;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventTimelineTest {
    @Test
    void subTickFramesAndEqualFramesStayOrderedAcrossDrains() {
        // docs/GENSHIN_MINECRAFT_BRIEF.md: Hard problem 5 (60fps reference vs 20TPS).
        EventTimeline timeline = new EventTimeline();
        List<String> calls = new ArrayList<>();
        timeline.schedule(24, frame -> calls.add("last@" + frame));
        timeline.schedule(23, frame -> calls.add("first@" + frame));
        timeline.schedule(22, frame -> calls.add("early@" + frame));
        timeline.schedule(23, frame -> calls.add("second@" + frame));
        timeline.advanceTo(Frames.atServerTick(7));
        assertEquals(List.of(), calls);
        timeline.advanceTo(22);
        assertEquals(List.of("early@22"), calls);
        timeline.advanceTo(Frames.atServerTick(8));
        assertEquals(List.of("early@22", "first@23", "second@23", "last@24"), calls);
        assertEquals(24, timeline.frame());
        assertEquals(0, timeline.pendingEvents());
    }

    @Test
    void callbackCanScheduleAtSameFrameWithoutOvertakingExistingEvents() {
        EventTimeline timeline = new EventTimeline();
        List<Long> calls = new ArrayList<>();
        timeline.schedule(23, frame -> {
            calls.add(frame);
            timeline.schedule(frame, nested -> calls.add(nested + 200));
            timeline.schedule(24, nested -> calls.add(nested));
        });
        timeline.schedule(23, frame -> calls.add(frame + 100));
        timeline.advanceTo(24);
        assertEquals(List.of(23L, 123L, 223L, 24L), calls);
    }

    @Test
    void noTimeReversalOrReentrantDrains() {
        EventTimeline timeline = new EventTimeline();
        timeline.schedule(3, frame -> assertThrows(IllegalStateException.class, () -> timeline.advanceTo(6)));
        timeline.advanceTo(3);
        assertThrows(IllegalArgumentException.class, () -> timeline.advanceTo(2));
        assertThrows(IllegalArgumentException.class, () -> timeline.schedule(2, frame -> {}));
        assertEquals(60, Frames.atServerTick(20));
    }
}

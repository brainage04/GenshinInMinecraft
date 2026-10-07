package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.kit.*;
import java.util.List;
import java.util.function.BiConsumer;

/** Starter solo roster and its one shared stamina pool; the server adapter owns this party. */
public final class Party {
    public static final int SIZE = 4;
    public static final long SWITCH_COOLDOWN_FRAMES = 60; // party.md: one second.
    private final EventTimeline timeline;
    private final Stamina stamina = new Stamina();
    private final CharacterKit[] kits = new CharacterKit[SIZE];
    private final List<CharacterState> members;
    private int active;
    private long switchReady;

    public Party(EventTimeline timeline, BiConsumer<CharacterKit, CharacterKit.Hit> hits) {
        this.timeline = timeline;
        kits[0] = new TravelerAnemoKit(timeline, stamina, hit -> hits.accept(kits[0], hit));
        kits[1] = new AmberKit(timeline, stamina, hit -> hits.accept(kits[1], hit));
        kits[2] = new KaeyaKit(timeline, stamina, hit -> hits.accept(kits[2], hit));
        kits[3] = new LisaKit(timeline, stamina, hit -> hits.accept(kits[3], hit));
        members = List.of(kits[0].state(), kits[1].state(), kits[2].state(), kits[3].state());
    }
    public List<CharacterState> members() { return members; }
    public CharacterKit kit(int slot) { return kits[slot]; }
    public CharacterKit activeKit() { return kits[active]; }
    public CharacterState activeMember() { return activeKit().state(); }
    public int activeSlot() { return active; }
    public Stamina stamina() { return stamina; }
    public long frame() { return timeline.frame(); }
    public long switchReadyFrame() { return switchReady; }
    public void advanceTo(long frame) { timeline.advanceTo(frame); stamina.advanceTo(frame); }
    public boolean switchTo(int slot, long frame) {
        advanceTo(frame);
        if (slot < 0 || slot >= SIZE || slot == active || !members.get(slot).alive()
                || !activeMember().alive() || frame < switchReady || !activeKit().canSwitch(frame)) return false;
        activate(slot, frame);
        return true;
    }
    /** Adaptation: immediate cyclic next-alive replacement, bypassing manual cooldown/action lock. */
    public boolean forceSwitch(long frame) {
        if (activeMember().alive()) return false;
        for (int offset = 1; offset < SIZE; offset++) {
            int next = (active + offset) % SIZE;
            if (members.get(next).alive()) { activate(next, frame); return true; }
        }
        activeKit().leaveField(frame);
        return false;
    }
    private void activate(int slot, long frame) {
        activeMember().switchTo(members.get(slot), frame);
        activeKit().leaveField(frame);
        active = slot;
        switchReady = frame + SWITCH_COOLDOWN_FRAMES;
    }
    public void collect(Energy.Item item, Element element, int count) {
        if (count < 0) throw new IllegalArgumentException("Negative item count");
        for (int index = 0; index < SIZE; index++) {
            var member = members.get(index);
            member.grantEnergy(count * Energy.received(item, element, member.element(), index == active,
                    SIZE, member.energyRecharge()));
        }
    }
}

package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import java.util.function.Consumer;

/** Ceremonial Bladework, talent 1; Frostgnaw/Glacial Waltz are deliberately unavailable. */
public final class KaeyaKit extends NormalAttackKit {
    public static final double BURST_COST = 60;
    public static final long CHARGED_SWITCH_FRAME = 34;
    private static final double[] MULTIPLIERS = {.5375, .5169, .6527, .7086, .8824};
    private static final int[] HIT_FRAMES = {14, 9, 14, 23, 30};
    private static final int[] RECOVERY_FRAMES = {27, 27, 47, 46, 74};
    private static final int[] CHARGE_FRAMES = {36, 31, 55, 54};
    public KaeyaKit(Consumer<Hit> hits) { this(new EventTimeline(), new Stamina(), hits); }
    public KaeyaKit(EventTimeline timeline, Stamina stamina, Consumer<Hit> hits) {
        super(CharacterBaseStats.Character.KAEYA, Weapon.SWORD, CharacterState.TRAINING_SWORD_BASE_ATK,
                BURST_COST, timeline, stamina, hits);
    }
    @Override protected void normal(long frame, int index) {
        combo(frame, index, 5, RECOVERY_FRAMES[index]);
        hit(frame + HIT_FRAMES[index], Kind.NORMAL, MULTIPLIERS[index], Element.PHYSICAL, 0, null, frame);
        if (index < 4) charge(frame + CHARGE_FRAMES[index], Stamina.SWORD_CHARGED_COST, 54,
                (int) CHARGED_SWITCH_FRAME, at -> {
                    // Original sheet: 16-frame first hit; second hit has zero offset, not frame zero.
                    hit(at + 16, Kind.CHARGED, .5504, Element.PHYSICAL, 0, null, frame);
                    hit(at + 16, Kind.CHARGED, .731, Element.PHYSICAL, 0, null, frame);
                });
    }
}

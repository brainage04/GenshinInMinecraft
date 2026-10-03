package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import java.util.function.Consumer;

/** Lightning Touch, talent 1; no ascension Conductive stacks or unavailable E/Q talents. */
public final class LisaKit extends NormalAttackKit {
    public static final double BURST_COST = 80;
    public static final double CHARGED_STAMINA_COST = 50;
    public static final int ADAPTED_CHARGED_HIT_FRAMES = 58; // First 72-frame trial minus reported ~14-frame skipped windup.
    public static final int ADAPTED_CHARGED_RECOVERY_FRAMES = 77;
    public static final int ADAPTED_CHARGED_SWITCH_FRAMES = 76;
    public static final String NORMAL_ICD_TAG = "Lisa Electro DMG";
    private static final double[] MULTIPLIERS = {.396, .3592, .428, .5496};
    private static final int[] HIT_FRAMES = {26, 17, 17, 31};
    private static final int[] RECOVERY_FRAMES = {30, 20, 34, 57};
    private static final int[] CHARGE_FRAMES = {31, 24, 40};
    public LisaKit(Consumer<Hit> hits) { this(new EventTimeline(), new Stamina(), hits); }
    public LisaKit(EventTimeline timeline, Stamina stamina, Consumer<Hit> hits) {
        super(CharacterBaseStats.Character.LISA, Weapon.CATALYST, CharacterState.TRAINING_CATALYST_BASE_ATK,
                BURST_COST, timeline, stamina, hits);
    }
    @Override protected void normal(long frame, int index) {
        combo(frame, index, 4, RECOVERY_FRAMES[index]);
        hit(frame + HIT_FRAMES[index], Kind.NORMAL, MULTIPLIERS[index], Element.ELECTRO, 1, NORMAL_ICD_TAG, frame);
        if (index < 3) charge(frame + CHARGE_FRAMES[index], CHARGED_STAMINA_COST,
                ADAPTED_CHARGED_RECOVERY_FRAMES, ADAPTED_CHARGED_SWITCH_FRAMES,
                at -> hit(at + ADAPTED_CHARGED_HIT_FRAMES, Kind.CHARGED, 1.7712, Element.ELECTRO, 1, null, frame));
    }
}

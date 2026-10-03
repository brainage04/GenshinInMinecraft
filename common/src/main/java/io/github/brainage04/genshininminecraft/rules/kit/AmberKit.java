package io.github.brainage04.genshininminecraft.rules.kit;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import java.util.function.Consumer;

/** Sharpshooter Physical arrows only; aimed shots and talents belong to item 9c. */
public final class AmberKit extends NormalAttackKit {
    public static final double BURST_COST = 40;
    private static final double[] MULTIPLIERS = {.3612, .3612, .4644, .4730, .5934};
    private static final int[] RELEASE_FRAMES = {14, 10, 27, 26, 26};
    private static final int[] RECOVERY_FRAMES = {26, 22, 37, 34, 60};
    public AmberKit(Consumer<Hit> hits) { this(new EventTimeline(), new Stamina(), hits); }
    public AmberKit(EventTimeline timeline, Stamina stamina, Consumer<Hit> hits) {
        super(CharacterBaseStats.Character.AMBER, Weapon.BOW, CharacterState.TRAINING_BOW_BASE_ATK,
                BURST_COST, timeline, stamina, hits);
    }
    @Override protected void normal(long frame, int index) {
        combo(frame, index, 5, RECOVERY_FRAMES[index]);
        hit(frame + RELEASE_FRAMES[index], Kind.NORMAL, MULTIPLIERS[index], Element.PHYSICAL, 0, null, frame);
    }
}

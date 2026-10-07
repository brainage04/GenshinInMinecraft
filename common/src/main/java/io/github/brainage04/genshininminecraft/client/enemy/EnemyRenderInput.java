package io.github.brainage04.genshininminecraft.client.enemy;

import com.geckolib.constant.dataticket.DataTicket;
import io.github.brainage04.genshininminecraft.rules.EnemyAnimations.Phase;

/** Captured server phase and world-clock seek, including a stopped clock for Frozen. */
public record EnemyRenderInput(Phase phase, int occurrence, double seconds, int hurtOccurrence, double hurtSeconds) {
    public static final DataTicket<EnemyRenderInput> TICKET = DataTicket.create("genshin_enemy_input", EnemyRenderInput.class);
}

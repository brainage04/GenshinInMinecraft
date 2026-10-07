package io.github.brainage04.genshininminecraft.client.character;

import io.github.brainage04.genshininminecraft.rules.Locomotion;
import io.github.brainage04.genshininminecraft.rules.CombatVisual;

/** Extracted values only: no entity, world or live session survives into deferred submission. */
public record CharacterRenderInput(long viewId, int slot, Locomotion.Phase phase, int occurrence,
        double elapsedSeconds, double age, float bodyYaw, float headYaw, float pitch, float scale,
        int light, int overlay, boolean invisible, boolean invisibleToPlayer, int outlineColor,
        float deathTime, boolean arms, CombatVisual.Action action, int actionOccurrence, double actionSeconds,
        int hurtOccurrence, double hurtSeconds) {}

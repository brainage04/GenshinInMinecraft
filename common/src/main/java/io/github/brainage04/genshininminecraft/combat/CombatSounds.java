package io.github.brainage04.genshininminecraft.combat;

import io.github.brainage04.genshininminecraft.rules.CombatAudio;
import io.github.brainage04.genshininminecraft.rules.CombatAudio.Cue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Minecraft playback adapter for the single, plain-Java vanilla mapping table. */
public final class CombatSounds {
    private static final SoundEvent[] EVENTS;
    static {
        Cue[] cues = Cue.values();
        EVENTS = new SoundEvent[cues.length];
        for (Cue cue : cues) {
            var id = Identifier.withDefaultNamespace(cue.event);
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(id)) throw new IllegalStateException("Missing vanilla sound " + id);
            EVENTS[cue.ordinal()] = BuiltInRegistries.SOUND_EVENT.getValue(id);
        }
    }
    private CombatSounds() {}
    public static SoundEvent event(Cue cue) { return EVENTS[cue.ordinal()]; }
    public static void play(Entity entity, Cue cue) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), event(cue), entity.getSoundSource(), cue.volume, cue.pitch);
    }
    public static void play(ServerLevel level, Vec3 position, Cue cue, long frame, CombatAudio.Budget budget) {
        if (budget.allow(cue, frame, level.getGameTime()))
            level.playSound(null, position.x, position.y, position.z, event(cue), SoundSource.PLAYERS, cue.volume, cue.pitch);
    }
    public static void landing(Entity entity, double height) {
        Cue cue = Cue.LANDING;
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), event(cue), SoundSource.PLAYERS,
                (float) Math.clamp(.2 + height / 15, .2, 1), cue.pitch);
    }
}

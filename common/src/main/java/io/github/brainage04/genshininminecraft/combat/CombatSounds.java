package io.github.brainage04.genshininminecraft.combat;

import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Reaction;
import java.util.Arrays;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Vanilla-only cues at authoritative transitions; a session-wide budget coalesces AoE spam. */
public final class CombatSounds {
    public enum Cue { PHYSICAL, PYRO, CRYO, ELECTRO, ANEMO, HYDRO,
        OVERLOADED, SUPERCONDUCT, MELT, VAPORIZE, SWIRL, ELECTRO_CHARGED, FROZEN, SHATTER, CRIT, ROSE }
    public static final class Budget {
        private final long[] last = new long[Cue.values().length];
        public Budget() { Arrays.fill(last, Long.MIN_VALUE); }
        public boolean allow(Cue cue, long frame) {
            int interval = cue == Cue.ELECTRO_CHARGED ? 30 : cue.ordinal() <= Cue.HYDRO.ordinal() ? 6 : 12;
            int index = cue.ordinal();
            if (last[index] != Long.MIN_VALUE && frame - last[index] < interval) return false;
            last[index] = frame;
            return true;
        }
        public void play(ServerLevel level, Vec3 position, Cue cue, long frame) {
            if (!allow(cue, frame)) return;
            level.playSound(null, position.x, position.y, position.z, event(cue), SoundSource.PLAYERS,
                    cue == Cue.OVERLOADED ? .55F : cue == Cue.ROSE || cue == Cue.ELECTRO_CHARGED ? .25F : .35F,
                    cue == Cue.CRIT ? 1.5F : cue == Cue.PHYSICAL ? .9F : 1.2F);
        }
    }
    private CombatSounds() {}
    public static Cue element(Element element) {
        return switch (element) {
            case PHYSICAL -> Cue.PHYSICAL; case PYRO -> Cue.PYRO; case CRYO -> Cue.CRYO;
            case ELECTRO -> Cue.ELECTRO; case ANEMO -> Cue.ANEMO; case HYDRO -> Cue.HYDRO;
        };
    }
    public static Cue reaction(Reaction.Type type) {
        return switch (type) {
            case OVERLOADED -> Cue.OVERLOADED; case SUPERCONDUCT -> Cue.SUPERCONDUCT;
            case MELT -> Cue.MELT; case VAPORIZE -> Cue.VAPORIZE; case SWIRL -> Cue.SWIRL;
            case ELECTRO_CHARGED -> Cue.ELECTRO_CHARGED; case FROZEN -> Cue.FROZEN; case SHATTER -> Cue.SHATTER;
        };
    }
    public static SoundEvent event(Cue cue) {
        return switch (cue) {
            case PHYSICAL -> SoundEvents.PLAYER_ATTACK_STRONG;
            case PYRO -> SoundEvents.FIRE_AMBIENT;
            case CRYO, FROZEN -> SoundEvents.GLASS_HIT;
            case ELECTRO, ELECTRO_CHARGED, ROSE -> SoundEvents.BREEZE_SHOOT;
            case ANEMO, SWIRL -> SoundEvents.BREEZE_SLIDE;
            case HYDRO, MELT, VAPORIZE -> SoundEvents.FIRE_EXTINGUISH;
            case OVERLOADED -> SoundEvents.GENERIC_EXPLODE.value();
            case SUPERCONDUCT -> SoundEvents.AMETHYST_BLOCK_BREAK;
            case SHATTER -> SoundEvents.GLASS_BREAK;
            case CRIT -> SoundEvents.PLAYER_ATTACK_CRIT;
        };
    }
}

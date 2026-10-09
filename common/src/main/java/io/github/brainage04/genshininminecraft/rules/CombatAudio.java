package io.github.brainage04.genshininminecraft.rules;

import java.util.Arrays;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;

/** Vanilla event IDs and presentation policy, independent of either loader or Minecraft bootstrap. */
public final class CombatAudio {
    public enum Cue {
        PHYSICAL("entity.player.attack.strong", .35F, .9F, 6),
        ARROW_HIT("entity.arrow.hit", .35F, 1.2F, 6),
        PYRO("item.firecharge.use", .15F, 1.3F, 6),
        CRYO("entity.player.hurt_freeze", .6F, 1.3F, 6),
        ELECTRO("block.sculk_sensor.clicking", .4F, 1.6F, 6),
        ANEMO("entity.breeze.deflect", .3F, 1.2F, 6),
        HYDRO("block.fire.extinguish", .35F, 1.2F, 6),
        OVERLOADED("entity.generic.explode", .55F, 1.2F, 12),
        SUPERCONDUCT("block.amethyst_block.break", .7F, 1.2F, 12),
        MELT("block.fire.extinguish", .6F, 1.2F, 12),
        VAPORIZE("block.fire.extinguish", .6F, 1.2F, 12),
        SWIRL("item.trident.riptide_1", .45F, 1.4F, 12),
        ELECTRO_CHARGED("block.sculk_sensor.clicking", .4F, 1.6F, 30),
        FROZEN("block.amethyst_cluster.break", 1F, .8F, 12),
        SHATTER("block.glass.break", .35F, 1.2F, 12),
        CRIT("entity.player.attack.crit", .35F, 1.5F, 12),
        ROSE("block.sculk_sensor.clicking", .4F, 1.6F, 12),
        SWORD_NORMAL("entity.player.attack.sweep", .65F, 1.1F),
        SWORD_CHARGED("entity.player.attack.sweep", .65F, .9F),
        BOW_NORMAL("entity.arrow.shoot", .65F, 1.1F),
        BOW_CHARGED("entity.arrow.shoot", .65F, .9F),
        CATALYST_LAUNCH("block.amethyst_cluster.place", 1F, 1.3F),
        SKILL_CAST("entity.evoker.cast_spell", .6F, 1.1F),
        FROSTGNAW_CAST("entity.player.attack.sweep", .4F, 1.4F),
        FROSTGNAW_HIT("block.glass.break", .65F, 1.1F),
        BUNNY_THROW("entity.witch.throw", .5F, 1.1F),
        BURST_CAST("entity.illusioner.mirror_move", .6F, 1F),
        STORM_RELEASE("entity.breeze.wind_burst", .65F, 1.1F),
        FIELD_CAST("entity.evoker.cast_spell", .65F, 1.1F),
        ENERGY_PICKUP("entity.experience_orb.pickup", .35F, 1.25F),
        BURST_READY("block.note_block.chime", .8F, 1.5F),
        SKILL_READY("ui.button.click", .18F, 1.4F),
        SWITCH("item.armor.equip_leather", .4F, 1.3F),
        FALLEN("block.beacon.deactivate", .5F, 1.2F),
        REJECTED_BURST("ui.button.click", .25F, .7F),
        DASH("entity.breeze.deflect", .3F, 1.5F),
        GLIDER_OPEN("entity.ender_dragon.flap", .25F, 1.5F),
        GLIDER_CLOSE("item.armor.equip_leather", .5F, .8F),
        CLIMB("block.stone.hit", .4F, 1.25F),
        LANDING("block.stone.fall", 1F, .8F),
        ENEMY_AGGRO("entity.piglin.angry", .45F, .95F),
        ENEMY_TELEGRAPH("entity.piglin.angry", .65F, .75F),
        ENEMY_SWING("entity.player.attack.sweep", .7F, .7F),
        ENEMY_IMPACT("entity.player.attack.strong", .8F, .65F),
        ENEMY_HURT("entity.piglin.hurt", .45F, .8F),
        ENEMY_DEATH("entity.piglin.death", .8F, .8F),
        BUNNY_IDLE("entity.rabbit.attack", .4F, 1.2F),
        BUNNY_LAND("block.wool.fall", .8F, .85F),
        BUNNY_EXPLODE("entity.generic.explode", .8F, 1.25F);

        public final String event;
        public final float volume;
        public final float pitch;
        public final int intervalFrames;
        Cue(String event, float volume, float pitch) { this(event, volume, pitch, 0); }
        Cue(String event, float volume, float pitch, int intervalFrames) {
            this.event = event; this.volume = volume; this.pitch = pitch; this.intervalFrames = intervalFrames;
        }
    }
    private static final Cue[] CUES = Cue.values();
    private static final int[] EVENT_GROUP = new int[CUES.length];
    static {
        for (Cue cue : CUES) {
            EVENT_GROUP[cue.ordinal()] = cue.ordinal();
            for (Cue other : CUES) if (other.event.equals(cue.event)) {
                EVENT_GROUP[cue.ordinal()] = other.ordinal();
                break;
            }
        }
    }
    private CombatAudio() {}
    public static Cue element(Element element, boolean projectile, boolean reacting) {
        if (reacting) return null; // The reaction replaces, rather than layers over, its element hit.
        return switch (element) {
            case PHYSICAL -> projectile ? Cue.ARROW_HIT : Cue.PHYSICAL;
            case PYRO -> Cue.PYRO; case CRYO -> Cue.CRYO; case ELECTRO -> Cue.ELECTRO;
            case ANEMO -> Cue.ANEMO; case HYDRO -> Cue.HYDRO;
        };
    }
    public static Cue reaction(Reaction.Type type) { return Cue.valueOf(type.name()); }
    public static boolean landing(double height, boolean water, boolean vanillaFallSound) {
        return height > .15 && !water && !vanillaFallSound;
    }
    public static boolean playerHurt(boolean managed, boolean fall) { return !managed || !fall; }
    public static final class Budget {
        private final long[] last = new long[CUES.length];
        private final long[] eventTick = new long[CUES.length];
        private final boolean[] burstReady = new boolean[Party.SIZE];
        private long lastRejection = Long.MIN_VALUE;
        private long lastBurstReadyTick = Long.MIN_VALUE;
        public Budget() { Arrays.fill(last, Long.MIN_VALUE); Arrays.fill(eventTick, Long.MIN_VALUE); }
        public boolean allow(Cue cue, long frame, long tick) {
            int index = cue.ordinal();
            if (last[index] != Long.MIN_VALUE && frame - last[index] < cue.intervalFrames) return false;
            // Different gameplay kinds can resolve the same vanilla event (Rose, hit, EC).
            int group = EVENT_GROUP[index];
            if (eventTick[group] == tick) return false;
            last[index] = frame;
            eventTick[group] = tick;
            return true;
        }
        public boolean rejected(Intent intent, long frame) {
            if (intent != Intent.BURST_PRESS || lastRejection != Long.MIN_VALUE && frame - lastRejection < 60) return false;
            lastRejection = frame;
            return true;
        }
        public boolean burstReady(int slot, int activeSlot, boolean ready, long tick) {
            boolean play = ready && !burstReady[slot] && slot == activeSlot && lastBurstReadyTick != tick;
            if (play) lastBurstReadyTick = tick;
            burstReady[slot] = ready;
            return play;
        }
    }
    /** One instance per server; each enemy retains its own last successful broadcast tick. */
    public static final class EnemyHurtBudget {
        private long lastTick = Long.MIN_VALUE;
        public boolean allow(long lastEnemyTick, long tick) {
            if (lastTick == tick || lastEnemyTick != Long.MIN_VALUE && tick - lastEnemyTick < 10) return false;
            lastTick = tick;
            return true;
        }
    }
}

package io.github.brainage04.genshininminecraft.rules;

import io.github.brainage04.genshininminecraft.rules.kit.CharacterState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Durable resources only. All durations are remaining 60-fps frames, never session timestamps. */
public record PartySave(int version, int activeSlot, List<Member> members, double stamina,
                        int staminaRegenRemaining, boolean staminaExhausted, int switchRemaining) {
    public static final int CURRENT_VERSION = 1;
    // KQM overworld health/revive evidence (v2.2); waypoint placement is a Minecraft adaptation.
    public static final double WIPE_REVIVE_HP_FRACTION = .35;
    private static final CharacterBaseStats.Character[] ROSTER = CharacterBaseStats.Character.values();

    public record Member(double hp, double energy, int skillRemaining, int burstRemaining,
                         int skillCooldownFrames, int burstCooldownFrames, boolean fallen) {
        public Member {
            if (!Double.isFinite(hp) || !Double.isFinite(energy) || skillRemaining < 0 || burstRemaining < 0
                    || skillCooldownFrames < 0 || burstCooldownFrames < 0) throw new IllegalArgumentException("Invalid member resources");
        }
        public static Member capture(CharacterState state, long frame) {
            return new Member(state.hp(), state.energy(), remaining(state.skillReadyFrame(), frame),
                    remaining(state.burstReadyFrame(), frame), state.skillCooldownFrames(), state.burstCooldownFrames(), !state.alive());
        }
        private Map<String, Object> toRecord() {
            return Map.of("hp", hp, "energy", energy, "skill_remaining", skillRemaining, "burst_remaining", burstRemaining,
                    "skill_cooldown_frames", skillCooldownFrames, "burst_cooldown_frames", burstCooldownFrames, "fallen", fallen);
        }
    }

    public PartySave {
        if (version != CURRENT_VERSION || activeSlot < 0 || activeSlot >= Party.SIZE || members.size() != Party.SIZE
                || !Double.isFinite(stamina) || staminaRegenRemaining < 0 || switchRemaining < 0)
            throw new IllegalArgumentException("Invalid party save");
        members = List.copyOf(members);
    }
    public static int remaining(long deadline, long frame) { return (int) Math.clamp(deadline - frame, 0, Integer.MAX_VALUE); }

    /** A loader-independent record round trip, also used by the server's NBT codec. */
    public Map<String, Object> toRecord() {
        var rows = new ArrayList<Map<String, Object>>(Party.SIZE);
        for (Member member : members) rows.add(member.toRecord());
        return Map.of("version", version, "active_slot", activeSlot, "members", rows,
                "stamina", stamina, "stamina_regen_remaining", staminaRegenRemaining, "stamina_exhausted", staminaExhausted,
                "switch_remaining", switchRemaining);
    }
    public static PartySave fromRecord(Map<?, ?> record) {
        Map<?, ?> migrated = migrate(record);
        Object rows = migrated.get("members");
        if (!(rows instanceof List<?> list) || list.size() > Party.SIZE) throw new IllegalArgumentException("Invalid saved members");
        var members = new ArrayList<Member>(Party.SIZE);
        for (int slot = 0; slot < Party.SIZE; slot++) {
            Map<?, ?> row = slot < list.size() && list.get(slot) instanceof Map<?, ?> map ? map : Map.of();
            double hp = row.containsKey("hp") ? number(row, "hp", 0).doubleValue()
                    : StarterLoadout.stats(ROSTER[slot], false, 0, 0).hp();
            int skill = number(row, "skill_remaining", 0).intValue();
            int burst = number(row, "burst_remaining", 0).intValue();
            members.add(new Member(hp, number(row, "energy", 0).doubleValue(), skill, burst,
                    number(row, "skill_cooldown_frames", skill).intValue(), number(row, "burst_cooldown_frames", burst).intValue(),
                    bool(row, "fallen", hp <= 0)));
        }
        return new PartySave(CURRENT_VERSION, number(migrated, "active_slot", 0).intValue(), members,
                number(migrated, "stamina", Stamina.NEW_PLAYER_MAX).doubleValue(),
                number(migrated, "stamina_regen_remaining", 0).intValue(), bool(migrated, "stamina_exhausted", false),
                number(migrated, "switch_remaining", 0).intValue());
    }
    /** Add subsequent version steps here; future versions must never be silently downgraded. */
    private static Map<?, ?> migrate(Map<?, ?> record) {
        int version = number(record, "version", 0).intValue();
        if (version == 0) {
            var migrated = new HashMap<Object, Object>(record);
            migrated.put("version", CURRENT_VERSION);
            migrated.putIfAbsent("members", List.of());
            // v0 lacked stamina/recovery/fallen/cooldown-span fields: decode supplies starter defaults.
            return migrated;
        }
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported party save version " + version);
        return record;
    }
    private static Number number(Map<?, ?> record, String key, Number fallback) {
        Object value = record.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Number number)) throw new IllegalArgumentException("Expected number for " + key);
        return number;
    }
    private static boolean bool(Map<?, ?> record, String key, boolean fallback) {
        Object value = record.get(key);
        if (value == null) return fallback;
        // NBT has no boolean tag: its byte 0/1 arrives as a Number through JavaOps.
        if (value instanceof Number number && (number.intValue() == 0 || number.intValue() == 1))
            return number.intValue() != 0;
        if (!(value instanceof Boolean result)) throw new IllegalArgumentException("Expected boolean for " + key);
        return result;
    }
}

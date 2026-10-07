package io.github.brainage04.genshininminecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World-owned reserve/camp clocks, player-owned unlocks. Every timer uses Unix milliseconds. */
public final class OverlaySavedData extends SavedData {
    private static final Codec<OverlayDefinition> DEFINITION = Codec.PASSTHROUGH.comapFlatMap(value -> {
        try { return DataResult.success(OverlayDefinition.parse(value.convert(JsonOps.INSTANCE).getValue().getAsJsonObject())); }
        catch (RuntimeException exception) { return DataResult.error(() -> "Invalid overlay: " + exception.getMessage()); }
    }, value -> new Dynamic<>(JsonOps.INSTANCE, value.json()));
    public static final Codec<OverlaySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DEFINITION.optionalFieldOf("definition").forGetter(data -> Optional.ofNullable(data.definition)),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("players", Map.of()).forGetter(data -> data.players),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("members", Map.of()).forGetter(data -> data.members),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("wipe_destinations", Map.of()).forGetter(data -> data.wipes),
            Codec.DOUBLE.optionalFieldOf("reserve", 0.0).forGetter(data -> data.reserve),
            Codec.LONG.optionalFieldOf("regenerated_at", 0L).forGetter(data -> data.regeneratedAt)
    ).apply(instance, OverlaySavedData::new));
    public static final SavedDataType<OverlaySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("genshininminecraft", "overlay"), OverlaySavedData::new, CODEC, null);
    public static final double HP_PER_STATUE = 5000;
    public static final long REGEN_INTERVAL_MILLIS = 15000;
    private OverlayDefinition definition;
    private final Map<String, List<String>> players;
    private final Map<String, Long> members; // absent = never spawned, -1 = alive, >0 = dead until this time.
    private final Map<String, String> wipes;
    private double reserve;
    private long regeneratedAt;
    public OverlaySavedData() { this(Optional.empty(), Map.of(), Map.of(), Map.of(), 0, 0); }
    private OverlaySavedData(Optional<OverlayDefinition> definition, Map<String, List<String>> players,
                             Map<String, Long> members, Map<String, String> wipes, double reserve, long regeneratedAt) {
        this.definition = definition.orElse(null);
        this.players = new HashMap<>(players);
        this.members = new HashMap<>(members);
        this.wipes = new HashMap<>(wipes);
        this.reserve = reserve;
        this.regeneratedAt = regeneratedAt;
    }
    public static OverlaySavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
    public OverlayDefinition definition() { return definition; }
    public void install(OverlayDefinition value) {
        if (definition != null && !definition.id().equals(value.id())) throw new IllegalStateException("A different overlay is already installed");
        definition = value;
        setDirty();
    }
    /** An empty unlock list also records first entry; it never unlocks a destination. */
    public boolean enter(UUID player) {
        if (players.putIfAbsent(player.toString(), List.of()) != null) return false;
        setDirty();
        return true;
    }
    public boolean activated(UUID player, String point) { return players.getOrDefault(player.toString(), List.of()).contains(point); }
    public boolean activatedByAnyone(String point) {
        for (var values : players.values()) if (values.contains(point)) return true;
        return false;
    }
    public boolean activate(UUID player, String point, long now) {
        if (definition == null || definition.point(point) == null || activated(player, point)) return false;
        regenerate(now);
        boolean newStatue = definition.point(point).statue() && !activatedByAnyone(point);
        var values = new java.util.ArrayList<>(players.getOrDefault(player.toString(), List.of()));
        values.add(point);
        players.put(player.toString(), List.copyOf(values));
        if (newStatue) reserve += HP_PER_STATUE;
        setDirty();
        return true;
    }
    public double capacity() {
        if (definition == null) return 0;
        int count = 0;
        for (var point : definition.points()) if (point.statue() && activatedByAnyone(point.id())) count++;
        return count * HP_PER_STATUE;
    }
    public double reserve() { return reserve; }
    public void regenerate(long now) {
        if (regeneratedAt == 0) { regeneratedAt = now; setDirty(); return; }
        long steps = Math.max(0, now - regeneratedAt) / REGEN_INTERVAL_MILLIS;
        if (steps == 0) return;
        reserve = Math.min(capacity(), Math.max(0, reserve) + capacity() * .01 * steps);
        regeneratedAt += steps * REGEN_INTERVAL_MILLIS;
        setDirty();
    }
    public double spend(double wanted) {
        double amount = Math.min(Math.max(0, reserve), Math.max(0, wanted));
        if (amount > 0) { reserve -= amount; setDirty(); }
        return amount;
    }
    public long memberState(String id) { return members.getOrDefault(id, 0L); }
    public void memberState(String id, long value) { if (value != members.getOrDefault(id, 0L)) { members.put(id, value); setDirty(); } }
    public void rememberWipe(UUID player, String point) { wipes.put(player.toString(), point); setDirty(); }
    public String consumeWipe(UUID player) {
        String value = wipes.remove(player.toString());
        if (value != null) setDirty();
        return value;
    }
}

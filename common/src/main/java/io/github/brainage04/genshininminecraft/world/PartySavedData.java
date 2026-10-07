package io.github.brainage04.genshininminecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import io.github.brainage04.genshininminecraft.GenshinInMinecraft;
import io.github.brainage04.genshininminecraft.rules.PartySave;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** One UUID-keyed resource store for the whole world, independent of loader and dimension. */
public final class PartySavedData extends SavedData {
    public static final Codec<PartySave> PARTY_CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
        Object value = dynamic.convert(JavaOps.INSTANCE).getValue();
        if (!(value instanceof Map<?, ?> record)) return DataResult.error(() -> "Expected party record");
        try { return DataResult.success(PartySave.fromRecord(record)); }
        catch (IllegalArgumentException exception) { return DataResult.error(exception::getMessage); }
    }, save -> new Dynamic<>(JavaOps.INSTANCE, save.toRecord()));
    public static final Codec<PartySavedData> CODEC = Codec.unboundedMap(Codec.STRING, PARTY_CODEC)
            .fieldOf("players").codec().xmap(PartySavedData::new, data -> data.players);
    public static final SavedDataType<PartySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GenshinInMinecraft.MOD_ID, "parties"), PartySavedData::new, CODEC, null);
    private final Map<String, PartySave> players;

    public PartySavedData() { players = new HashMap<>(); }
    private PartySavedData(Map<String, PartySave> players) { this.players = new HashMap<>(players); }
    public static PartySavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
    public PartySave get(UUID player) { return players.get(player.toString()); }
    public void put(UUID player, PartySave save) {
        if (!save.equals(players.put(player.toString(), save))) setDirty();
    }
}

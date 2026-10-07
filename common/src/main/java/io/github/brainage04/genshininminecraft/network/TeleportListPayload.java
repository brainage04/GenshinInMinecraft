package io.github.brainage04.genshininminecraft.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TeleportListPayload(List<Entry> points) implements CustomPacketPayload {
    public record Entry(String id, String name, boolean statue) {}
    public TeleportListPayload { points = List.copyOf(points); }
    public static final Type<TeleportListPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "teleport_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportListPayload> CODEC = StreamCodec.of((buffer, value) -> {
        buffer.writeVarInt(value.points.size());
        for (var point : value.points) { buffer.writeUtf(point.id, 128); buffer.writeUtf(point.name, 256); buffer.writeBoolean(point.statue); }
    }, buffer -> {
        int count = buffer.readVarInt();
        if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid teleport list size");
        var points = new ArrayList<Entry>(count);
        for (int index = 0; index < count; index++) points.add(new Entry(buffer.readUtf(128), buffer.readUtf(256), buffer.readBoolean()));
        return new TeleportListPayload(points);
    });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

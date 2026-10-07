package io.github.brainage04.genshininminecraft.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Cosmetic throw/removal snapshots only; never a client entity, hitbox or combat intent. */
public record BunnyVisualPayload(UUID owner, Identifier dimension, Kind kind, long startWorldFrame,
        Vec3 origin, Vec3 destination, float yaw) implements CustomPacketPayload {
    public enum Kind { THROW, EXPLODE, CANCEL }
    public static final Type<BunnyVisualPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "bunny_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BunnyVisualPayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeUUID(packet.owner); buffer.writeIdentifier(packet.dimension); buffer.writeEnum(packet.kind);
                buffer.writeLong(packet.startWorldFrame);
                buffer.writeDouble(packet.origin.x); buffer.writeDouble(packet.origin.y); buffer.writeDouble(packet.origin.z);
                buffer.writeDouble(packet.destination.x); buffer.writeDouble(packet.destination.y); buffer.writeDouble(packet.destination.z);
                buffer.writeFloat(packet.yaw);
            }, buffer -> new BunnyVisualPayload(buffer.readUUID(), buffer.readIdentifier(), buffer.readEnum(Kind.class),
                    buffer.readLong(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat()));
    @Override public Type<BunnyVisualPayload> type() { return TYPE; }
}

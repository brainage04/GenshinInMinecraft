package io.github.brainage04.genshininminecraft.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Server-authored cosmetic geometry. No client hitbox, damage, prediction or C2S counterpart. */
public record ProjectileVisualPayload(UUID owner, Identifier dimension, long id, Kind kind,
        long startWorldFrame, int durationFrames, Vec3 origin, Vec3 destination) implements CustomPacketPayload {
    public enum Kind { ARROW, PYRO_ARROW, CATALYST_BOLT, CHARGED_BOLT, VIOLET_ORB, ROSE_BOLT,
        RAIN_ARROW, PALM_VORTEX, TORNADO, FROST_ICICLE, WALTZ_ICICLE, CANCEL }
    public static final double ADAPTED_ARROW_BLOCKS_PER_SECOND = 32;
    public static final Type<ProjectileVisualPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "projectile_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProjectileVisualPayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeUUID(packet.owner); buffer.writeIdentifier(packet.dimension); buffer.writeLong(packet.id);
                buffer.writeEnum(packet.kind); buffer.writeLong(packet.startWorldFrame); buffer.writeVarInt(packet.durationFrames);
                buffer.writeDouble(packet.origin.x); buffer.writeDouble(packet.origin.y); buffer.writeDouble(packet.origin.z);
                buffer.writeDouble(packet.destination.x); buffer.writeDouble(packet.destination.y); buffer.writeDouble(packet.destination.z);
            }, buffer -> new ProjectileVisualPayload(buffer.readUUID(), buffer.readIdentifier(), buffer.readLong(), buffer.readEnum(Kind.class),
                    buffer.readLong(), buffer.readVarInt(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())));
    @Override public Type<ProjectileVisualPayload> type() { return TYPE; }
}

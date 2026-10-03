package io.github.brainage04.genshininminecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Camera basis for server-owned dashes; never replaces the entity's combat-facing rotation. */
public record CameraYawPayload(float yaw) implements CustomPacketPayload {
    public static final Type<CameraYawPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "camera_yaw"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CameraYawPayload> CODEC =
            CustomPacketPayload.codec((packet, buffer) -> buffer.writeFloat(packet.yaw),
                    buffer -> new CameraYawPayload(buffer.readFloat()));
    @Override public Type<CameraYawPayload> type() { return TYPE; }
}

package io.github.brainage04.genshininminecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Empty destination requests a fresh server-owned list; other IDs request travel, never coordinates. */
public record TeleportRequestPayload(String destination) implements CustomPacketPayload {
    public static final Type<TeleportRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "teleport_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRequestPayload> CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeUtf(value.destination, 128), buffer -> new TeleportRequestPayload(buffer.readUtf(128)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

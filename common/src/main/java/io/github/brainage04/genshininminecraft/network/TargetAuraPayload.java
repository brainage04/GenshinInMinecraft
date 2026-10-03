package io.github.brainage04.genshininminecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Element ordinal bitset; an empty mask clears indicators without granting a client aura. */
public record TargetAuraPayload(int targetId, int elements) implements CustomPacketPayload {
    public static final Type<TargetAuraPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "target_aura"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TargetAuraPayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeVarInt(packet.targetId);
                buffer.writeVarInt(packet.elements);
            }, buffer -> new TargetAuraPayload(buffer.readVarInt(), buffer.readVarInt()));
    @Override public Type<TargetAuraPayload> type() { return TYPE; }
}

package io.github.brainage04.genshininminecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Aura bitset and independent Conductive count; visual state never grants client authority. */
public record TargetAuraPayload(int targetId, int elements, int conductiveStacks) implements CustomPacketPayload {
    public static final Type<TargetAuraPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "target_aura"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TargetAuraPayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeVarInt(packet.targetId);
                buffer.writeVarInt(packet.elements);
                buffer.writeByte(packet.conductiveStacks);
            }, buffer -> new TargetAuraPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readUnsignedByte()));
    @Override public Type<TargetAuraPayload> type() { return TYPE; }
}

package io.github.brainage04.genshininminecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Fixed solo Traveler identity; floats plus varint reference-frame cooldowns, no target state. */
public record CharacterStatePayload(boolean managed, float hp, float maxHp, float energy,
        int skillRemainingFrames, int burstRemainingFrames) implements CustomPacketPayload {
    public static final CharacterStatePayload UNMANAGED = new CharacterStatePayload(false, 0, 0, 0, 0, 0);
    public static final Type<CharacterStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "character_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterStatePayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeBoolean(packet.managed);
                buffer.writeFloat(packet.hp);
                buffer.writeFloat(packet.maxHp);
                buffer.writeFloat(packet.energy);
                buffer.writeVarInt(packet.skillRemainingFrames);
                buffer.writeVarInt(packet.burstRemainingFrames);
            }, buffer -> new CharacterStatePayload(buffer.readBoolean(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt()));
    @Override public Type<CharacterStatePayload> type() { return TYPE; }
}

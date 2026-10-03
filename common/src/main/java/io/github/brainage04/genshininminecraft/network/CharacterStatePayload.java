package io.github.brainage04.genshininminecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Solo Traveler resources; stamina adds one float and a packed exhausted/draining flag byte. */
public record CharacterStatePayload(boolean managed, float hp, float maxHp, float energy,
        int skillRemainingFrames, int burstRemainingFrames, float stamina,
        boolean staminaExhausted, boolean staminaDraining) implements CustomPacketPayload {
    public static final CharacterStatePayload UNMANAGED = new CharacterStatePayload(false, 0, 0, 0, 0, 0, 0, false, false);
    public static final Type<CharacterStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "character_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterStatePayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeBoolean(packet.managed);
                buffer.writeFloat(packet.hp);
                buffer.writeFloat(packet.maxHp);
                buffer.writeFloat(packet.energy);
                buffer.writeVarInt(packet.skillRemainingFrames);
                buffer.writeVarInt(packet.burstRemainingFrames);
                buffer.writeFloat(packet.stamina);
                buffer.writeByte((packet.staminaExhausted ? 1 : 0) | (packet.staminaDraining ? 2 : 0));
            }, buffer -> {
                boolean managed = buffer.readBoolean();
                float hp = buffer.readFloat(), maxHp = buffer.readFloat(), energy = buffer.readFloat();
                int skill = buffer.readVarInt(), burst = buffer.readVarInt();
                float stamina = buffer.readFloat();
                int flags = buffer.readUnsignedByte();
                return new CharacterStatePayload(managed, hp, maxHp, energy, skill, burst, stamina,
                        (flags & 1) != 0, (flags & 2) != 0);
            });
    @Override public Type<CharacterStatePayload> type() { return TYPE; }
}

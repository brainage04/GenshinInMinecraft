package io.github.brainage04.genshininminecraft.network;

import io.github.brainage04.genshininminecraft.rules.Locomotion;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Public committed appearance/traversal only. Private party resources stay in CharacterStatePayload. */
public record PlayerCharacterPayload(int playerId, UUID playerUuid, int slot, Locomotion.Phase phase,
        int occurrence, long phaseStartFrame, int traversalFlags, long sampleFrame, long sampleGameTime)
        implements CustomPacketPayload {
    public static final Type<PlayerCharacterPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "player_character"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerCharacterPayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeVarInt(packet.playerId); buffer.writeUUID(packet.playerUuid); buffer.writeByte(packet.slot);
                buffer.writeEnum(packet.phase); buffer.writeVarInt(packet.occurrence); buffer.writeVarLong(packet.phaseStartFrame);
                buffer.writeByte(packet.traversalFlags); buffer.writeVarLong(packet.sampleFrame); buffer.writeVarLong(packet.sampleGameTime);
            }, buffer -> new PlayerCharacterPayload(buffer.readVarInt(), buffer.readUUID(), buffer.readByte(),
                    buffer.readEnum(Locomotion.Phase.class), buffer.readVarInt(), buffer.readVarLong(),
                    buffer.readUnsignedByte(), buffer.readVarLong(), buffer.readVarLong()));
    @Override public Type<PlayerCharacterPayload> type() { return TYPE; }
}

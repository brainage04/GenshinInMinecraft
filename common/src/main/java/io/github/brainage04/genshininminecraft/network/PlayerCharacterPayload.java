package io.github.brainage04.genshininminecraft.network;

import io.github.brainage04.genshininminecraft.rules.Locomotion;
import io.github.brainage04.genshininminecraft.rules.CombatVisual;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Public committed appearance/motion/combat only. Private party resources stay in CharacterStatePayload. */
public record PlayerCharacterPayload(int playerId, UUID playerUuid, int slot, Locomotion.Phase phase,
        int occurrence, long phaseStartFrame, int traversalFlags, long sampleFrame, long sampleGameTime,
        CombatVisual.Action action, int actionOccurrence, long actionStartFrame, int strikeFrame, int recoveryFrames,
        int hurtOccurrence, long hurtStartFrame)
        implements CustomPacketPayload {
    public static final Type<PlayerCharacterPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "player_character"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerCharacterPayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeVarInt(packet.playerId); buffer.writeUUID(packet.playerUuid); buffer.writeByte(packet.slot);
                buffer.writeEnum(packet.phase); buffer.writeVarInt(packet.occurrence); buffer.writeVarLong(packet.phaseStartFrame);
                buffer.writeByte(packet.traversalFlags); buffer.writeVarLong(packet.sampleFrame); buffer.writeVarLong(packet.sampleGameTime);
                buffer.writeEnum(packet.action); buffer.writeVarInt(packet.actionOccurrence); buffer.writeVarLong(packet.actionStartFrame);
                buffer.writeVarInt(packet.strikeFrame); buffer.writeVarInt(packet.recoveryFrames);
                buffer.writeVarInt(packet.hurtOccurrence); buffer.writeVarLong(packet.hurtStartFrame);
            }, buffer -> new PlayerCharacterPayload(buffer.readVarInt(), buffer.readUUID(), buffer.readByte(),
                    buffer.readEnum(Locomotion.Phase.class), buffer.readVarInt(), buffer.readVarLong(),
                    buffer.readUnsignedByte(), buffer.readVarLong(), buffer.readVarLong(), buffer.readEnum(CombatVisual.Action.class),
                    buffer.readVarInt(), buffer.readVarLong(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarLong()));
    @Override public Type<PlayerCharacterPayload> type() { return TYPE; }
}

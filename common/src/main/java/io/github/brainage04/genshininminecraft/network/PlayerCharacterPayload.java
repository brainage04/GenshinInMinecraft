package io.github.brainage04.genshininminecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Public appearance only, never another player's private resource state. -1 clears the plate. */
public record PlayerCharacterPayload(int playerId, int slot) implements CustomPacketPayload {
    public static final Type<PlayerCharacterPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "player_character"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerCharacterPayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> { buffer.writeVarInt(packet.playerId); buffer.writeByte(packet.slot); },
            buffer -> new PlayerCharacterPayload(buffer.readVarInt(), buffer.readByte()));
    @Override public Type<PlayerCharacterPayload> type() { return TYPE; }
}

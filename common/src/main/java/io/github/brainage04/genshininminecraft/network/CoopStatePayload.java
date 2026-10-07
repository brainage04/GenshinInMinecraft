package io.github.brainage04.genshininminecraft.network;

import io.github.brainage04.genshininminecraft.rules.Coop;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Public active resources only; dormant/owned character resources are never shared with teammates. */
public record CoopStatePayload(int allocatedMask, List<Teammate> teammates) implements CustomPacketPayload {
    public record Teammate(UUID uuid, String playerName, int slot, float hp, float maxHp) {}
    public CoopStatePayload { teammates = List.copyOf(teammates); }
    public static final CoopStatePayload SOLO = new CoopStatePayload(Coop.FULL_ROSTER, List.of());
    public static final Type<CoopStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "coop_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoopStatePayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeByte(packet.allocatedMask);
                buffer.writeVarInt(packet.teammates.size());
                for (Teammate teammate : packet.teammates) {
                    buffer.writeUUID(teammate.uuid); buffer.writeUtf(teammate.playerName, 16);
                    buffer.writeByte(teammate.slot); buffer.writeFloat(teammate.hp); buffer.writeFloat(teammate.maxHp);
                }
            }, buffer -> {
                int mask = buffer.readUnsignedByte();
                int count = buffer.readVarInt();
                if (mask == 0 || (mask & ~Coop.FULL_ROSTER) != 0 || count < 0 || count >= Coop.MAX_PLAYERS)
                    throw new IllegalArgumentException("Invalid co-op state");
                var teammates = new java.util.ArrayList<Teammate>(count);
                for (int index = 0; index < count; index++) {
                    UUID uuid = buffer.readUUID(); String name = buffer.readUtf(16); int slot = buffer.readUnsignedByte();
                    if (slot >= 4) throw new IllegalArgumentException("Invalid teammate character");
                    teammates.add(new Teammate(uuid, name, slot, buffer.readFloat(), buffer.readFloat()));
                }
                return new CoopStatePayload(mask, teammates);
            });
    @Override public Type<CoopStatePayload> type() { return TYPE; }
}

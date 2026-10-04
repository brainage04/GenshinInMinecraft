package io.github.brainage04.genshininminecraft.network;

import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Party;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Fixed starter roster: four HP fractions/energy floats and cooldown varints, plus shared stamina. */
public record CharacterStatePayload(boolean managed, int activeSlot, List<Member> members, float stamina,
        boolean staminaExhausted, boolean staminaDraining, int traversalFlags) implements CustomPacketPayload {
    public record Member(float hpFraction, float energy, int skillRemainingFrames, int burstRemainingFrames) {}
    public static final List<CharacterBaseStats.Character> ROSTER = List.of(CharacterBaseStats.Character.TRAVELER_ANEMO,
            CharacterBaseStats.Character.AMBER, CharacterBaseStats.Character.KAEYA, CharacterBaseStats.Character.LISA);
    public static final CharacterStatePayload UNMANAGED = new CharacterStatePayload(false, 0, List.of(), 0, false, false, 0);
    public CharacterStatePayload { members = List.copyOf(members); }
    public float maxHp() { return managed ? (float) CharacterBaseStats.at(ROSTER.get(activeSlot), 20).hp() : 0; }
    public float hp() { return managed ? members.get(activeSlot).hpFraction() * maxHp() : 0; }
    public float energy() { return managed ? members.get(activeSlot).energy() : 0; }
    public int skillRemainingFrames() { return managed ? members.get(activeSlot).skillRemainingFrames() : 0; }
    public int burstRemainingFrames() { return managed ? members.get(activeSlot).burstRemainingFrames() : 0; }
    public boolean climbing() { return (traversalFlags & 3) == 1; }
    public boolean gliding() { return (traversalFlags & 3) == 2; }
    public int wallOrdinal() { return (traversalFlags >> 2) & 7; }
    public boolean climbJumping() { return (traversalFlags >> 5) != 0; }
    public int climbJumpSide() { return (traversalFlags >> 5) - 2; }
    public static final Type<CharacterStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "character_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterStatePayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeBoolean(packet.managed);
                if (!packet.managed) return;
                buffer.writeByte(packet.activeSlot);
                for (Member member : packet.members) {
                    buffer.writeFloat(member.hpFraction);
                    buffer.writeFloat(member.energy);
                    buffer.writeVarInt(member.skillRemainingFrames);
                    buffer.writeVarInt(member.burstRemainingFrames);
                }
                buffer.writeFloat(packet.stamina);
                buffer.writeByte((packet.staminaExhausted ? 1 : 0) | (packet.staminaDraining ? 2 : 0));
                buffer.writeByte(packet.traversalFlags);
            }, buffer -> {
                if (!buffer.readBoolean()) return UNMANAGED;
                int slot = buffer.readUnsignedByte();
                if (slot >= Party.SIZE) throw new IllegalArgumentException("Invalid active party slot");
                var members = new java.util.ArrayList<Member>(Party.SIZE);
                for (int index = 0; index < Party.SIZE; index++) members.add(new Member(buffer.readFloat(), buffer.readFloat(),
                        buffer.readVarInt(), buffer.readVarInt()));
                float stamina = buffer.readFloat();
                int flags = buffer.readUnsignedByte();
                return new CharacterStatePayload(true, slot, members, stamina, (flags & 1) != 0, (flags & 2) != 0,
                        buffer.readUnsignedByte());
            });
    @Override public Type<CharacterStatePayload> type() { return TYPE; }
}

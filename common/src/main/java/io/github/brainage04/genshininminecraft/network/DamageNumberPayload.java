package io.github.brainage04.genshininminecraft.network;

import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Reaction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One accepted damage instance, or a zero-amount non-damaging reaction announcement. */
public record DamageNumberPayload(int targetId, float amount, Element element, Reaction.Type reaction,
        boolean critical) implements CustomPacketPayload {
    public static final Type<DamageNumberPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "damage_number"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DamageNumberPayload> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeVarInt(packet.targetId);
                buffer.writeFloat(packet.amount);
                buffer.writeEnum(packet.element);
                buffer.writeVarInt(packet.reaction == null ? 0 : packet.reaction.ordinal() + 1);
                buffer.writeBoolean(packet.critical);
            }, buffer -> {
                int target = buffer.readVarInt();
                float amount = buffer.readFloat();
                Element element = buffer.readEnum(Element.class);
                int reaction = buffer.readVarInt();
                return new DamageNumberPayload(target, amount, element,
                        reaction == 0 ? null : Reaction.Type.values()[reaction - 1], buffer.readBoolean());
            });
    @Override public Type<DamageNumberPayload> type() { return TYPE; }
}

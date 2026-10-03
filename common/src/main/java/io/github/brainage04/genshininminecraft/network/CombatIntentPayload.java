package io.github.brainage04.genshininminecraft.network;

import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit.Intent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Intent only: never includes targets, client damage, energy or client timestamps. */
public record CombatIntentPayload(Intent intent) implements CustomPacketPayload {
    public static final Type<CombatIntentPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("genshininminecraft", "combat_intent"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombatIntentPayload> CODEC =
            CustomPacketPayload.codec((packet, buffer) -> buffer.writeEnum(packet.intent),
                    buffer -> new CombatIntentPayload(buffer.readEnum(Intent.class)));
    @Override public Type<CombatIntentPayload> type() { return TYPE; }
}

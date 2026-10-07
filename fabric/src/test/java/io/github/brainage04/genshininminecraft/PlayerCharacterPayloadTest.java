package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.network.PlayerCharacterPayload;
import io.github.brainage04.genshininminecraft.rules.Locomotion;
import io.github.brainage04.genshininminecraft.rules.CombatVisual;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerCharacterPayloadTest {
    @Test void publicVisualCodecPreservesIdentityPhaseOccurrenceAndBothClockAnchors() {
        for (var phase : Locomotion.Phase.values()) {
            var packet = new PlayerCharacterPayload(42, new UUID(12, 34), 3, phase, 19,
                    123456789L, 1 | 3 << 2, 123456804L, 987654321L,
                    CombatVisual.Action.SKILL_HOLD, 21, 123456780L, -1, -1, 4, 123456800L);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                PlayerCharacterPayload.CODEC.encode(buffer, packet);
                assertEquals(packet, PlayerCharacterPayload.CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
        var clear = new PlayerCharacterPayload(42, new UUID(12, 34), -1, Locomotion.Phase.IDLE, 0, 0, 0, 0, 0,
                CombatVisual.Action.NONE, 0, 0, -1, 0, 0, -60);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            PlayerCharacterPayload.CODEC.encode(buffer, clear);
            assertEquals(clear, PlayerCharacterPayload.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}

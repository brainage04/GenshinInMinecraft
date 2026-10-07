package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.network.BunnyVisualPayload;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BunnyVisualPayloadTest {
    @Test void cosmeticEventsPreserveOwnerDimensionAbsoluteClockPositionAndRotation() {
        for (var kind : BunnyVisualPayload.Kind.values()) {
            var packet = new BunnyVisualPayload(new UUID(12, 34), Identifier.fromNamespaceAndPath("minecraft", "overworld"),
                    kind, 123456789L, new Vec3(1.25, -60, 2.5), new Vec3(1.25, -61, 5.5), 145.5F);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                BunnyVisualPayload.CODEC.encode(buffer, packet);
                assertEquals(packet, BunnyVisualPayload.CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
}

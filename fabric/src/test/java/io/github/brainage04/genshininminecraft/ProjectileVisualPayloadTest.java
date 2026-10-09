package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.rules.CombatAudio;
import io.github.brainage04.genshininminecraft.network.ProjectileVisualPayload;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Reaction;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectileVisualPayloadTest {
    @Test void everyMeshAndCancellationPreservesIdentityAbsoluteClockAndEndpoints() {
        for (var kind : ProjectileVisualPayload.Kind.values()) {
            var packet = new ProjectileVisualPayload(new UUID(12, 34), Identifier.fromNamespaceAndPath("minecraft", "overworld"),
                    987654321L, kind, 123456789L, 480, new Vec3(1.25, -60, 2.5), new Vec3(1.25, -61, 5.5));
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                ProjectileVisualPayload.CODEC.encode(buffer, packet);
                assertEquals(packet, ProjectileVisualPayload.CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
    @Test void audioBudgetsBoundAoeEcRoseAndCritWithoutSuppressingDifferentCues() {
        var budget = new CombatAudio.Budget();
        for (var element : Element.values()) assertNotNull(CombatAudio.element(element, false, false));
        for (var reaction : Reaction.Type.values()) assertNotNull(CombatAudio.reaction(reaction));
        assertTrue(budget.allow(CombatAudio.Cue.PYRO, 0, 0));
        assertFalse(budget.allow(CombatAudio.Cue.PYRO, 5, 1));
        assertTrue(budget.allow(CombatAudio.Cue.PYRO, 6, 2));
        assertTrue(budget.allow(CombatAudio.Cue.CRIT, 6, 2));
        assertFalse(budget.allow(CombatAudio.Cue.CRIT, 17, 5));
        assertTrue(budget.allow(CombatAudio.Cue.CRIT, 18, 6));
        assertTrue(budget.allow(CombatAudio.Cue.ELECTRO_CHARGED, 0, 0));
        assertFalse(budget.allow(CombatAudio.Cue.ELECTRO_CHARGED, 29, 9));
        assertTrue(budget.allow(CombatAudio.Cue.ELECTRO_CHARGED, 30, 10));
        assertFalse(budget.allow(CombatAudio.Cue.ROSE, 30, 10)); // Same resolved event cannot double.
        assertTrue(budget.allow(CombatAudio.Cue.ROSE, 33, 11));
        assertFalse(budget.allow(CombatAudio.Cue.ROSE, 44, 14));
        assertTrue(budget.allow(CombatAudio.Cue.ROSE, 45, 15));
    }
}

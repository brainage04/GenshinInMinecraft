package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.combat.CombatSounds;
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
        var budget = new CombatSounds.Budget();
        for (var element : Element.values()) assertNotNull(CombatSounds.element(element));
        for (var reaction : Reaction.Type.values()) assertNotNull(CombatSounds.reaction(reaction));
        assertTrue(budget.allow(CombatSounds.Cue.PYRO, 0));
        assertFalse(budget.allow(CombatSounds.Cue.PYRO, 5));
        assertTrue(budget.allow(CombatSounds.Cue.PYRO, 6));
        assertTrue(budget.allow(CombatSounds.Cue.CRIT, 6));
        assertFalse(budget.allow(CombatSounds.Cue.CRIT, 17));
        assertTrue(budget.allow(CombatSounds.Cue.CRIT, 18));
        assertTrue(budget.allow(CombatSounds.Cue.ELECTRO_CHARGED, 0));
        assertFalse(budget.allow(CombatSounds.Cue.ELECTRO_CHARGED, 29));
        assertTrue(budget.allow(CombatSounds.Cue.ELECTRO_CHARGED, 30));
        assertTrue(budget.allow(CombatSounds.Cue.ROSE, 30));
        assertFalse(budget.allow(CombatSounds.Cue.ROSE, 41));
        assertTrue(budget.allow(CombatSounds.Cue.ROSE, 42));
    }
}

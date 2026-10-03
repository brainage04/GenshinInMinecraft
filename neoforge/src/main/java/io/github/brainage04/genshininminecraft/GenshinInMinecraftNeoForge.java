package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ModCommands;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.network.CombatIntentPayload;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(GenshinInMinecraft.MOD_ID)
public final class GenshinInMinecraftNeoForge {
    public GenshinInMinecraftNeoForge(ModContainer container, IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> ModCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent event) -> {
            if (ManagedWorld.preventsBlockModification(event.getPlayer().level(), event.getPlayer())) {
                event.setCanceled(true);
                event.setNotifyClient(true);
            }
        });
        modBus.addListener((RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("1");
            registrar.playToServer(CombatIntentPayload.TYPE, CombatIntentPayload.CODEC, (packet, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    CombatRuntime.get(player.level().getServer()).receive(player, packet.intent());
                }
            });
            registrar.playToClient(CharacterStatePayload.TYPE, CharacterStatePayload.CODEC);
        });
        CombatRuntime.setSender(PacketDistributor::sendToPlayer);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> CombatRuntime.get(event.getServer()).tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> CombatRuntime.stop(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) CombatRuntime.get(player.level().getServer()).forget(player.getUUID());
        });
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent event) -> {
            if (CombatRuntime.cancelsVanillaMelee(event.getEntity())) event.setCanceled(true);
        });
        GenshinInMinecraft.initialize(container.getModInfo().getVersion().toString());
    }
}

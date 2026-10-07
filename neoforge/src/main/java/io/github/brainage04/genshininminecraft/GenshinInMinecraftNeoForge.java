package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ModCommands;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.enemy.BaronBunny;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.network.CombatIntentPayload;
import io.github.brainage04.genshininminecraft.network.CameraYawPayload;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.network.DamageNumberPayload;
import io.github.brainage04.genshininminecraft.network.TargetAuraPayload;
import io.github.brainage04.genshininminecraft.network.PlayerCharacterPayload;
import io.github.brainage04.genshininminecraft.network.BunnyVisualPayload;
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
        modBus.addListener((RegisterEvent event) -> {
            event.register(BuiltInRegistries.ENTITY_TYPE.key(), GenshinEntities.HILICHURL_ID, () -> GenshinEntities.HILICHURL);
            event.register(BuiltInRegistries.ENTITY_TYPE.key(), GenshinEntities.BARON_BUNNY_ID, () -> GenshinEntities.BARON_BUNNY);
        });
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(GenshinEntities.HILICHURL, Hilichurl.attributes().build());
            event.put(GenshinEntities.BARON_BUNNY, BaronBunny.createAttributes().build());
        });
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
            registrar.playToServer(CameraYawPayload.TYPE, CameraYawPayload.CODEC, (packet, context) -> {
                if (context.player() instanceof ServerPlayer player)
                    CombatRuntime.get(player.level().getServer()).receiveCameraYaw(player, packet.yaw());
            });
            registrar.playToClient(CharacterStatePayload.TYPE, CharacterStatePayload.CODEC);
            registrar.playToClient(DamageNumberPayload.TYPE, DamageNumberPayload.CODEC);
            registrar.playToClient(TargetAuraPayload.TYPE, TargetAuraPayload.CODEC);
            registrar.playToClient(PlayerCharacterPayload.TYPE, PlayerCharacterPayload.CODEC);
            registrar.playToClient(BunnyVisualPayload.TYPE, BunnyVisualPayload.CODEC);
        });
        CombatRuntime.setSender(PacketDistributor::sendToPlayer);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> CombatRuntime.get(event.getServer()).tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> CombatRuntime.stop(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) CombatRuntime.get(player.level().getServer()).forget(player.getUUID());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                CombatRuntime.get(player.level().getServer()).startTracking(player, event.getTarget());
            }
        });
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent event) -> {
            if (CombatRuntime.cancelsVanillaMelee(event.getEntity())) event.setCanceled(true);
        });
        GenshinInMinecraft.initialize(container.getModInfo().getVersion().toString());
    }
}

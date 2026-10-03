package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ModCommands;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.network.CombatIntentPayload;
import io.github.brainage04.genshininminecraft.network.CameraYawPayload;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import io.github.brainage04.genshininminecraft.network.DamageNumberPayload;
import io.github.brainage04.genshininminecraft.network.TargetAuraPayload;
import io.github.brainage04.genshininminecraft.network.PlayerCharacterPayload;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;

public class GenshinInMinecraftFabric implements ModInitializer {
	@Override
	public void onInitialize() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GenshinEntities.HILICHURL_ID, GenshinEntities.HILICHURL);
        FabricDefaultAttributeRegistry.register(GenshinEntities.HILICHURL, Hilichurl.attributes());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ModCommands.register(dispatcher));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
                !ManagedWorld.preventsBlockModification(level, player));
        PayloadTypeRegistry.serverboundPlay().register(CombatIntentPayload.TYPE, CombatIntentPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CameraYawPayload.TYPE, CameraYawPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CharacterStatePayload.TYPE, CharacterStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DamageNumberPayload.TYPE, DamageNumberPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TargetAuraPayload.TYPE, TargetAuraPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlayerCharacterPayload.TYPE, PlayerCharacterPayload.CODEC);
        EntityTrackingEvents.START_TRACKING.register((entity, player) ->
                CombatRuntime.get(player.level().getServer()).startTracking(player, entity));
        ServerPlayNetworking.registerGlobalReceiver(CombatIntentPayload.TYPE, (packet, context) ->
                CombatRuntime.get(context.server()).receive(context.player(), packet.intent()));
        ServerPlayNetworking.registerGlobalReceiver(CameraYawPayload.TYPE, (packet, context) ->
                CombatRuntime.get(context.server()).receiveCameraYaw(context.player(), packet.yaw()));
        CombatRuntime.setSender(ServerPlayNetworking::send);
        ServerTickEvents.END_SERVER_TICK.register(server -> CombatRuntime.get(server).tick(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(CombatRuntime::stop);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CombatRuntime.get(server).forget(handler.player.getUUID()));
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                CombatRuntime.cancelsVanillaMelee(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        GenshinInMinecraft.initialize(FabricLoader.getInstance()
                .getModContainer(GenshinInMinecraft.MOD_ID)
                .orElseThrow()
                .getMetadata()
                .getVersion()
                .getFriendlyString());
	}
}

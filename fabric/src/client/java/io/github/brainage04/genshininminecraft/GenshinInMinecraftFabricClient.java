package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ClientModCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class GenshinInMinecraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                ClientModCommands.register(dispatcher, FabricClientCommandSource::sendFeedback));
        GenshinInMinecraftClient.initialize();
        KeyMappingHelper.registerKeyMapping(CombatInput.SKILL);
        KeyMappingHelper.registerKeyMapping(CombatInput.BURST);
        CombatInput.initialize(ClientPlayNetworking::send);
        ClientPlayNetworking.registerGlobalReceiver(CharacterStatePayload.TYPE,
                (packet, context) -> CombatInput.accept(packet));
    }
}

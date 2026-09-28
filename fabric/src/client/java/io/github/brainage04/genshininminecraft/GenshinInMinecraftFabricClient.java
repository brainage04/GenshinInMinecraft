package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ClientModCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class GenshinInMinecraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                ClientModCommands.register(dispatcher, FabricClientCommandSource::sendFeedback));
        GenshinInMinecraftClient.initialize();
    }
}

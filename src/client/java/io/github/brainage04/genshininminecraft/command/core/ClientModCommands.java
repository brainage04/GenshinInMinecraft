package io.github.brainage04.genshininminecraft.command.core;

import io.github.brainage04.genshininminecraft.command.ExampleClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class ClientModCommands {
    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ExampleClientCommand.initialize(dispatcher);
        });
    }
}

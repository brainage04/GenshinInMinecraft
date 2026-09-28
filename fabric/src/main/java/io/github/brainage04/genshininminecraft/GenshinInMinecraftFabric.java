package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ModCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class GenshinInMinecraftFabric implements ModInitializer {
	@Override
	public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ModCommands.register(dispatcher));
        GenshinInMinecraft.initialize();
	}
}

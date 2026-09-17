package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ClientModCommands;
import net.fabricmc.api.ClientModInitializer;

public class GenshinInMinecraftClient implements ClientModInitializer {
    private static volatile boolean initialized;

    @Override
    public void onInitializeClient() {
        ClientModCommands.initialize();
        initialized = true;

        GenshinInMinecraft.LOGGER.info("{} client initialised.", GenshinInMinecraft.MOD_NAME);
    }

    public static boolean isInitialized() {
        return initialized;
    }
}

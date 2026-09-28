package io.github.brainage04.genshininminecraft;

public final class GenshinInMinecraftClient {
    private static volatile boolean initialized;

    private GenshinInMinecraftClient() {
    }

    /** Called once by each loader's client entrypoint. */
    public static void initialize() {
        initialized = true;

        GenshinInMinecraft.LOGGER.info("{} client initialised.", GenshinInMinecraft.MOD_NAME);
    }

    public static boolean isInitialized() {
        return initialized;
    }
}

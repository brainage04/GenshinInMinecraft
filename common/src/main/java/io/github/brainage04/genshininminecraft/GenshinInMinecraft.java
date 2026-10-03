package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GenshinInMinecraft {
    public static final String MOD_ID = "genshininminecraft";
    public static final String MOD_NAME = "GenshinInMinecraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    private static String version;

    private GenshinInMinecraft() {
    }

    /** Called once by each loader's common entrypoint. */
	public static void initialize(String modVersion) {
        version = modVersion;
        LOGGER.info("{} initialising...", MOD_NAME);

        ModConfig.init();

        if (ModConfig.get().logConfigOnStartup) {
            LOGGER.info("Loaded config: logConfigOnStartup=true");
        }

        LOGGER.info("{} initialised.", MOD_NAME);
	}

    public static String getVersion() {
        return version;
    }
}

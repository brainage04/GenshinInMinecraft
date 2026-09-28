package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GenshinInMinecraft {
    public static final String MOD_ID = "genshininminecraft";
    public static final String MOD_NAME = "GenshinInMinecraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private GenshinInMinecraft() {
    }

    /** Called once by each loader's common entrypoint. */
	public static void initialize() {
        LOGGER.info("{} initialising...", MOD_NAME);

        ModConfig.init();

        if (ModConfig.CONFIG.logConfigOnStartup.get()) {
            LOGGER.info(
                    "Loaded config: message='{}', mode={}, featuredItem={}, retries={}",
                    ModConfig.CONFIG.welcomeMessage.get(),
                    ModConfig.CONFIG.syncMode.get(),
                    ModConfig.CONFIG.featuredItem.get(),
                    ModConfig.CONFIG.startupRetries.get()
            );
        }

        LOGGER.info("{} initialised.", MOD_NAME);
	}
}

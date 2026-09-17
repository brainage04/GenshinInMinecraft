package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ModCommands;
import io.github.brainage04.genshininminecraft.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenshinInMinecraft implements ModInitializer {
    public static final String MOD_ID = "genshininminecraft";
    public static final String MOD_NAME = "GenshinInMinecraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	@Override
	public void onInitialize() {
        LOGGER.info("{} initialising...", MOD_NAME);

        ModConfig.init();
        ModCommands.initialize();

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

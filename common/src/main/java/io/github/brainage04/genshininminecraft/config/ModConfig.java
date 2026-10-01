package io.github.brainage04.genshininminecraft.config;

import io.github.brainage04.genshininminecraft.GenshinInMinecraft;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

/**
 * Cloth Config's AutoConfig saves this class to {@code config/genshininminecraft.json} and generates
 * its config screen (Mod Menu on Fabric, the mod list on NeoForge) from the public fields.
 */
@Config(name = GenshinInMinecraft.MOD_ID)
public class ModConfig implements ConfigData {
    public boolean logConfigOnStartup = true;
    public String exampleMessage = "This is an example command.";

    /** Registers the config and loads it from disk. Call once, during mod initialization. */
    public static void init() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
    }

    public static ModConfig get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }
}

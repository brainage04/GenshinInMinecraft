package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ModCommands;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(GenshinInMinecraft.MOD_ID)
public final class GenshinInMinecraftNeoForge {
    public GenshinInMinecraftNeoForge(ModContainer container) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> ModCommands.register(event.getDispatcher()));
        GenshinInMinecraft.initialize(container.getModInfo().getVersion().toString());
    }
}

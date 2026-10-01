package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ClientModCommands;
import io.github.brainage04.genshininminecraft.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = GenshinInMinecraft.MOD_ID, dist = Dist.CLIENT)
public final class GenshinInMinecraftNeoForgeClient {
    public GenshinInMinecraftNeoForgeClient(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> AutoConfigClient.getConfigScreen(ModConfig.class, parent).get());
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                ClientModCommands.register(event.getDispatcher(), CommandSourceStack::sendSystemMessage));
        GenshinInMinecraftClient.initialize();
    }
}

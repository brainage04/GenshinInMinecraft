package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ClientModCommands;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = GenshinInMinecraft.MOD_ID, dist = Dist.CLIENT)
public final class GenshinInMinecraftNeoForgeClient {
    public GenshinInMinecraftNeoForgeClient() {
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                ClientModCommands.register(event.getDispatcher(), CommandSourceStack::sendSystemMessage));
        GenshinInMinecraftClient.initialize();
    }
}

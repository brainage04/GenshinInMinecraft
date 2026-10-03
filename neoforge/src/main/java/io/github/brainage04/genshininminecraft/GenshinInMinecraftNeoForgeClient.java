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
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = GenshinInMinecraft.MOD_ID, dist = Dist.CLIENT)
public final class GenshinInMinecraftNeoForgeClient {
    public GenshinInMinecraftNeoForgeClient(ModContainer container, IEventBus modBus) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> AutoConfigClient.getConfigScreen(ModConfig.class, parent).get());
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                ClientModCommands.register(event.getDispatcher(), CommandSourceStack::sendSystemMessage));
        GenshinInMinecraftClient.initialize();
        CombatInput.initialize(ClientPacketDistributor::sendToServer);
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(CombatInput.SKILL);
            event.register(CombatInput.BURST);
        });
        modBus.addListener((RegisterClientPayloadHandlersEvent event) ->
                event.register(CharacterStatePayload.TYPE, (packet, context) -> CombatInput.accept(packet)));
    }
}

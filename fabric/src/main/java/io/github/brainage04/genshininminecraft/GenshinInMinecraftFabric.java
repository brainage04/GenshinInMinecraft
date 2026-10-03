package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ModCommands;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;

public class GenshinInMinecraftFabric implements ModInitializer {
	@Override
	public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ModCommands.register(dispatcher));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
                !ManagedWorld.preventsBlockModification(level, player));
        GenshinInMinecraft.initialize(FabricLoader.getInstance()
                .getModContainer(GenshinInMinecraft.MOD_ID)
                .orElseThrow()
                .getMetadata()
                .getVersion()
                .getFriendlyString());
	}
}

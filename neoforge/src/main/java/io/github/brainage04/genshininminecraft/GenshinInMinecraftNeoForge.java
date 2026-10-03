package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ModCommands;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

@Mod(GenshinInMinecraft.MOD_ID)
public final class GenshinInMinecraftNeoForge {
    public GenshinInMinecraftNeoForge(ModContainer container) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> ModCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent event) -> {
            if (ManagedWorld.preventsBlockModification(event.getPlayer().level(), event.getPlayer())) {
                event.setCanceled(true);
                event.setNotifyClient(true);
            }
        });
        GenshinInMinecraft.initialize(container.getModInfo().getVersion().toString());
    }
}

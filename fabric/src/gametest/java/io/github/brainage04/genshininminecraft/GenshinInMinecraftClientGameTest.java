package io.github.brainage04.genshininminecraft;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.lwjgl.glfw.GLFW;


import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public class GenshinInMinecraftClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();

        ClientGameTestServers.withDedicatedServer(context, serverProperties, "GenshinInMinecraft GameTest", server -> { try {
            context.computeOnClient(client -> {
                if (!GenshinInMinecraftClient.isInitialized()) {
                    throw new AssertionError("Expected the client initializer to run before the client GameTest.");
                }
        
                if (client.level == null) {
                    throw new AssertionError("Expected the client to be connected to a world during the client GameTest.");
                }
        
                if (client.player == null) {
                    throw new AssertionError("Expected the client player to be available during the client GameTest.");
                }
        
                return null;
            });
        
            ClientGameTestRecorder.startRecording(context);
            ClientGameTestRecorder.showStep(context, "genshin.ready", "GenshinInMinecraft client GameTest", "client initializer and world ready");
            context.waitTicks(20);
            server.runCommand("execute as @a at @s run genshin arena");
            context.waitFor(client -> CombatInput.managed());
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            server.waitFor(minecraftServer -> minecraftServer.getPlayerList().getPlayers().stream().anyMatch(player ->
                    CombatRuntime.get(minecraftServer).session(player).kit().skillReadyFrame() > 0));
            context.waitTicks(10);
            context.runOnClient(client -> {
                if (client.gui.screen() instanceof InventoryScreen) throw new AssertionError("Managed E must not also open vanilla inventory");
                if (CombatInput.state().skillRemainingFrames() <= 0) throw new AssertionError("Server cooldown must reach the client");
            });
            ClientGameTestRecorder.showStep(context, "genshin.skill", "Managed arena controls", "E starts server-owned Palm Vortex, not inventory");
            server.runCommand("genshin managed off");
            context.waitFor(client -> !CombatInput.managed());
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            context.waitForScreen(InventoryScreen.class);
        } finally {
            context.getInput().releaseKey(GLFW.GLFW_KEY_E);
        } });
    }
}

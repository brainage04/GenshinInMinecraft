package io.github.brainage04.genshininminecraft.gametest;

import io.github.brainage04.genshininminecraft.command.GenshinCommand;
import net.minecraft.gametest.framework.GameTestHelper;

/// GameTest bodies shared by the Fabric and NeoForge GameTest registrations.
public final class GenshinInMinecraftGameTests {
    private GenshinInMinecraftGameTests() {
    }

    public static void genshinCommandIsRegistered(GameTestHelper context) {
        if (context.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild(GenshinCommand.COMMAND_NAME) == null) {
            throw new AssertionError("Expected /genshin to be registered on the dedicated server.");
        }

        context.succeed();
    }
}

package io.github.brainage04.genshininminecraft.gametest;

import io.github.brainage04.genshininminecraft.command.ExampleCommand;
import net.minecraft.gametest.framework.GameTestHelper;

/// GameTest bodies shared by the Fabric and NeoForge GameTest registrations.
public final class GenshinInMinecraftGameTests {
    private GenshinInMinecraftGameTests() {
    }

    public static void exampleCommandIsRegistered(GameTestHelper context) {
        if (context.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild(ExampleCommand.COMMAND_NAME) == null) {
            throw new AssertionError("Expected the example command to be registered on the dedicated server.");
        }

        context.succeed();
    }
}

package io.github.brainage04.genshininminecraft.command.core;

import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.genshininminecraft.command.ExampleCommand;
import net.minecraft.commands.CommandSourceStack;

public class ModCommands {
    /** Registers every server command; each loader calls this from its command registration event. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ExampleCommand.initialize(dispatcher);
    }
}

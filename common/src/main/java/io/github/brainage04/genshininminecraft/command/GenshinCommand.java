package io.github.brainage04.genshininminecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.brainage04.genshininminecraft.GenshinInMinecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class GenshinCommand {
    public static final String COMMAND_NAME = "genshin";

    private GenshinCommand() {
    }

    public static int execute(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal(GenshinInMinecraft.MOD_NAME + " " + GenshinInMinecraft.getVersion()),
                false);
        return 1;
    }

    public static void initialize(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal(COMMAND_NAME)
                .executes(context -> execute(context.getSource())));
    }
}

package io.github.brainage04.genshininminecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.brainage04.genshininminecraft.command.core.ClientCommandFeedback;
import io.github.brainage04.genshininminecraft.config.ModConfig;
import net.minecraft.network.chat.Component;

public class ExampleClientCommand {
    public static final String COMMAND_NAME = "exampleclient";

    public static <S> int execute(S source, ClientCommandFeedback<S> feedback) {
        feedback.sendFeedback(source, Component.literal(ModConfig.get().exampleMessage));

        return 1;
    }

    public static <S> void initialize(CommandDispatcher<S> dispatcher, ClientCommandFeedback<S> feedback) {
        dispatcher.register(LiteralArgumentBuilder.<S>literal(COMMAND_NAME)
                .executes(context ->
                        execute(
                                context.getSource(),
                                feedback
                        )
                )
        );
    }
}

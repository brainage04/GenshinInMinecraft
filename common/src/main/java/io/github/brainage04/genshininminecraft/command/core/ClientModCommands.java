package io.github.brainage04.genshininminecraft.command.core;

import com.mojang.brigadier.CommandDispatcher;

public class ClientModCommands {
    /** Registers every client command; each loader calls this from its client command registration event. */
    public static <S> void register(CommandDispatcher<S> dispatcher, ClientCommandFeedback<S> feedback) {
        // No client-only commands are currently registered; /genshin is server-owned.
    }
}
